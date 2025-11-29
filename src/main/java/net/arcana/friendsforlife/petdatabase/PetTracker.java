package net.arcana.friendsforlife.petdatabase;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import net.arcana.friendsforlife.loaders.fabric.FabricEntrypoint;
import org.jetbrains.annotations.Nullable;

import io.netty.buffer.Unpooled;
import net.arcana.friendsforlife.Packets;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.storage.DimensionDataStorage;

public class PetTracker extends SavedData {
    private final Map<UUID, PetData> tracked = new HashMap<>();
    private final Set<UUID> recreated = new HashSet<>(); // Entities that have been recreated and must be removed

    public void addPet(TamableAnimal tame, ServerPlayer player, ItemStack item, long tameTimestamp) {
        FabricEntrypoint.LOGGER.info("adding new entity to be tracked: " + tame.getName());
        if (tracked.values().stream().noneMatch(pet -> pet.entityUuid.equals(tame.getUUID()))) {
            CompoundTag collar = new CompoundTag();
            item.save(collar);
            PetData pd = new PetData(tame, player, collar, tameTimestamp);
            tracked.put(pd.uuid, pd);
            var petList = PetTracker.serializePetList(player.getServer());
            ServerPlayNetworking.send(player, Packets.RESPONSE_PET_TRACKER, petList);
        }
    }

    public void removePet(MinecraftServer server, UUID uuid) {
        var player = server.getPlayerList().getPlayer(tracked.get(uuid).ownerUuid);
        PetData petData = tracked.get(uuid);
        if (petData != null) {
            petData.onRemoved(server);
        }
        tracked.remove(uuid);
        if (player == null) {
            FabricEntrypoint.LOGGER.error(FabricEntrypoint.MOD_ID, "Could not find player to update pet list for removed pet: " + uuid);
            return;
        }
        var petList = PetTracker.serializePetList(server);
        ServerPlayNetworking.send((ServerPlayer) player, Packets.RESPONSE_PET_TRACKER, petList);
    }

    public Map<UUID, PetData> getTrackedMap() {
        return tracked;
    }

    /** Map of entities marked for deletion */
    public Set<UUID> getRecreatedMap() {
        return recreated;
    }

    public static PetTracker get(MinecraftServer server) {
        DimensionDataStorage manager = server.overworld().getDataStorage();
        return manager.computeIfAbsent(PetTracker::fromNBT, PetTracker::new, "friends_for_life_tracked_pets");
    }

    public PetData get(UUID uuid) {
        return tracked.get(uuid);
    }

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

    /** Serializes pet list for player */
    public static FriendlyByteBuf serializePetList(MinecraftServer server) {
        List<PetData> pets = new ArrayList<>(PetTracker.get(server).tracked.values());
        pets.sort((a, b) -> Long.compare(a.tameTimestamp, b.tameTimestamp));

        FriendlyByteBuf out = new FriendlyByteBuf(Unpooled.buffer());
        out.writeInt(pets.size());

        for (PetData pet : pets) {
            serializePetData(out, pet, server);
            // out.writeUuid(pet.uuid);

            // var data = pet.getEntity(player.getServer()).join();
            // if (data != null && data.entity() != null) {
            // writeEntityData(out, data.entity(), null);
            // } else if (data != null && data.shoulderNbt() != null) {
            // writeEntityData(out, null, data.shoulderNbt());
            // } else {
            // writeEntityData(out, null, pet.getEntityData());
            // }

            // out.writeString(pet.position.toShortString());
            // out.writeLong(pet.homePosition != null ? pet.homePosition.asLong() : 0L);
            // out.writeString(pet.homeDimension != null ?
            // pet.homeDimension.getValue().toString() : "");
        }

        return out;
    }

    @Override
    public CompoundTag save(CompoundTag nbt) {
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

        return nbt;
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
                    ? entity.getCustomName().getString()
                    : entity.getType().getDescription().getString();
            out.writeUtf(name);
        } else if (nbt != null) {
            out.writeUtf(nbt.getString("id"));
            out.writeNbt(nbt);

            String name = nbt.contains("CustomName")
                    ? Component.Serializer.fromJson(nbt.getString("CustomName")).getString()
                    : BuiltInRegistries.ENTITY_TYPE.get(new ResourceLocation(nbt.getString("id"))).getDescription().getString();
            out.writeUtf(name);
        }
    }

}
