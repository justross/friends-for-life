package net.arcana.friendsforlife.mixin;

import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.arcana.friendsforlife.accessors.TameableEntityDecorator;
import net.arcana.friendsforlife.homeblock.HomeBlock;
import net.arcana.friendsforlife.homeblock.HomeBlock.HomeBlockHandle;
import net.arcana.friendsforlife.petdatabase.PetData;
import net.arcana.friendsforlife.petdatabase.PetTracker;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.phys.Vec3;

@Mixin(RandomStrollGoal.class)
public abstract class WanderAroundGoalMixin {
    @Shadow
    @Final
    protected PathfinderMob mob;

    @Inject(method = "getPosition", at = @At("RETURN"), cancellable = true)
    private void clampWanderRadius(CallbackInfoReturnable<Vec3> cir) {
        Vec3 target = cir.getReturnValue();
        if (target == null)
            return;

        // Clamp to radius of homeblock if forceWander is true
        if (this.mob instanceof TamableAnimal pet) {
            if (pet instanceof TameableEntityDecorator
                    && ((TameableEntityDecorator) pet).friendsforlife_isForceWander()) {

                HomeBlockHandle home = null;
                PetTracker tracker = PetTracker.get(pet.getServer());
                PetData pd = tracker.getByEntityId(pet.getUUID());

                // Check if pet has its own home, otherwise use default home
                HomeBlockHandle petHbh = pd.getHomeBlock();
                if (petHbh != null && petHbh.pos() != null) {
                    home = petHbh;
                } else {
                    HomeBlock homes = HomeBlock.get((MinecraftServer) pet.getServer());
                    home = homes.getHome(pet.getServer(), (pet.getOwnerUUID()));
                }

                if (home != null) {
                    Vec3 homePos = Vec3.atCenterOf(home.pos());
                    double wanderRadius = 10; // Distance from home block pet can wander

                    if (target.distanceTo(homePos) > wanderRadius) {
                        Vec3 direction = target.subtract(homePos).normalize();
                        target = homePos.add(direction.scale(wanderRadius));
                        cir.setReturnValue(target);
                    }
                }
            }
        }
    }
}