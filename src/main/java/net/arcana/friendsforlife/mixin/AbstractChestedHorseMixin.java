package net.arcana.friendsforlife.mixin;

import net.arcana.friendsforlife.accessors.EntityAccessor;
import net.arcana.friendsforlife.petdatabase.PetData;
import net.arcana.friendsforlife.petdatabase.PetTracker;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.animal.horse.AbstractChestedHorse;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractChestedHorse.class)
public abstract class AbstractChestedHorseMixin {

    @Inject(method = "setChest", at = @At("TAIL"))
    private void setChest(boolean bl, CallbackInfo ci) {
        AbstractHorse self = ((AbstractHorse) (Object) this);
        MinecraftServer server = ((EntityAccessor) self).friendsforlife_getServer();
        if (server != null) {
            PetData trackedPetData = PetTracker.get(server).getByEntityId(self.getUUID());
            if (trackedPetData != null) {
                trackedPetData.updateEntityData(server, self::saveWithoutId);
            }
        }
    }

}
