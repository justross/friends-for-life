package net.arcana.friendsforlife.mixin;

import net.arcana.friendsforlife.TameableWanderHelper;
import net.arcana.friendsforlife.accessors.TameableEntityDecorator;
import net.arcana.friendsforlife.accessors.TrackableEntityDecorator;
import net.arcana.friendsforlife.homeblock.HomeBlock;
import net.arcana.friendsforlife.petdatabase.PetData;
import net.arcana.friendsforlife.petdatabase.PetTracker;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Objects;

//? if >=1.21.10 {
/*import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
*///?}

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {
    @Unique
    protected int downedTicks = 0;

    @Shadow
    protected abstract SoundEvent getDeathSound();

    @Shadow
    protected abstract float getSoundVolume();

    @Unique
    private void setDowned() {
        LivingEntity self = (LivingEntity) (Object) this;
        if (!(self instanceof TrackableEntityDecorator pet)) {
            return;
        }
        OwnableEntity oldTame = (OwnableEntity) self;

        self.setHealth(self.getMaxHealth());
        self.removeAllEffects();
        pet.friendsforlife_setDowned(true);

        ((Player) Objects.requireNonNull(oldTame.getOwner())).displayClientMessage(Component.translatable("message.friendsforlife.pet_retreated", self.getName()), true);

        // Teleport pet home if downed
        Objects.requireNonNull(self.level().getServer()).execute(() -> {
            PetData pd = PetTracker.get(self.level().getServer()).get(pet.friendsforlife_getUuid());
            assert pd != null;

            HomeBlock hb = HomeBlock.get(self.level().getServer());
            assert hb != null;

            // Determine home block
            var home = hb.getHome(self.level().getServer(), pd.ownerUuid);
            assert home != null;

            var homePos = pd.homePosition;
            var homeDim = pd.homeDimension;
            if (homePos == null || homeDim == null) {
                homePos = home.pos();
                homeDim = home.dim();
            }

            pet.friendsforlife_setDowned(true);
            pd.setDowned(self.level().getServer());


            var livingEntity = pd.recreateEntity(self.level().getServer(), homeDim, homePos.getX(), homePos.getY(), homePos.getZ(), self);
            if (livingEntity instanceof TamableAnimal tame) {
                TameableWanderHelper.makeTameableWander(tame);
            }
            SoundEvent deathSound = getDeathSound();
            if (deathSound != null) {
                self.playSound(deathSound, getSoundVolume(), self.getVoicePitch());
            }
            ((PathfinderMob) self).getNavigation().stop();
            if (self.isSleeping()) {
                self.stopSleeping();
            }
            self.setDeltaMovement(0, 0, 0);
        });
    }

    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void friendsforlife$downedTick(CallbackInfo ci) {

        LivingEntity self = (LivingEntity) (Object) this;
        if (self instanceof OwnableEntity) {
            TrackableEntityDecorator pet = (self instanceof TrackableEntityDecorator ted) ? ted : null;
            if (pet != null
                    && pet.friendsforlife_getMode() == PetData.FollowMode.GUARDIAN
                    && pet.friendsforlife_getDowned()
            ) {
                ci.cancel(); // skip normal tick, so AI doesn't run
            }
        }
    }

    //? if >=1.21.10 {
    /*@Inject(method = "hurtServer", at = @At("HEAD"), cancellable = true)
    private void friendsforlife$preventDeath(ServerLevel level, DamageSource source, float f, CallbackInfoReturnable<Boolean> cir) {
        if (friendsforlife$tryPreventDeath(source, f)) {
            cir.setReturnValue(false);
        }
    }

    // Saddles are an equipment slot since 1.21.5 (AbstractHorse#equipSaddle is gone), so snapshot here.
    // tickCount > 0 skips equipment set while the entity is still loading.
    @Inject(method = "setItemSlot", at = @At("TAIL"))
    private void friendsforlife$onSaddleChanged(EquipmentSlot slot, ItemStack stack, CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (slot != EquipmentSlot.SADDLE || !(self instanceof AbstractHorse) || self.tickCount == 0) {
            return;
        }
        MinecraftServer server = self.level().getServer();
        if (server == null) {
            return;
        }
        PetData pd = PetTracker.get(server).getByEntityId(self.getUUID());
        if (pd != null) {
            pd.updateEntityData(server, self);
        }
    }
    *///?} else {
    @Inject(method = "hurt", at = @At("HEAD"), cancellable = true)
    private void friendsforlife$preventDeath(DamageSource source, float f, CallbackInfoReturnable<Boolean> cir) {
        if (friendsforlife$tryPreventDeath(source, f)) {
            cir.setReturnValue(false);
        }
    }
    //?}

    /**
     * Downs a guardian pet instead of letting it die. Returns true if the damage should be cancelled.
     */
    @Unique
    private boolean friendsforlife$tryPreventDeath(DamageSource source, float f) {
        // Only apply to tameable pets
        LivingEntity self = (LivingEntity) (Object) this;
        if (self instanceof TamableAnimal) {
            TameableEntityDecorator pet = (self instanceof TameableEntityDecorator ted) ? ted : null;

            if (pet != null && Objects.equals(pet.friendsforlife_getMode(), PetData.FollowMode.GUARDIAN) && !source.isCreativePlayer() && self.getHealth() - f <= 0) {
                setDowned();
                return true;
            }
        } else if (self instanceof AbstractHorse) {
            TrackableEntityDecorator pet = (self instanceof TrackableEntityDecorator ted) ? ted : null;

            if (pet != null && Objects.equals(pet.friendsforlife_getMode(), PetData.FollowMode.GUARDIAN) && !source.isCreativePlayer() && self.getHealth() - f <= 0) {
                setDowned();
                return true;
            }
        }
        return false;
    }
}
