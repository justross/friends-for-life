package net.arcana.friendsforlife.petdatabase;

import net.arcana.friendsforlife.TameableWanderHelper;
import net.arcana.friendsforlife.accessors.ShoulderAccessor;
import net.arcana.friendsforlife.accessors.TameableEntityDecorator;
import net.arcana.friendsforlife.homeblock.HomeBlock.HomeBlockHandle;
import net.arcana.friendsforlife.loaders.fabric.FabricEntrypoint;
import net.arcana.friendsforlife.packets.PetListPacket;
import net.arcana.friendsforlife.util.CompatComponent;
import net.arcana.friendsforlife.util.CompatResourceLocation;
import net.arcana.friendsforlife.util.NBTCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public class PetData {
    /**
     * Represents the UUID of the PetData
     *
     * @see #entityUuid
     */
    public final UUID uuid;
    public UUID entityUuid;
    public BlockPos position;
    public CompoundTag collar;
    public final UUID ownerUuid;
    public final String OwnerName;
    public BlockPos homePosition;
    public ResourceKey<Level> homeDimension;
    /**
     * The time, in milliseconds, when the pet was tamed
     */
    public final long tameTimestamp;
    private CompoundTag entityData;

    public CompoundTag getEntityData() {
        return entityData;
    }

    public record PetEntityHandle(@Nullable TamableAnimal entity, @Nullable CompoundTag shoulderNbt) {
    }

    public PetData(TamableAnimal tame, Entity player, CompoundTag collarItem, long tameDate) {
        this.uuid = ((TameableEntityDecorator) tame).friendsforlife_getUuid(); // Separate UUID from entity for tracking
        // purposes
        this.entityUuid = tame.getUUID();
        this.position = tame.blockPosition();
        this.OwnerName = player.getScoreboardName();
        this.ownerUuid = player.getUUID();
        this.collar = collarItem;
        this.entityData = new CompoundTag();
        tame.saveAsPassenger(this.entityData);
        this.entityData.putString("EntityType", String.valueOf(BuiltInRegistries.ENTITY_TYPE.getKey(tame.getType())));
        this.tameTimestamp = tameDate;
    }

    public PetData(UUID friendsforlifeUuid, BlockPos pos, UUID OwnerUuid, String OwnerName, CompoundTag collarItem,
                   long tameDate, BlockPos homePosition, ResourceKey<Level> homeDimension, CompoundTag entityData) {
        this.uuid = friendsforlifeUuid;
        this.entityUuid = uuid;
        this.position = pos;
        this.OwnerName = OwnerName;
        this.ownerUuid = OwnerUuid;
        this.collar = collarItem;
        this.tameTimestamp = tameDate;
        this.homeDimension = homeDimension;
        this.homePosition = homePosition;
        this.entityData = entityData;
    }

    public record Entry(
            UUID uuid,
            String entityTypeId,
            byte[] nbtBytes,
            String position,
            long homePosLong,
            String homeDimStr
    ) {

        // Factory to create Entry from a CompoundTag
        public static Entry of(UUID uuid, String entityTypeId, CompoundTag nbt,
                               String position, long homePosLong, String homeDimStr) {
            return new Entry(uuid, entityTypeId, NBTCodec.serialize(nbt),
                    position, homePosLong, homeDimStr);
        }

        // Decode the stored bytes back to CompoundTag
        public CompoundTag nbt() {
            return NBTCodec.deserialize(nbtBytes);
        }
    }

    public Entry toEntry() {
        return Entry.of(
                this.uuid,
                this.getEntityData().getString("id"),
                this.getEntityData(),
                this.position.toShortString(),
                this.homePosition != null ? this.homePosition.asLong() : 0L,
                //? if <1.21 {
                /*this.homeDimension != null ? this.homeDimension.location().toString() : ""
                 *///?} else {
                this.homeDimension != null ? this.homeDimension.location().toString() : ""
                //?}
        );
    }

    public CompoundTag toNbt() {
        CompoundTag tag = new CompoundTag();
        tag.putUUID("FriendsForLifeEntityUUID", uuid);

        tag.putUUID("OwnerUuid", ownerUuid);
        tag.putString("OwnerName", OwnerName);
        tag.put("collarItem", collar);

        tag.putInt("x", position.getX());
        tag.putInt("y", position.getY());
        tag.putInt("z", position.getZ());

        tag.putLong("tameDate", tameTimestamp);

        tag.putLong("homePos", homePosition != null ? homePosition.asLong() : 0L);
        tag.putString("homeDim", homeDimension != null ? homeDimension.location().toString() : "");

        tag.put("EntityData", entityData);
        return tag;
    }

    @Nullable
    public static PetData fromNbt(CompoundTag tag) {
        if (tag == null) {
            return null;
        }

        UUID friendsforlife_uuid = tag.getUUID("FriendsForLifeEntityUUID");

        UUID OwnerUuid = tag.getUUID("OwnerUuid");
        String OwnerName = tag.getString("OwnerName");
        CompoundTag collarItem = tag.getCompound("collarItem");

        BlockPos pos = new BlockPos(tag.getInt("x"), tag.getInt("y"), tag.getInt("z"));

        long tameDate = tag.getLong("tameDate");

        BlockPos homePos = tag.contains("homePos") ? BlockPos.of(tag.getLong("homePos")) : null;
        ResourceKey<Level> homeDim = tag.contains("homeDim") && !tag.getString("homeDim").isEmpty()
                ? ResourceKey.create(Registries.DIMENSION, new CompatResourceLocation(tag.getString("homeDim")).unwrap())
                : null;

        CompoundTag entityData = tag.contains("EntityData") ? tag.getCompound("EntityData") : null;

        return new PetData(friendsforlife_uuid, pos, OwnerUuid, OwnerName, collarItem, tameDate, homePos, homeDim,
                entityData);
    }

    public void updateEntityData(Consumer<CompoundTag> editor) {
        if (entityData == null)
            return;
        editor.accept(entityData);
    }

    @Nullable
    public HomeBlockHandle getHomeBlock() {
        if (homePosition == null || homeDimension == null)
            return null;
        return new HomeBlockHandle(homePosition, homeDimension);
    }

    /**
     *
     * @param server
     * @return entity or nbt if entity is on player's shoulder
     * @Nullable
     */
    public CompletableFuture<PetEntityHandle> getEntity(MinecraftServer server) {
        if (server == null)
            return CompletableFuture.completedFuture(null);

        for (ServerLevel world : server.getAllLevels()) {
            var entity = world.getEntity(this.entityUuid);
            if (entity != null) {
                return CompletableFuture.completedFuture(new PetEntityHandle((TamableAnimal) entity, null));
            }
        }

        for (ServerLevel world : server.getAllLevels()) {
            for (Entity entity : world.getAllEntities()) {
                if (entity instanceof TamableAnimal tame && tame instanceof TameableEntityDecorator data) {
                    if (data.friendsforlife_getUuid().equals(uuid)) {
                        return CompletableFuture.completedFuture(new PetEntityHandle((TamableAnimal) tame, null));
                    }
                }
            }
        }

        var player = server.getPlayerList().getPlayer(ownerUuid);
        if (player != null) {

            CompoundTag left = player.getShoulderEntityLeft();
            CompoundTag right = player.getShoulderEntityRight();
            // Check if the entity is on a player's shoulder
            for (CompoundTag nbt : List.of(left, right)) {
                if (nbt != null && !nbt.isEmpty()) {
                    if (entityDataMatchesShoulder(nbt, entityData)) {
                        return CompletableFuture.completedFuture(new PetEntityHandle(null, nbt));
                    }
                }
            }

        }

        return CompletableFuture.completedFuture(new PetEntityHandle(null, null));
    }

    /**
     * Recreate Entity
     *
     * @param server
     * @param worldKey - Dimension to spawn entity
     * @param x        - Position to spawn entity
     * @param y
     * @param z
     * @return The recreated TameableEntity, or null if recreation failed
     */
    @SuppressWarnings("DataFlowIssue")
    @Nullable
    public TamableAnimal recreateEntity(MinecraftServer server, ResourceKey<Level> worldKey, int x, int y,
                                        int z) {
        if (server == null || entityData == null)
            return null;

        String typeId = entityData.getString("id");
        EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.get(new CompatResourceLocation(typeId).unwrap());

        // Discard entity if it exists
        getEntity(server).thenAccept(pdh -> {
            if (pdh != null && pdh.entity() != null) {
                pdh.entity().discard();
            }
        });

        Entity entity = type.create(server.getLevel(worldKey));
        if (!(entity instanceof TamableAnimal tame))
            return null;

        // Remove from shoulder
        ServerPlayer player = server.getPlayerList().getPlayer(ownerUuid);
        if (player != null) {
            CompoundTag left = player.getShoulderEntityLeft();
            CompoundTag right = player.getShoulderEntityRight();

            if (!left.isEmpty() && left.hasUUID("UUID") && left.getUUID("UUID").equals(entityUuid)) {
                ((ShoulderAccessor) player).friendsforlife_clearShoulder(entityUuid);
            }

            if (!right.isEmpty() && right.hasUUID("UUID") && right.getUUID("UUID").equals(entityUuid)) {
                ((ShoulderAccessor) player).friendsforlife_clearShoulder(entityUuid);
            }
        }

        CompoundTag nbtCopy = entityData.copy();
        UUID oldUuid = entityUuid;
        entityUuid = UUID.randomUUID();
        nbtCopy.putUUID("UUID", entityUuid);
        nbtCopy.remove("Pos");
        nbtCopy.remove("Motion");
        nbtCopy.remove("Rotation");
        // Remove sitting because we can't rely on entity.setSitting to work on new
        // entities
        nbtCopy.remove("Sitting");
        nbtCopy.remove("DeathTime");
        nbtCopy.remove("ActiveEffects");

        tame.load(nbtCopy);
        entityData = nbtCopy;
        server.getLevel(worldKey).addFreshEntity(tame);

        tame.moveTo(x, y, z, entity.getYRot(), entity.getXRot());
        PetTracker pt = PetTracker.get(server);
        pt.getRecreatedEntityMap().add(oldUuid); // Mark for deletion
        return tame;
    }

    private boolean entityDataMatchesShoulder(CompoundTag shoulderNbt, CompoundTag savedEntityData) {
        // Compare type
        String shoulderId = shoulderNbt.getString("id");
        String savedId = savedEntityData.getString("id");
        if (!shoulderId.equals(savedId))
            return false;

        // Compare variant (for parrots)
        if (shoulderId.equals("minecraft:parrot")) {
            int shoulderVariant = shoulderNbt.getInt("Variant");
            int savedVariant = savedEntityData.getInt("Variant");
            if (shoulderVariant != savedVariant)
                return false;
        }

        // Compare custom customName if present
        boolean shoulderHasName = shoulderNbt.contains("CustomName");
        boolean savedHasName = savedEntityData.contains("CustomName");

        if (shoulderHasName && savedHasName) {
            String shoulderName = Objects.requireNonNull(CompatComponent.fromJson(shoulderNbt.getString("CustomName"))).getString();
            String savedName = Objects.requireNonNull(CompatComponent.fromJson(savedEntityData.getString("CustomName"))).getString();
            return shoulderName.equals(savedName);
        }

        return !shoulderHasName && !savedHasName;
    }

    public void onRemoved(MinecraftServer server) {
        OcarinaRegistry.get(server).unregisterAllForPet(this.uuid, server);
        var peh = getEntity(server).join();
        if (peh.entity() != null) {
            TameableWanderHelper.stopWandering(peh.entity());
        } else {
            FabricEntrypoint.LOGGER.warn("Unable to find entity for removed pet");
        }

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
