package net.arcana.friendsforlife.mixin;

import java.util.UUID;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.arcana.friendsforlife.Net;
import net.arcana.friendsforlife.homeblock.HomeBlock;
import net.arcana.friendsforlife.packets.HomeUpdatePacket;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

@Mixin(ServerPlayer.class)
public abstract class ServerPlayerEntityMixin {

    @Inject(method = "setRespawnPosition", at = @At("RETURN"))
    private void onSetSpawnPoint(ResourceKey<Level> dimension, BlockPos pos, float angle, boolean forced,
            boolean update, CallbackInfo ci) {

        ServerPlayer self = (ServerPlayer) (Object) this;
        UUID uuid = self.getUUID();

        HomeBlock home = HomeBlock.get(self.server);

        if (!home.hasHome(uuid)) {

            ResourceLocation dimId = self.level().dimension().location();

            Net.CHANNEL.serverHandle(self)
                    .send(new HomeUpdatePacket(pos, dimId));
        }
    }
}
