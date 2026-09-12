package net.arcana.friendsforlife.petdatabase;

import net.arcana.friendsforlife.*;
import net.arcana.friendsforlife.accessors.ShoulderAccessor;
import net.arcana.friendsforlife.accessors.TrackableEntityDecorator;
import net.arcana.friendsforlife.homeblock.HomeBlock.HomeBlockHandle;
import net.arcana.friendsforlife.packets.PetDataUpdatePacket;
import net.arcana.friendsforlife.packets.PetDownedUpdatePacket;
import net.arcana.friendsforlife.util.CompatComponent;
import net.arcana.friendsforlife.util.CompatConfig;
import net.arcana.friendsforlife.util.CompatResourceLocation;
import net.arcana.friendsforlife.util.NBTCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.*;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * Used to manage the Pet's state
 */
public class PetData {
    public enum FollowMode {
        COMPANION(0),
        GUARDIAN(1);

        public final int id;

        FollowMode(int id) {
            this.id = id;
        }

        public static FollowMode fromId(int id) {
            for (FollowMode m : values()) {
                if (m.id == id) return m;
            }
            throw new IllegalArgumentException("Invalid FollowMode id: " + id);
        }
    }

    /**
     * Represents the UUID of the PetData
     *
     * @see #entityUuid
     */
    public final UUID uuid;

    /**
     * Represents the last known entity of the pet
     */
    @Nullable
    public UUID entityUuid;
    public BlockPos position;
    public CompoundTag collar;
    public final UUID ownerUuid;
    public final String OwnerName;
    public BlockPos homePosition;
    public FollowMode mode;
    /**
     * List of modes available for pet
     */
    public List<FollowMode> modes;

    public ResourceKey<Level> homeDimension;
    /**
     * The time, in milliseconds, when the pet was tamed
     */
    public final long tameTimestamp;

    public boolean isDowned = false;

    private CompoundTag entityData;

    public CompoundTag getEntityData() {
        return entityData;
    }

    public boolean canEntityFight() {
        assert this.entityData != null;
        var entityTypeString = this.entityData.getString("id");
        ResourceLocation rl = new CompatResourceLocation(entityTypeString).unwrap();
        EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.get(rl);

        return !type.is(Tags.PET_ALWAYS_PASSIVE);
    }

    /**
     * Attempts to set mode on PetData.
     *
     */
    public FollowMode trySetMode(FollowMode mode, Entity entity) {
        if (mode == FollowMode.GUARDIAN && canEntityFight()) {
            this.mode = mode;
        } else {
            this.mode = FollowMode.COMPANION;
        }

        TrackableModeHelper.setTrackableMode(this.mode, entity);
        return this.mode;
    }

    public FollowMode trySetMode(FollowMode mode, MinecraftServer server) {
        var pdh = getEntity(server).join();
        if (pdh.entity() != null) {
            mode = trySetMode(mode, pdh.entity());
        }
        return mode;
    }

    public List<FollowMode> getModes() {
        List<FollowMode> modes = new ArrayList<>();
        modes.add(FollowMode.COMPANION);
        if (canEntityFight()) {
            modes.add(FollowMode.GUARDIAN);
        }
        return modes;
    }

    public record PetEntityHandle(@Nullable Entity entity, @Nullable CompoundTag shoulderNbt) {
    }

    public PetData(Entity entity, Entity player, CompoundTag collarItem, long tameDate) {
        this.uuid = ((TrackableEntityDecorator) entity).friendsforlife_getUuid(); // Separate UUID from entity for tracking purposes
        this.entityUuid = entity.getUUID();
        this.position = entity.blockPosition();
        this.OwnerName = player.getScoreboardName();
        this.ownerUuid = player.getUUID();
        this.collar = collarItem;
        this.entityData = new CompoundTag();
        entity.save(this.entityData);
        this.entityData.putString("EntityType", String.valueOf(BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType())));
        trySetMode(FollowMode.COMPANION, entity);
        this.modes = getModes();
        this.tameTimestamp = tameDate;
    }

    public PetData(UUID friendsforlifeUuid, BlockPos pos, UUID OwnerUuid, String OwnerName, CompoundTag collarItem,
                   long tameDate, BlockPos homePosition, ResourceKey<Level> homeDimension, CompoundTag entityData, boolean isDowned, FollowMode mode) {
        this.uuid = friendsforlifeUuid;
        this.position = pos;
        this.OwnerName = OwnerName;
        this.ownerUuid = OwnerUuid;
        this.collar = collarItem;
        this.tameTimestamp = tameDate;
        this.homeDimension = homeDimension;
        this.homePosition = homePosition;
        this.entityData = entityData;
        this.mode = mode;
        this.isDowned = isDowned;
        this.modes = getModes();
    }

    // The model used in networking
    public record Entry(
            UUID uuid,
            String entityTypeId,
            byte[] nbtBytes,
            String position,
            long homePosLong,
            String homeDimStr,
            boolean isDowned,
            FollowMode followMode,
            String followModes
    ) {

        // Factory to create Entry from a CompoundTag
        public static Entry of(UUID uuid, String entityTypeId, CompoundTag nbt,
                               String position, long homePosLong,
                               String homeDimStr,
                               boolean isDowned,
                               FollowMode followMode,
                               String followModes) {
            return new Entry(uuid, entityTypeId, NBTCodec.serialize(nbt),
                    position, homePosLong, homeDimStr, isDowned, followMode, followModes);
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
                this.homeDimension != null ? this.homeDimension.location().toString() : "",
                 //?} else {
                /*this.homeDimension != null ? this.homeDimension.location().toString() : "",
                *///?}
                this.isDowned,
                this.mode,
                this.modes.stream()
                        .map(FollowMode::name)
                        .collect(Collectors.joining(","))
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

        tag.putString("FollowMode", mode.name());
        tag.putBoolean("IsDowned", false); // Remove downed because cooldown scheduled task does not persist
        return tag;
    }

    @Nullable
    /**
     * Deserialized from save. Not used in networking
     */
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

        BlockPos homePos = tag.contains("homePos") ? net.minecraft.core.BlockPos.of(tag.getLong("homePos")) : null;
        ResourceKey<Level> homeDim = tag.contains("homeDim") && !tag.getString("homeDim").isEmpty()
                ? ResourceKey.create(Registries.DIMENSION, new CompatResourceLocation(tag.getString("homeDim")).unwrap())
                : null;

        CompoundTag entityData = tag.contains("EntityData") ? tag.getCompound("EntityData") : null;
        String followModeStr = tag.getString("FollowMode");
        FollowMode mode;
        if (followModeStr.isEmpty()) {
            mode = FollowMode.COMPANION;
        } else {
            mode = FollowMode.valueOf(followModeStr);
        }
        boolean isDowned = tag.getBoolean("IsDowned");

        return new PetData(friendsforlife_uuid, pos, OwnerUuid, OwnerName, collarItem, tameDate, homePos, homeDim,
                entityData, isDowned, mode);
    }

    public void updateEntityData(MinecraftServer server, Consumer<CompoundTag> editor) {
        if (entityData == null)
            return;
        entityData.remove("SaddleItem");
        //? if >=1.21.1 {
        /*entityData.remove("body_armor_item");
        *///?} else {
        entityData.remove("ArmorItem");
        entityData.remove("DecorItem");
        //?}
        editor.accept(entityData);

        ServerPlayer player = server.getPlayerList().getPlayer(ownerUuid);
        if (player != null) {
            Net.CHANNEL.serverHandle(player).send(new PetDataUpdatePacket(toEntry()));
        }
    }

    @Nullable
    public HomeBlockHandle getHomeBlock() {
        if (homePosition == null || homeDimension == null)
            return null;
        return new HomeBlockHandle(homePosition, homeDimension);
    }

    /**
     *
     * @return entity or nbt if entity is on player's shoulder
     */
    public CompletableFuture<PetEntityHandle> getEntity(MinecraftServer server) {
        if (server == null)
            return CompletableFuture.completedFuture(new PetEntityHandle(null, null));

        for (ServerLevel world : server.getAllLevels()) {
            Entity entity = null;
            if (this.entityUuid != null) {
                entity = world.getEntity(this.entityUuid);
            }
            if (entity != null) {
                return CompletableFuture.completedFuture(new PetEntityHandle((Entity) entity, null));
            }
        }

        for (ServerLevel world : server.getAllLevels()) {
            for (Entity entity : world.getAllEntities()) {
                if (entity instanceof OwnableEntity ownable && ownable instanceof TrackableEntityDecorator data) {
                    if (data.friendsforlife_getUuid().equals(uuid)) {
                        return CompletableFuture.completedFuture(new PetEntityHandle((Entity) ownable, null));
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


    public LivingEntity recreateEntity(MinecraftServer server, ResourceKey<Level> worldKey, int x, int y, int z, Entity entity) {
        entity.discard();
        return recreateEntity(server, worldKey, x, y, z, false);
    }

    public LivingEntity recreateEntity(MinecraftServer server, ResourceKey<Level> worldKey, int x, int y, int z) {
        return recreateEntity(server, worldKey, x, y, z, true);
    }

    /**
     * Recreate Entity
     *
     * @return The recreated TrackableEntity, or null if recreation failed
     */
    @Nullable
    public LivingEntity recreateEntity(MinecraftServer server, ResourceKey<Level> worldKey, int x, int y, int z, boolean deleteEntity) {
        if (server == null || entityData == null)
            return null;

        String typeId = entityData.getString("id");
        EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.get(new CompatResourceLocation(typeId).unwrap());
        UUID oldUuid = entityUuid;

        // Discard entity if it exists
        if (deleteEntity) {
            getEntity(server).thenAccept(pdh -> {
                if (pdh != null && pdh.entity() != null) {
                    pdh.entity().discard();
                } else {
                    PetTracker pt = PetTracker.get(server);
                    pt.getRecreatedEntityMap().add(oldUuid); // Mark for deletion
                }
            });
        }

        Entity entity = type.create(server.getLevel(worldKey));
        if (!(entity instanceof LivingEntity livingEntity))
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
        nbtCopy.remove("HurtTime");
        nbtCopy.remove("HurtByTimestamp");
        nbtCopy.remove("SleepingX");
        nbtCopy.remove("SleepingY");
        nbtCopy.remove("SleepingZ");
        nbtCopy.remove("Brain");
        nbtCopy.remove("Pose");

        if (nbtCopy.getFloat("Health") <= 0) {
            nbtCopy.putFloat("Health", livingEntity.getMaxHealth());
        }

        entity.load(nbtCopy);
        entityData = nbtCopy;

        entity.setPos(x, y, z);

        Objects.requireNonNull(server.getLevel(worldKey)).addFreshEntity(entity);

        entity.moveTo(x, y, z, entity.getYRot(), entity.getXRot());
        return livingEntity;
    }

    /**
     *
     * @param server
     */
    public void setDowned(MinecraftServer server) {
        this.isDowned = true;
        Net.CHANNEL.serverHandle(server).send(new PetDownedUpdatePacket(this.uuid, this.isDowned));
        var entity = getEntity(server).join().entity();
        if (entity instanceof TamableAnimal tame) {
            tame.setPose(Pose.SITTING);
        }
        TaskScheduler.schedule(() -> {
            this.isDowned = false;
            var newEntity = getEntity(server).join().entity();
            if (newEntity != null) {
                ((TrackableEntityDecorator) newEntity).friendsforlife_setDowned(false);
            }
            Net.CHANNEL.serverHandle(server).send(new PetDownedUpdatePacket(this.uuid, this.isDowned));
        }, CompatConfig.petRespawnTimer() * 20);
    }

    public void onRemoved(MinecraftServer server) {
        OcarinaRegistry.get(server).unregisterAllForPet(this.uuid, server);
        var peh = getEntity(server).join();
        if (peh.entity() != null) {
            TameableWanderHelper.stopWandering(peh.entity());
            peh.entity().setInvulnerable(false);
        } else {
            Init.LOGGER.warn("Unable to find entity for removed pet");
        }
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

}