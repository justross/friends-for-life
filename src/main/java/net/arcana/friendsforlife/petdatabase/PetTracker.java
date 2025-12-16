package net.arcana.friendsforlife.petdatabase;

import io.netty.buffer.Unpooled;
import net.arcana.friendsforlife.Init;
import net.arcana.friendsforlife.Net;
import net.arcana.friendsforlife.packets.PetListPacket;
import net.arcana.friendsforlife.util.CompatComponent;
import net.arcana.friendsforlife.util.CompatItemStack;
import net.arcana.friendsforlife.util.CompatResourceLocation;
import net.arcana.friendsforlife.util.SavedData;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class PetTracker extends SavedData {
    private final Map<UUID, PetData> tracked = new HashMap<>();
    private final Set<UUID> recreated = new HashSet<>(); // Entities that have been recreated and must be removed

    @Nullable
    public PetData addPet(TamableAnimal tame, ServerPlayer player, ItemStack item, long tameTimestamp) {
        Init.LOGGER.info("adding new entity to be tracked: " + tame.getName());
        if (tracked.values().stream().noneMatch(pet -> pet.entityUuid.equals(tame.getUUID()))) {
            CompoundTag collar = new CompoundTag();
            //? if >=1.21 {
            /*collar = CompatItemStack.save(item, collar, player.level());
            *///?} else {
            CompatItemStack.save(item, collar, player.level());
            //?}
            PetData pd = new PetData(tame, player, collar, tameTimestamp);
            tracked.put(pd.uuid, pd);
            Net.CHANNEL.serverHandle(player).send(PetTracker.toEntries(player.getServer()));
            return pd;
        }
        return null;
    }

    public void removePet(MinecraftServer server, UUID uuid) {
        var player = server.getPlayerList().getPlayer(tracked.get(uuid).ownerUuid);
        PetData petData = tracked.get(uuid);
        if (petData != null) {
            petData.onRemoved(server);
        }
        tracked.remove(uuid);
        if (player == null) {
            Init.LOGGER.error("Could not find player to update pet list for removed pet: " + uuid);
            return;
        }
        var petList = PetTracker.toEntries(server);
        Net.CHANNEL.serverHandle(player).send(PetTracker.toEntries(player.getServer()));
    }

    public Map<UUID, PetData> getTrackedMap() {
        return tracked;
    }

    /**
     * Map of entities marked for deletion
     */
    public Set<UUID> getRecreatedEntityMap() {
        return recreated;
    }

    public static PetTracker get(MinecraftServer server) {
        return SavedData.get(server, "friends_for_life_tracked_pets", PetTracker::fromNBT, PetTracker::new);
    }

    public PetData get(UUID uuid) {
        return tracked.get(uuid);
    }

    @Nullable
    public PetData getByEntityId(UUID entityUuid) {
        return tracked.values().stream()
                .filter(pet -> pet.entityUuid.equals(entityUuid))
                .findFirst()
                .orElse(null);
    }

    public static List<PetData> getAllForPlayer(Player player) {
        return PetTracker.get(player.getServer()).tracked.values().stream()
                .filter(pet -> pet.ownerUuid.equals(player.getUUID()))
                .toList();
    }

    public static FriendlyByteBuf serializePetData(PetData pet, MinecraftServer server) {
        FriendlyByteBuf out = new FriendlyByteBuf(Unpooled.buffer());
        serializePetData(out, pet, server);
        return out;
    }

    private static void serializePetData(FriendlyByteBuf out, PetData pet, MinecraftServer server) {
        out.writeUUID(pet.uuid);

        var data = pet.getEntity(server).join();
        if (data != null && data.entity() != null) {
            writeEntityData(out, data.entity(), null);
        } else if (data != null && data.shoulderNbt() != null) {
            writeEntityData(out, null, data.shoulderNbt());
        } else {
            writeEntityData(out, null, pet.getEntityData());
        }

        out.writeUtf(pet.position.toShortString());
        out.writeLong(pet.homePosition != null ? pet.homePosition.asLong() : 0L);
        out.writeUtf(pet.homeDimension != null ? pet.homeDimension.location().toString() : "");
    }

    /**
     * Serializes pet list for player
     */
    public static PetListPacket toEntries(MinecraftServer server) {
        List<PetData> pets = new ArrayList<>(PetTracker.get(server).tracked.values());
        pets.sort((a, b) -> Long.compare(a.tameTimestamp, b.tameTimestamp));
        return new PetListPacket(pets.stream().map(PetData::toEntry).toList());
    }

    @Override
    protected void write(CompoundTag nbt) {
        ListTag petList = new ListTag();
        for (PetData data : tracked.values()) {
            petList.add(data.toNbt());
        }
        nbt.put("pets", petList);

        ListTag recreatedList = new ListTag();
        for (UUID uuid : recreated) {
            CompoundTag tag = new CompoundTag();
            tag.putUUID("uuid", uuid);
            recreatedList.add(tag);
        }
        nbt.put("recreatedPets", recreatedList);
    }

    public static PetTracker fromNBT(CompoundTag nbt) {
        PetTracker tracker = new PetTracker();

        // load pets
        ListTag petList = nbt.getList("pets", Tag.TAG_COMPOUND);
        for (Tag e : petList) {
            PetData data = PetData.fromNbt((CompoundTag) e);
            if (data != null) {
                tracker.tracked.put(data.uuid, data);
            }
        }

        // load recreated UUIDs
        ListTag recreatedList = nbt.getList("recreatedPets", Tag.TAG_COMPOUND);
        for (Tag e : recreatedList) {
            CompoundTag tag = (CompoundTag) e;
            if (tag.hasUUID("uuid")) {
                tracker.recreated.add(tag.getUUID("uuid"));
            }
        }

        return tracker;
    }

    /**
     * Serializer helper method
     *
     * @param out
     * @param entity
     * @param nbt
     */
    private static void writeEntityData(FriendlyByteBuf out, Entity entity, @Nullable CompoundTag nbt) {
        if (entity != null) {
            out.writeUtf(BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).toString());
            CompoundTag entityNbt = new CompoundTag();
            entity.save(entityNbt);
            out.writeNbt(entityNbt);

            String name = entity.hasCustomName()
                    ? Objects.requireNonNull(entity.getCustomName()).getString()
                    : entity.getType().getDescription().getString();
            out.writeUtf(name);
        } else if (nbt != null) {
            out.writeUtf(nbt.getString("id"));
            out.writeNbt(nbt);

            String name = nbt.contains("CustomName")
                    ? Objects.requireNonNull(CompatComponent.fromJson(nbt.getString("CustomName"))).getString()
                    : BuiltInRegistries.ENTITY_TYPE.get(new CompatResourceLocation(nbt.getString("id")).unwrap()).getDescription().getString();
            out.writeUtf(name);
        }
    }

}
