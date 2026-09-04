package net.arcana.friendsforlife.mixin;

import net.arcana.friendsforlife.accessors.TrackableEntityDecorator;
import net.arcana.friendsforlife.petdatabase.PetData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;

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


    @Inject(method = "addAdditionalSaveData", at = @At("TAIL"))
    private void writeStateToNbt(CompoundTag nbt, CallbackInfo ci) {
        nbt.putUUID(PET_UUID, this.friendsforlife_getUuid());
        nbt.putBoolean(IS_DOWNED, this.friendsforlife_getDowned());
    }

    @Inject(method = "readAdditionalSaveData", at = @At("TAIL"))
    private void readStateFromNbt(CompoundTag nbt, CallbackInfo ci) {
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

}
