package net.arcana.friendsforlife.mixin;

import net.arcana.friendsforlife.Net;
import net.arcana.friendsforlife.accessors.ShoulderAccessor;
import net.arcana.friendsforlife.homeblock.HomeBlock;
import net.arcana.friendsforlife.packets.HomeUpdatePacket;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;

//? if >=1.21.10 {


/*@Mixin(ServerPlayer.class)

public abstract class ServerPlayerEntityMixin implements ShoulderAccessor {

    @Shadow
    protected abstract void setShoulderEntityLeft(CompoundTag nbt);

    @Shadow
    protected abstract void setShoulderEntityRight(CompoundTag nbt);

    public void friendsforlife_clearShoulder(boolean isLeft) {
        if (isLeft) {
            setShoulderEntityLeft(new CompoundTag());
        } else {
            setShoulderEntityRight(new CompoundTag());
        }
    }

    @Inject(method = "setRespawnPosition", at = @At("RETURN"))
    private void onSetSpawnPoint(ServerPlayer.RespawnConfig respawnConfig, boolean bl, CallbackInfo ci) {

        ServerPlayer self = (ServerPlayer) (Object) this;
        var server = self.level().getServer();
        UUID uuid = self.getUUID();

        HomeBlock home = HomeBlock.get(server);

        if (!home.hasHome(uuid) && respawnConfig.respawnData().pos() != null) {

            ResourceLocation dimId = self.level().dimension().location();

            Net.CHANNEL.serverHandle(self)
                    .send(new HomeUpdatePacket(respawnConfig.respawnData().pos(), dimId));
        }
    }

*///?} else {

@Mixin(ServerPlayer.class)
public abstract class ServerPlayerEntityMixin {
    @Inject(method = "setRespawnPosition", at = @At("RETURN"))
    private void onSetSpawnPoint(ResourceKey<Level> dimension, @Nullable BlockPos pos, float angle, boolean forced,
                                 boolean update, CallbackInfo ci) {

        ServerPlayer self = (ServerPlayer) (Object) this;
        var server = self.level().getServer();
        UUID uuid = self.getUUID();

        HomeBlock home = HomeBlock.get(server);

        if (!home.hasHome(uuid) && pos != null) {

            ResourceLocation dimId = self.level().dimension().location();

            Net.CHANNEL.serverHandle(self)
                    .send(new HomeUpdatePacket(pos, dimId));
        }
    }
 
//?}
}
