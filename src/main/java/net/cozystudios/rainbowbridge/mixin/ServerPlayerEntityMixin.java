package net.cozystudios.rainbowbridge.mixin;

import java.util.UUID;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.cozystudios.rainbowbridge.RainbowBridgeNet;
import net.cozystudios.rainbowbridge.homeblock.HomeBlock;
import net.cozystudios.rainbowbridge.packets.HomeUpdatePacket;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

@Mixin(ServerPlayerEntity.class)
public abstract class ServerPlayerEntityMixin {

    @Inject(method = "setSpawnPoint", at = @At("RETURN"))
    private void onSetSpawnPoint(RegistryKey<World> dimension, BlockPos pos, float angle, boolean forced,
            boolean update, CallbackInfo ci) {

        ServerPlayerEntity self = (ServerPlayerEntity) (Object) this;
        UUID uuid = self.getUuid();

        HomeBlock home = HomeBlock.get(self.server);

        if (!home.hasHome(uuid)) {

            Identifier dimId = self.getWorld().getRegistryKey().getValue();

            RainbowBridgeNet.CHANNEL.serverHandle(self)
                    .send(new HomeUpdatePacket(pos, dimId));
        }
    }
}
