package net.arcana.friendsforlife.mixin;

import net.arcana.friendsforlife.accessors.TameableEntityDecorator;
import net.arcana.friendsforlife.petdatabase.PetData;
import net.arcana.friendsforlife.petdatabase.PetTracker;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Objects;
import java.util.UUID;

//? if >=1.21.10 {
/*import net.minecraft.core.UUIDUtil;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
*///?}

@Mixin(TamableAnimal.class)
public abstract class TamableAnimalMixin implements TameableEntityDecorator {
    @Unique
    private final String FOLLOW_MODE = "FriendsForLifeFollowMode";
    @Unique
    private final String FORCE_WANDER = "FriendsForLifeForceWander";
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
    private boolean friendsforlife_forceWander = false;

    public boolean friendsforlife_isForceWander() {
        return friendsforlife_forceWander;
    }

    public void friendsforlife_setForceWander(boolean value) {
        this.friendsforlife_forceWander = value;
    }

    @Inject(method = "canAttack", at = @At("HEAD"), cancellable = true)
    private void friendsforlife$disableCanBeAttackedIfCompanion(LivingEntity target, CallbackInfoReturnable<Boolean> cir) {
        MinecraftServer server = target.level().getServer();
        if (server == null)
            return;
        TamableAnimal self = ((TamableAnimal) (Object) this);
        PetData trackedPetData = PetTracker.get(server).getByEntityId(self.getUUID());
        if (trackedPetData != null && friendsforlife_getMode() != null && Objects.equals(friendsforlife_getMode(), PetData.FollowMode.COMPANION)) {
                cir.setReturnValue(false);
        }
    }

    // Remove wandering behavior when setSetting is called.
    // Method is called anytime the pet should not sit, not just when players set their pet to sit
    @Inject(method = "setOrderedToSit", at = @At("HEAD"))
    private void onSetSitting(boolean sitting, CallbackInfo ci) {
        this.friendsforlife_forceWander = false;
    }

    //? if >=1.21.10 {
    /*@Inject(method = "addAdditionalSaveData", at = @At("TAIL"))
    private void writeStateToNbt(ValueOutput output, CallbackInfo ci) {
        output.putBoolean(FORCE_WANDER, this.friendsforlife_isForceWander());
        // UUIDUtil.CODEC writes the same int array as the pre-1.21.5 putUUID, so old saves still load
        output.store(PET_UUID, UUIDUtil.CODEC, this.friendsforlife_getUuid());
        if (this.friendsforlife_mode != null) {
            output.putInt(FOLLOW_MODE, this.friendsforlife_mode.id);
        }
        output.putBoolean(IS_DOWNED, this.friendsforlife_getDowned());
    }
    *///?} else {


    @Inject(method = "addAdditionalSaveData", at = @At("TAIL"))
    private void writeStateToNbt(CompoundTag nbt, CallbackInfo ci) {
        nbt.putBoolean(FORCE_WANDER, this.friendsforlife_isForceWander());
        nbt.putUUID(PET_UUID, this.friendsforlife_getUuid());
        if (this.friendsforlife_mode != null) {
            nbt.putInt(FOLLOW_MODE, this.friendsforlife_mode.id);
        }
        nbt.putBoolean(IS_DOWNED, this.friendsforlife_getDowned());
    }
 
    //?}


    //? if >=1.21.10 {
    /*@Inject(method = "readAdditionalSaveData", at = @At("TAIL"))
    private void readStateFromNbt(ValueInput input, CallbackInfo ci) {
        this.friendsforlife_setForceWander(input.getBooleanOr(FORCE_WANDER, false));
        input.read(PET_UUID, UUIDUtil.CODEC).ifPresent(this::friendsforlife_setUuid);
        input.getInt(FOLLOW_MODE).ifPresent(id -> this.friendsforlife_setMode(PetData.FollowMode.fromId(id)));
        this.friendsforlife_setDowned(input.getBooleanOr(IS_DOWNED, false));
    }
    *///?} else {
   

    @Inject(method = "readAdditionalSaveData", at = @At("TAIL"))
    private void readStateFromNbt(CompoundTag nbt, CallbackInfo ci) {
        if (nbt.contains(FORCE_WANDER)) {
            this.friendsforlife_setForceWander(nbt.getBoolean(FORCE_WANDER));
        }
        if (nbt.contains(PET_UUID)) {
            UUID entityUuid = nbt.getUUID(PET_UUID);
            this.friendsforlife_setUuid(entityUuid);
        }
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
