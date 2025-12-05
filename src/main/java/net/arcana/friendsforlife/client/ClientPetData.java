package net.arcana.friendsforlife.client;

import net.arcana.friendsforlife.petdatabase.PetData;
import net.arcana.friendsforlife.util.CompatComponent;
import net.arcana.friendsforlife.util.CompatResourceLocation;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.UUID;

public class ClientPetData {
    public final UUID uuid;
    public String position;
    public CompoundTag entityData;
    public final EntityType<?> type;
    @Nullable
    public BlockPos homePosition;
    @Nullable
    public ResourceLocation homeDimension;

    public ClientPetData(UUID uuid, String entityType, CompoundTag entityData, String position,
                         @Nullable BlockPos homePosition, @Nullable ResourceLocation homeDimension) {
        this.uuid = uuid;
        this.entityData = entityData;
        this.position = position;
        this.homePosition = homePosition;
        this.homeDimension = homeDimension;
        this.type = BuiltInRegistries.ENTITY_TYPE.get(new CompatResourceLocation(entityType).unwrap());

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

        return new ClientPetData(uuid, entityTypeId, nbt, position, homePosition,
                homeDimension);
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
            return new UUID(0L, 0L);
        }
    }

    public String getName() {
        Objects.requireNonNull(entityData);
        if (entityData.contains("CustomName")) {
            return Objects.requireNonNull(CompatComponent.fromJson(entityData.getString("CustomName"))).getString();
        }

        if (entityData.contains("EntityType")) {
            String typeKey = entityData.getString("EntityType");
            ResourceLocation id = new CompatResourceLocation(typeKey).unwrap();

            EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.get(id);
            return I18n.get(type.getDescriptionId());
        }

        return "Unknown Pet";
    }


}
