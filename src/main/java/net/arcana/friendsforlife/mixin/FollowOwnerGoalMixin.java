package net.arcana.friendsforlife.mixin;

import net.arcana.friendsforlife.accessors.TameableEntityDecorator;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.goal.FollowOwnerGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(FollowOwnerGoal.class)
 abstract class FollowOwnerGoalMixin extends Goal {

    @Final
    @Shadow
    private TamableAnimal tamable;

    @Inject(method = "canUse", at = @At("HEAD"), cancellable = true)
    private void onCanStart(CallbackInfoReturnable<Boolean> cir) {
        if (tamable instanceof TameableEntityDecorator td && td.friendsforlife_isForceWander()) {
            cir.setReturnValue(false); // Skip following while wandering
        }
    }
}