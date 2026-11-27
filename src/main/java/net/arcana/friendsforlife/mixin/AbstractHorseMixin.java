package net.arcana.friendsforlife.mixin;

import net.arcana.friendsforlife.accessors.EntityAccessor;
import net.arcana.friendsforlife.accessors.TrackableEntityDecorator;
import net.arcana.friendsforlife.petdatabase.PetData;
import net.arcana.friendsforlife.petdatabase.PetTracker;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;

//? if >=1.21.10 {
/*import net.minecraft.core.UUIDUtil;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
*///?}

@Mixin(AbstractHorse.class)
public abstract class AbstractHorseMixin implements TrackableEntityDecorator {
    @Unique
    private final String FOLLOW_MODE = "FriendsForLifeFollowMode";
    /**
     * The uuid for the associated PetData
     *
     * @see PetData
     */
    @Unique
    private final String PET_UUID = "FriendsForLifeEntityUUID";

    @Unique
    private final String IS_DOWNED = "FriendsForLifeIsDowned";

    @Unique
    private boolean friendsforlife_downed = false;

    @Unique
    @Override
    public boolean friendsforlife_getDowned() {
        return friendsforlife_downed;
    }

    @Unique
    @Override
    public void friendsforlife_setDowned(boolean value) {
        friendsforlife_downed = value;
    }

    @Unique
    private UUID friendsforlife_uuid = UUID.randomUUID();

    public UUID friendsforlife_getUuid() {
        return this.friendsforlife_uuid;
    }

    public void friendsforlife_setUuid(UUID uuid) {
        this.friendsforlife_uuid = uuid;
    }

    @Unique
    private int friendsforlife_generation = 0;

    @Override
    public int friendsforlife_getGeneration() {
        return friendsforlife_generation;
    }

    @Override
    public void friendsforlife_setGeneration(int generation) {
        this.friendsforlife_generation = generation;
    }

    @Unique
    @Nullable
    private PetData.FollowMode friendsforlife_mode;

    @Override
    public PetData.FollowMode friendsforlife_getMode() {
        return friendsforlife_mode;
    }

    @Override
    public void friendsforlife_setMode(PetData.FollowMode mode) {
        this.friendsforlife_mode = mode;
    }

    @Unique
    private void updateEntityData() {
        AbstractHorse self = ((AbstractHorse) (Object) this);
        MinecraftServer server = ((EntityAccessor) self).friendsforlife_getServer();
        if (server != null) {
        PetData trackedPetData = PetTracker.get(server).getByEntityId(self.getUUID());
            if (trackedPetData != null) {
                trackedPetData.updateEntityData(server, self);
            }
        }
    }


    //? if >=1.21.10 {
    /*// Saddles are an equipment slot since 1.21.5; LivingEntityMixin snapshots on SADDLE slot changes.
    @Inject(method = "equipBodyArmor", at = @At("TAIL"))
    private void equipBodyArmor(Player player, ItemStack itemStack, CallbackInfo ci) {
        updateEntityData();
    }
    *///?} else if >=1.21.1 {
    /*@Inject(method = "equipSaddle", at = @At("TAIL"))
    private void onSaddle(ItemStack itemStack, @Nullable SoundSource soundSourcy, CallbackInfo ci) {
        updateEntityData();
    }

    @Inject(method = "equipBodyArmor", at = @At("TAIL"))
    private void equipBodyArmor(Player player, ItemStack itemStack, CallbackInfo ci) {
        updateEntityData();
    }
    *///?} else {
    @Inject(method = "equipArmor", at = @At("TAIL"))
    private void equipArmor(Player player, ItemStack itemStack, CallbackInfo ci) {
        updateEntityData();
    }

    @Inject(method = "equipSaddle", at = @At("TAIL"))
    private void onSaddle(@Nullable SoundSource soundSourcy, CallbackInfo ci) {
        updateEntityData();
    }
    //?}

//    @Inject(method = "containerChanged", at = @At("TAIL"))
//    private void containerChanged(Container container, CallbackInfo ci) {
//        updateEntityData();
//    }

    //? if >=1.21.10 {
    /*@Inject(method = "addAdditionalSaveData", at = @At("TAIL"))
    private void writeStateToNbt(ValueOutput output, CallbackInfo ci) {
        // UUIDUtil.CODEC writes the same int array as the pre-1.21.5 putUUID, so old saves still load
        output.store(PET_UUID, UUIDUtil.CODEC, this.friendsforlife_getUuid());
        output.putInt(PetData.GENERATION_KEY, this.friendsforlife_generation);
        output.putBoolean(IS_DOWNED, this.friendsforlife_getDowned());
    }

    @Inject(method = "readAdditionalSaveData", at = @At("TAIL"))
    private void readStateFromNbt(ValueInput input, CallbackInfo ci) {
        input.read(PET_UUID, UUIDUtil.CODEC).ifPresent(this::friendsforlife_setUuid);
        this.friendsforlife_generation = input.getIntOr(PetData.GENERATION_KEY, 0);
        input.getInt(FOLLOW_MODE).ifPresent(id -> this.friendsforlife_setMode(PetData.FollowMode.fromId(id)));
        this.friendsforlife_setDowned(input.getBooleanOr(IS_DOWNED, false));
    }
    *///?} else {
    @Inject(method = "addAdditionalSaveData", at = @At("TAIL"))
    private void writeStateToNbt(CompoundTag nbt, CallbackInfo ci) {
        nbt.putUUID(PET_UUID, this.friendsforlife_getUuid());
        nbt.putInt(PetData.GENERATION_KEY, this.friendsforlife_generation);
        nbt.putBoolean(IS_DOWNED, this.friendsforlife_getDowned());
    }

    @Inject(method = "readAdditionalSaveData", at = @At("TAIL"))
    private void readStateFromNbt(CompoundTag nbt, CallbackInfo ci) {
        if (nbt.contains(PET_UUID)) {
            UUID entityUuid = nbt.getUUID(PET_UUID);
            this.friendsforlife_setUuid(entityUuid);
        }
        this.friendsforlife_generation = nbt.getInt(PetData.GENERATION_KEY); // 0 if missing
        if (nbt.contains(FOLLOW_MODE)) {
            PetData.FollowMode mode = PetData.FollowMode.fromId(nbt.getInt(FOLLOW_MODE));
            this.friendsforlife_setMode(mode);
        }
        if (nbt.contains(IS_DOWNED)) {
            boolean isDowned = nbt.getBoolean(IS_DOWNED);
            this.friendsforlife_setDowned(isDowned);
        }
    }
    //?}

}
