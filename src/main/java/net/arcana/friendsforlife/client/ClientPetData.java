package net.arcana.friendsforlife.client;

import java.util.UUID;

import org.jetbrains.annotations.Nullable;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class ClientPetData {
    public final UUID uuid;
    public String name;
    public String position;
    NbtCompound entityData;
    public final EntityType<?> type;
    @Nullable
    public BlockPos homePosition;
    @Nullable
    public Identifier homeDimension;

    public ClientPetData(UUID uuid, String entityType, NbtCompound entityData, String name, String position,
            BlockPos homePosition, Identifier homeDimension) {
        this.uuid = uuid;
        this.name = name;
        this.position = position;
        this.homePosition = homePosition;
        this.homeDimension = homeDimension;
        this.type = Registries.ENTITY_TYPE.get(new Identifier(entityType));

        this.entityData = entityData;
    }

    // Do not call on network thread
    public LivingEntity getRenderingEntity(World world) {
        LivingEntity entity = (LivingEntity) type.create(world);
        if (entity != null) {
            entity.readNbt(entityData);
        }
        return entity;
    }

    @Nullable
    public UUID getOwnerUuid() {
        if (this.entityData.contains("Owner")) {
            return this.entityData.getUuid("Owner");
        } else {
            return new UUID(0L,0L);
        }
    }

}
