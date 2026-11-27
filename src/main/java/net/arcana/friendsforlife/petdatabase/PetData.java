package net.arcana.friendsforlife.petdatabase;

import net.arcana.friendsforlife.*;
import net.arcana.friendsforlife.accessors.ShoulderAccessor;
import net.arcana.friendsforlife.accessors.TrackableEntityDecorator;
import net.arcana.friendsforlife.homeblock.HomeBlock.HomeEntry;
import net.arcana.friendsforlife.packets.PetDataUpdatePacket;
import net.arcana.friendsforlife.packets.PetDownedUpdatePacket;
import net.arcana.friendsforlife.util.CompatComponent;
import net.arcana.friendsforlife.util.CompatConfig;
import net.arcana.friendsforlife.util.CompatNbt;
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
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
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
    /**
     * Counts the pet's summons. Each summon stamps the new copy with the new count (in the entity's NBT under
     * {@link #GENERATION_KEY}), so any copy that loads with a lower count is a leftover from before a summon, whatever
     * its entity UUID. Copies made by other mods keep the stamp and are still recognised as the pet.
     */
    public int generation = 0;
    public static final String GENERATION_KEY = "FriendsForLifeGeneration";
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
        EntityType<?> type = getEntityType();

        return type == null || !type.is(Tags.PET_ALWAYS_PASSIVE);
    }

    /**
     * The pet's entity type id. Snapshots taken by versions before 1.4.0 used Entity#save, which writes
     * nothing (not even "id") when the entity is riding something or has been removed, e.g. by a mod
     * that replaces the wild mob with a tamed one. "EntityType" is written separately at tame time, so
     * it is the fallback.
     */
    public String getEntityTypeId() {
        if (entityData == null) {
            return "";
        }
        String id = CompatNbt.getString(entityData, "id");
        return id.isEmpty() ? CompatNbt.getString(entityData, "EntityType") : id;
    }

    @Nullable
    public EntityType<?> getEntityType() {
        return CompatNbt.entityTypeOrNull(getEntityTypeId());
    }

    /**
     * Why this pet's saved data can't be used to recreate it, or null if it can.
     */
    @Nullable
    public String getBrokenReason() {
        if (entityData == null) {
            return "it has no saved entity data";
        }
        if (getEntityType() == null) {
            return "its entity type '" + getEntityTypeId() + "' is unknown";
        }
        // 1.3.x saved pets that were riding something without an "id". Summoning one recreated it as a pig
        // and replaced the saved data with the pig's spawn data, which has no "Pos". A snapshot of a live pet
        // always has "Pos", so such pets can still be repaired from the real entity; these can't.
        if (!entityData.contains("id") && !entityData.contains("Pos")) {
            return "it was turned into a pig by an earlier version";
        }
        return null;
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
        // A pet that was collared before keeps its count, so its earlier copies are still recognised as old
        this.generation = ((TrackableEntityDecorator) entity).friendsforlife_getGeneration();
        this.position = entity.blockPosition();
        this.OwnerName = player.getScoreboardName();
        this.ownerUuid = player.getUUID();
        this.collar = collarItem;
        this.entityData = CompatNbt.saveEntity(entity);
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
                getEntityTypeId(),
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
        CompatNbt.putUUID(tag, "FriendsForLifeEntityUUID", uuid);
        // Saved so that a second copy from the same summon can be recognised as a duplicate (see generation)
        if (entityUuid != null) {
            CompatNbt.putUUID(tag, "EntityUUID", entityUuid);
        }
        tag.putInt("Generation", generation);

        CompatNbt.putUUID(tag, "OwnerUuid", ownerUuid);
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

        UUID friendsforlife_uuid = CompatNbt.getUUID(tag, "FriendsForLifeEntityUUID");
        if (friendsforlife_uuid == null) {
            return null;
        }

        UUID OwnerUuid = CompatNbt.getUUID(tag, "OwnerUuid");
        String OwnerName = CompatNbt.getString(tag, "OwnerName");
        CompoundTag collarItem = CompatNbt.getCompound(tag, "collarItem");

        BlockPos pos = new BlockPos(CompatNbt.getInt(tag, "x"), CompatNbt.getInt(tag, "y"), CompatNbt.getInt(tag, "z"));

        long tameDate = CompatNbt.getLong(tag, "tameDate");

        BlockPos homePos = tag.contains("homePos") ? net.minecraft.core.BlockPos.of(CompatNbt.getLong(tag, "homePos")) : null;
        ResourceKey<Level> homeDim = tag.contains("homeDim") && !CompatNbt.getString(tag, "homeDim").isEmpty()
                ? ResourceKey.create(Registries.DIMENSION, new CompatResourceLocation(CompatNbt.getString(tag, "homeDim")).unwrap())
                : null;

        CompoundTag entityData = tag.contains("EntityData") ? CompatNbt.getCompound(tag, "EntityData") : null;
        String followModeStr = CompatNbt.getString(tag, "FollowMode");
        FollowMode mode;
        if (followModeStr.isEmpty()) {
            mode = FollowMode.COMPANION;
        } else {
            mode = FollowMode.valueOf(followModeStr);
        }
        boolean isDowned = CompatNbt.getBoolean(tag, "IsDowned");

        PetData pd = new PetData(friendsforlife_uuid, pos, OwnerUuid, OwnerName, collarItem, tameDate, homePos, homeDim,
                entityData, isDowned, mode);
        pd.entityUuid = CompatNbt.getUUID(tag, "EntityUUID"); // null for saves from before 1.4.0, until the entity loads
        pd.generation = CompatNbt.getInt(tag, "Generation"); // 0 for saves from before 1.4.0, like their entities
        return pd;
    }

    public void updateEntityData(MinecraftServer server, Consumer<CompoundTag> editor) {
        if (entityData == null)
            return;
        editor.accept(entityData);

        ServerPlayer player = server.getPlayerList().getPlayer(ownerUuid);
        if (player != null) {
            Net.CHANNEL.serverHandle(player).send(new PetDataUpdatePacket(toEntry()));
        }
    }

    /**
     * Re-snapshots the entity's current state into the stored entity data.
     */
    public void updateEntityData(MinecraftServer server, Entity entity) {
        String typeId = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).toString();
        String storedTypeId = getEntityTypeId();
        if (!storedTypeId.isEmpty() && !storedTypeId.equals(typeId)) {
            // The entity isn't the pet any more (e.g. it was recreated as a pig before 1.4.0). Saving it
            // would overwrite the pet's real data, which is still enough to recreate it correctly.
            Init.LOGGER.warn("Not saving pet {}: entity is {} but the pet is {}", uuid, typeId, storedTypeId);
            return;
        }
        updateEntityData(server, tag -> {
            // Replace the snapshot rather than merging into it: anything the pet no longer has (a saddle, armor, a
            // chest or another mod's cart, expired effects) would otherwise come back from the old snapshot on summon
            for (String key : List.copyOf(CompatNbt.keys(tag))) {
                tag.remove(key);
            }
            tag.merge(CompatNbt.saveEntityWithoutId(entity));
            // Repairs snapshots that are missing their type id (see getEntityTypeId)
            tag.putString("id", typeId);
            tag.putString("EntityType", typeId);
        });
    }

    @Nullable
    public HomeEntry getHomeBlock() {
        if (homePosition == null || homeDimension == null)
            return null;
        return new HomeEntry(homePosition, homeDimension);
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
        updateEntityData(server, entity); // rebuild from its current state, not the last snapshot
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

        String typeId = getEntityTypeId();
        if (CompatNbt.getString(entityData, "id").isEmpty()) {
            Init.LOGGER.warn("Pet {} has no saved entity id (snapshot from before 1.4.0); recreating it as '{}'", uuid, typeId);
        }
        EntityType<?> type = CompatNbt.entityTypeOrNull(typeId);
        if (type == null) {
            // Spawning CompatNbt.entityType(typeId) here would fall back to the registry default and turn the pet into a pig
            Init.LOGGER.error("Can't recreate pet {}: unknown entity type '{}'", uuid, typeId);
            return null;
        }
        UUID oldUuid = entityUuid;

        // Discard entity if it exists
        if (deleteEntity) {
            Entity current = getEntity(server).join().entity();
            if (current != null) {
                // Rebuild from its current state, not the last snapshot (which only updates on unload or rename),
                // or e.g. armor sheared off since then would come back
                updateEntityData(server, current);
                current.discard();
            } else if (oldUuid != null) {
                // Not needed since the summon count (see generation), which also catches copies whose entity UUID
                // changed; kept as a fallback
                PetTracker.get(server).getRecreatedEntityMap().add(oldUuid);
            }
        }

        Entity entity = CompatNbt.createEntity(type, server.getLevel(worldKey));
        if (!(entity instanceof LivingEntity livingEntity))
            return null;

        // Remove from shoulder
        ServerPlayer player = server.getPlayerList().getPlayer(ownerUuid);
        if (player != null) {
            CompoundTag left = player.getShoulderEntityLeft();
            CompoundTag right = player.getShoulderEntityRight();

            if (!left.isEmpty() && CompatNbt.hasUUID(left, "UUID") && CompatNbt.getUUID(left, "UUID").equals(entityUuid)) {
                ((ShoulderAccessor) player).friendsforlife_clearShoulder(true);
            }

            if (!right.isEmpty() && CompatNbt.hasUUID(right, "UUID") && CompatNbt.getUUID(right, "UUID").equals(entityUuid)) {
                ((ShoulderAccessor) player).friendsforlife_clearShoulder(false);
            }
        }

        //? if >=1.21.1 {
        /*boolean hasLeash = entityData.contains("leash");
        *///?} else {

        boolean hasLeash = entityData.contains("Leash");
        //?}

        CompoundTag nbtCopy = entityData.copy();
        nbtCopy.putString("id", typeId);

        entityUuid = UUID.randomUUID();
        CompatNbt.putUUID(nbtCopy, "UUID", entityUuid);
        // Every copy saved before this summon now has a lower count and is removed when it loads
        generation++;
        nbtCopy.putInt(GENERATION_KEY, generation);
        PetTracker.get(server).setDirty();
        nbtCopy.remove("Pos");
        nbtCopy.remove("Motion");
        nbtCopy.remove("Rotation");
        // Remove sitting because we can't rely on entity.setSitting to work on new
        // entities
        nbtCopy.remove("Sitting");
        //? if >=1.21.1 {
        /*nbtCopy.remove("leash");
        *///?} else {
        nbtCopy.remove("Leash");
        //?}
        nbtCopy.remove("DeathTime");
        nbtCopy.remove("ActiveEffects");
        nbtCopy.remove("HurtTime");
        nbtCopy.remove("HurtByTimestamp");
        nbtCopy.remove("SleepingX");
        nbtCopy.remove("SleepingY");
        nbtCopy.remove("SleepingZ");
        nbtCopy.remove("Brain");
        nbtCopy.remove("Pose");

        if (CompatNbt.getFloat(nbtCopy, "Health") <= 0) {
            nbtCopy.putFloat("Health", livingEntity.getMaxHealth());
        }

        CompatNbt.loadEntity(entity, nbtCopy);
        entityData = nbtCopy;

        entity.setPos(x, y, z);

        Objects.requireNonNull(server.getLevel(worldKey)).addFreshEntity(entity);

        CompatNbt.moveTo(entity, x, y, z, entity.getYRot(), entity.getXRot());

        if (hasLeash) {
            ItemEntity leash = new ItemEntity(
                    Objects.requireNonNull(server.getLevel(worldKey)),
                    x + 0.5,
                    y + 0.5,
                    z + 0.5,
                    new ItemStack(Items.LEAD)
            );
            Objects.requireNonNull(server.getLevel(worldKey)).addFreshEntity(leash);
        }

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
        String shoulderId = CompatNbt.getString(shoulderNbt, "id");
        String savedId = CompatNbt.getString(savedEntityData, "id");
        if (savedId.isEmpty()) savedId = CompatNbt.getString(savedEntityData, "EntityType");
        if (!shoulderId.equals(savedId))
            return false;

        // Compare variant (for parrots)
        if (shoulderId.equals("minecraft:parrot")) {
            int shoulderVariant = CompatNbt.getInt(shoulderNbt, "Variant");
            int savedVariant = CompatNbt.getInt(savedEntityData, "Variant");
            if (shoulderVariant != savedVariant)
                return false;
        }

        // Compare custom customName if present
        return Objects.equals(CompatNbt.getCustomName(shoulderNbt), CompatNbt.getCustomName(savedEntityData));
    }

}