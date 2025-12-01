package net.arcana.friendsforlife.mixin;

import net.arcana.friendsforlife.accessors.ShoulderAccessor;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.UUID;

@Mixin(Player.class)
public abstract class PlayerEntityMixin implements ShoulderAccessor {

    @Shadow public abstract CompoundTag getShoulderEntityLeft();
    @Shadow public abstract CompoundTag getShoulderEntityRight();
    @Shadow protected abstract void setShoulderEntityLeft(CompoundTag nbt);
    @Shadow protected abstract void setShoulderEntityRight(CompoundTag nbt);

    @Override
    public void friendsforlife_clearShoulder(UUID petUUID) {
        if (!getShoulderEntityLeft().isEmpty() && getShoulderEntityLeft().hasUUID("UUID") &&
            getShoulderEntityLeft().getUUID("UUID").equals(petUUID)) {
            setShoulderEntityLeft(new CompoundTag());
        }
        if (!getShoulderEntityRight().isEmpty() && getShoulderEntityRight().hasUUID("UUID") &&
            getShoulderEntityRight().getUUID("UUID").equals(petUUID)) {
            setShoulderEntityRight(new CompoundTag());
        }
    }
}