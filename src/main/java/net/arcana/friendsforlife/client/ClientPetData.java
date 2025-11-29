package net.arcana.friendsforlife.client;

import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

public class ClientPetData {
    public final UUID uuid;
    public String name;
    public String position;
    CompoundTag entityData;
    public final EntityType<?> type;
    @Nullable
    public BlockPos homePosition;
    @Nullable
    public ResourceLocation homeDimension;

    public ClientPetData(UUID uuid, String entityType, CompoundTag entityData, String name, String position,
            BlockPos homePosition, ResourceLocation homeDimension) {
        this.uuid = uuid;
        this.name = name;
        this.position = position;
        this.homePosition = homePosition;
        this.homeDimension = homeDimension;
        this.type = BuiltInRegistries.ENTITY_TYPE.get(new ResourceLocation(entityType));

        this.entityData = entityData;
    }

    // Do not call on network thread
    public LivingEntity getRenderingEntity(Level world) {
        LivingEntity entity = (LivingEntity) type.create(world);
        if (entity != null) {
            entity.load(entityData);
        }
        return entity;
    }

    @Nullable
    public UUID getOwnerUuid() {
        if (this.entityData.contains("Owner")) {
            return this.entityData.getUUID("Owner");
        } else {
            return new UUID(0L,0L);
        }
    }

}
