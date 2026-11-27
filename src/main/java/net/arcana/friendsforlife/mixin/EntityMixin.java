package net.arcana.friendsforlife.mixin;

import net.arcana.friendsforlife.accessors.EntityAccessor;
import net.arcana.friendsforlife.accessors.TrackableEntityDecorator;
import net.arcana.friendsforlife.petdatabase.PetData;
import net.arcana.friendsforlife.petdatabase.PetTracker;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
//? if >=1.21.10 {
/*import net.minecraft.world.level.storage.ValueOutput;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
*///?} else {
import net.minecraft.nbt.CompoundTag;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
//?}

@Mixin(Entity.class)
public abstract class EntityMixin implements EntityAccessor {
    // Entity#getServer was removed in 1.21.9; Level#getServer exists on every supported version.
    @Override
    public MinecraftServer friendsforlife_getServer() {
        Entity self = (Entity) (Object) this;
        return self.level().getServer();
    }

    /**
     * Whether this is a collared pet in Companion mode, which the mod makes invulnerable through vanilla's Invulnerable
     * flag (TrackableModeHelper). The mod sets the flag again whenever the pet loads (EventHandler#handleEntityLoad),
     * so it doesn't need saving.
     */
    @Unique
    private boolean friendsforlife$isInvulnerableFromCollar() {
        Entity self = (Entity) (Object) this;
        if (!(self instanceof TrackableEntityDecorator pet) || pet.friendsforlife_getMode() != PetData.FollowMode.COMPANION) {
            return false;
        }
        MinecraftServer server = self.level().getServer();
        return server != null && PetTracker.get(server).get(pet.friendsforlife_getUuid()) != null;
    }

    // Save the collar's invulnerability as false, so it doesn't outlive the mod: with the mod removed, the pet loads
    // as an ordinary animal. While the game runs the flag is still set, for vanilla and other mods to see.
    //? if >=1.21.10 {
    /*@Inject(method = "saveWithoutId", at = @At("TAIL"))
    private void friendsforlife$dontSaveCollarInvulnerability(ValueOutput output, CallbackInfo ci) {
        if (friendsforlife$isInvulnerableFromCollar()) {
            output.putBoolean("Invulnerable", false);
        }
    }
    *///?} else {
    @Inject(method = "saveWithoutId", at = @At("TAIL"))
    private void friendsforlife$dontSaveCollarInvulnerability(CompoundTag tag, CallbackInfoReturnable<CompoundTag> cir) {
        if (friendsforlife$isInvulnerableFromCollar()) {
            tag.putBoolean("Invulnerable", false);
        }
    }
    //?}
}
