package net.arcana.friendsforlife.mixin;

import net.arcana.friendsforlife.accessors.TameableEntityDecorator;
import net.arcana.friendsforlife.petdatabase.PetData;
import net.arcana.friendsforlife.petdatabase.PetTracker;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.UUID;

@Mixin(TamableAnimal.class)
public abstract class TameableEntityMixin implements TameableEntityDecorator {
    private UUID friendsforlife_uuid = UUID.randomUUID();

    @Override
    public void friendsforlife_setUuid(UUID uuid) {
        this.friendsforlife_uuid = uuid;
    }

    @Override
    public UUID friendsforlife_getUuid() {
        return this.friendsforlife_uuid;
    }

    @Unique
    private boolean friendsforlife_forceWander = false;

    public boolean friendsforlife_isForceWander() {
        return friendsforlife_forceWander;
    }

    public void friendsforlife_setForceWander(boolean value) {
        this.friendsforlife_forceWander = value;
    }

    // disable other mobs being able to target pets
    @Inject(method = "canAttack", at = @At("HEAD"), cancellable = true)
    private void friendsforlife$disableCanBeAttackedIfPet(LivingEntity target, CallbackInfoReturnable<Boolean> cir) {
        MinecraftServer server = target.level().getServer();
        if (server == null)
            return;
        PetData trackedPetData = PetTracker.get(server).get(((TamableAnimal) (Object) this).getUUID());
        if (trackedPetData != null) {
            // Only prevent targeting of players
            if (target instanceof Player) {
                cir.setReturnValue(false);
            }
        }
    }

    // Remove wandering behavior when setSetting is called
    @Inject(method = "setOrderedToSit", at = @At("HEAD"))
    private void onSetSitting(boolean sitting, CallbackInfo ci) {
        this.friendsforlife_forceWander = false;
        TamableAnimal self = (TamableAnimal) (Object) this;

        if (!self.level().isClientSide()) {

            MinecraftServer server = self.level().getServer();
            if (server == null)
                return;

            PetData data = PetTracker.get(server).get(this.friendsforlife_getUuid());
            if (data == null)
                return;
            ((ServerLevel) self.level()).sendParticles(
                    ParticleTypes.HEART,
                    self.getX(),
                    self.getY(1),
                    self.getZ(),
                    1,
                    0.3, 0.3, 0.3,
                    0.02);
        }
    }

    @Inject(method = "addAdditionalSaveData", at = @At("TAIL"))
    private void writeWanderStateToNbt(CompoundTag nbt, CallbackInfo ci) {
        nbt.putBoolean("FriendsForLifeForceWander", this.friendsforlife_isForceWander());
        nbt.putUUID("FriendsForLifeEntityUUID", this.friendsforlife_getUuid());
    }

    @Inject(method = "readAdditionalSaveData", at = @At("TAIL"))
    private void readWanderStateFromNbt(CompoundTag nbt, CallbackInfo ci) {
        if (nbt.contains("FriendsForLifeForceWander")) {
            this.friendsforlife_setForceWander(nbt.getBoolean("FriendsForLifeForceWander"));
        }
        if (nbt.contains("FriendsForLifeEntityUUID")) {
            UUID entityUuid = nbt.getUUID("FriendsForLifeEntityUUID");
            if (entityUuid != null) {
                this.friendsforlife_setUuid(entityUuid);
            }
        }
    }

}
