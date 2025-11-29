package net.arcana.friendsforlife.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.arcana.friendsforlife.ModInit;
import net.arcana.friendsforlife.accessors.MobEntityAccessor;
import net.arcana.friendsforlife.petdatabase.PetData;
import net.arcana.friendsforlife.petdatabase.PetTracker;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.GoalSelector;

@Mixin(Mob.class)
public abstract class MobEntityMixin implements MobEntityAccessor {
    @Final
    @Shadow
    protected GoalSelector goalSelector;

    @Override
    public GoalSelector friendsforlife_getGoalSelector() {
        return this.goalSelector;
    }

    @Inject(method = "canAttackType", at = @At("HEAD"), cancellable = true)
    private void stopTamedPetsAttacking(EntityType<?> type, CallbackInfoReturnable<Boolean> cir) {
        MinecraftServer server = ((Mob) (Object) this).level().getServer();
        if (server == null)
            return;

        PetData trackedPetData = PetTracker.get(server).get(((Mob) (Object) this).getUUID());
        if (trackedPetData != null) {
            cir.setReturnValue(false);
        }
    }

}