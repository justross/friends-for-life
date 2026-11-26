package net.arcana.friendsforlife.mixin;

import net.arcana.friendsforlife.accessors.ShoulderAccessor;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

/**
 * Shoulder access before 1.21.9, where the shoulder setters live on Player.
 * From 1.21.9 they are declared on ServerPlayer, so ServerPlayerEntityMixin provides ShoulderAccessor instead.
 */
@Mixin(Player.class)
public abstract class PlayerEntityMixin
        //? if <1.21.10
        implements ShoulderAccessor
{
    //? if <1.21.10 {
    @Shadow
    protected abstract void setShoulderEntityLeft(CompoundTag nbt);

    @Shadow
    protected abstract void setShoulderEntityRight(CompoundTag nbt);

    @Override
    public void friendsforlife_clearShoulder(boolean isLeft) {
        if (isLeft) {
            setShoulderEntityLeft(new CompoundTag());
        } else {
            setShoulderEntityRight(new CompoundTag());
        }
    }
    //?}
}
