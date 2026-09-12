package net.arcana.friendsforlife.mixin;

import net.arcana.friendsforlife.accessors.TrackableEntityDecorator;
import net.arcana.friendsforlife.petdatabase.PetData;
import net.arcana.friendsforlife.petdatabase.PetTracker;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.HorseInventoryMenu;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(HorseInventoryMenu.class)
public class HorseInventoryMenuMixin {
    @Final
    @Shadow private AbstractHorse horse;

    @Unique
    private void updateEntityData(MinecraftServer server) {
        if (server != null) {
            PetData trackedPetData = PetTracker.get(server).get(((TrackableEntityDecorator) horse).friendsforlife_getUuid());
            if (trackedPetData != null) {
                trackedPetData.updateEntityData(server, horse::saveWithoutId);
            }
        }
    }
    @Inject(method = "removed", at = @At("TAIL"))
    private void friendsforlife$syncHorseData(Player player, CallbackInfo ci) {
        updateEntityData(horse.getServer());
    }
}