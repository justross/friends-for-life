package net.arcana.friendsforlife.mixin;

import java.util.UUID;

import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.arcana.friendsforlife.accessors.TameableEntityDecorator;
import net.arcana.friendsforlife.petdatabase.PetData;
import net.arcana.friendsforlife.petdatabase.PetTracker;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.passive.TameableEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;

@Mixin(TameableEntity.class)
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

    @Shadow
    @Nullable
    public abstract UUID getOwnerUuid();

    @Unique
    private boolean friendsforlife_forceWander = false;

    public boolean friendsforlife_isForceWander() {
        return friendsforlife_forceWander;
    }

    public void friendsforlife_setForceWander(boolean value) {
        this.friendsforlife_forceWander = value;
    }

    // disable other mobs being able to target pets
    @Inject(method = "canTarget", at = @At("HEAD"), cancellable = true)
    private void friendsforlife$disableCanBeAttackedIfPet(LivingEntity target, CallbackInfoReturnable<Boolean> cir) {
        MinecraftServer server = target.getWorld().getServer();
        if (server == null)
            return;
        PetData trackedPetData = PetTracker.get(server).get(((TameableEntity) (Object) this).getUuid());
        if (trackedPetData != null) {
            // Only prevent targeting of players
            if (target instanceof PlayerEntity) {
                cir.setReturnValue(false);
            }
        }
    }

    // Remove wandering behavior when setSetting is called
    @Inject(method = "setSitting", at = @At("HEAD"))
    private void onSetSitting(boolean sitting, CallbackInfo ci) {
        this.friendsforlife_forceWander = false;
        TameableEntity self = (TameableEntity) (Object) this;

        if (!self.getWorld().isClient()) {

            MinecraftServer server = self.getWorld().getServer();
            if (server == null)
                return;

            PetData data = PetTracker.get(server).get(this.friendsforlife_getUuid());
            if (data == null)
                return;
            ((ServerWorld) self.getWorld()).spawnParticles(
                    ParticleTypes.HEART,
                    self.getX(),
                    self.getBodyY(1),
                    self.getZ(),
                    1,
                    0.3, 0.3, 0.3,
                    0.02);
        }
    }

    @Inject(method = "writeCustomDataToNbt", at = @At("TAIL"))
    private void writeWanderStateToNbt(NbtCompound nbt, CallbackInfo ci) {
        nbt.putBoolean("FriendsForLifeForceWander", this.friendsforlife_isForceWander());
        nbt.putUuid("FriendsForLifeEntityUUID", this.friendsforlife_getUuid());
    }

    @Inject(method = "readCustomDataFromNbt", at = @At("TAIL"))
    private void readWanderStateFromNbt(NbtCompound nbt, CallbackInfo ci) {
        if (nbt.contains("FriendsForLifeForceWander")) {
            this.friendsforlife_setForceWander(nbt.getBoolean("FriendsForLifeForceWander"));
        }
        if (nbt.contains("FriendsForLifeEntityUUID")) {
            UUID entityUuid = nbt.getUuid("FriendsForLifeEntityUUID");
            if (entityUuid != null) {
                this.friendsforlife_setUuid(entityUuid);
            }
        }
    }

}
