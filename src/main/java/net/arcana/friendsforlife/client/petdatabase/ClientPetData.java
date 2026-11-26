package net.arcana.friendsforlife.client.petdatabase;

import net.arcana.friendsforlife.petdatabase.PetData;
import net.arcana.friendsforlife.util.CompatComponent;
import net.arcana.friendsforlife.util.CompatNbt;
import net.arcana.friendsforlife.util.CompatResourceLocation;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Stream;

public class ClientPetData {
    public final UUID uuid;
    public String position;
    public CompoundTag entityData;
    public final EntityType<?> type;
    @Nullable
    public BlockPos homePosition;
    @Nullable
    public ResourceLocation homeDimension;
    public PetData.FollowMode mode;
    public List<PetData.FollowMode> modes;
    public boolean isDowned;

    public ClientPetData(UUID uuid, String entityType, CompoundTag entityData, String position,
                         @Nullable BlockPos homePosition, @Nullable ResourceLocation homeDimension, boolean isDowned,
                         PetData.FollowMode mode, List<PetData.FollowMode> modes) {
        this.uuid = uuid;
        this.entityData = entityData;
        this.position = position;
        this.homePosition = homePosition;
        this.homeDimension = homeDimension;
        this.isDowned = isDowned;
        this.mode = mode;
        this.modes = modes;
        this.type = CompatNbt.entityType(entityType);
    }

    public static ClientPetData fromEntry(PetData.Entry entry) {
        UUID uuid = entry.uuid();
        String entityTypeId = entry.entityTypeId();
        CompoundTag nbt = entry.nbt();
        String position = entry.position();
        long homePosLong = entry.homePosLong();
        BlockPos homePosition = null;
        if (homePosLong != 0L) {
            homePosition = BlockPos.of(homePosLong);
        }
        ResourceLocation homeDimension = null;
        String homeDimStr = entry.homeDimStr();
        if (!homeDimStr.isEmpty()) {
            homeDimension = new CompatResourceLocation(homeDimStr).unwrap();
        }
        boolean isDowned = entry.isDowned();
        PetData.FollowMode mode = entry.followMode();
        List<PetData.FollowMode> followModes = Arrays.stream(entry.followModes().split(","))
                .map(PetData.FollowMode::valueOf)
                .toList();
        return new ClientPetData(uuid, entityTypeId, nbt, position, homePosition,
                homeDimension, isDowned, mode, followModes);
    }

    // Do not call on network thread
    public LivingEntity getRenderingEntity(Level world) {
        LivingEntity entity = (LivingEntity) CompatNbt.createEntity(type, world);
        if (entity != null) {
            CompatNbt.loadEntity(entity, entityData);
        }
        return entity;
    }

    @Nullable
    public UUID getOwnerUuid() {
        return CompatNbt.getUUID(this.entityData, "Owner");
    }

    public String getName() {
        Objects.requireNonNull(entityData);
        String customName = CompatNbt.getCustomName(entityData);
        if (customName != null) {
            return customName;
        }

        if (entityData.contains("EntityType")) {
            EntityType<?> type = CompatNbt.entityType(CompatNbt.getString(entityData, "EntityType"));
            return I18n.get(type.getDescriptionId());
        }

        return "Unknown Pet";
    }


}
