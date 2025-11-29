package net.arcana.friendsforlife;

import io.wispforest.owo.network.OwoNetChannel;
import net.arcana.friendsforlife.homeblock.HomeBlock;
import net.arcana.friendsforlife.homeblock.HomeBlock.HomeBlockHandle;
import net.arcana.friendsforlife.packets.DefaultSetHomeRequestPacket;
import net.arcana.friendsforlife.packets.HomeRequestPacket;
import net.arcana.friendsforlife.packets.HomeSetRequestPacket;
import net.arcana.friendsforlife.packets.HomeUpdatePacket;
import net.arcana.friendsforlife.packets.OcarinaUpdatePacket;
import net.arcana.friendsforlife.packets.PetDataRequestPacket;
import net.arcana.friendsforlife.packets.RespawnUpdatePacket;
import net.arcana.friendsforlife.petdatabase.PetData;
import net.arcana.friendsforlife.petdatabase.PetTracker;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

public class Net {
    public static final OwoNetChannel CHANNEL = OwoNetChannel.create(
            new ResourceLocation("friendsforlife", "main"));

    public static void register() {
        Net.CHANNEL.registerServerbound(HomeRequestPacket.class, (packet, handler) -> {
            ServerPlayer player = handler.player();

            // Get the persistent HomeBlock instance for this world
            HomeBlock homes = HomeBlock.get(player.getServer());
            HomeBlockHandle hbh = homes.getHome(player.getServer(), player.getUUID());

            if (hbh != null) {
                // Send it back to the client
                Net.CHANNEL.serverHandle(player).send(
                        new HomeUpdatePacket(hbh.pos(), hbh.dim().location()));
            }
        });

        // Set individual pet home block
        Net.CHANNEL.registerServerbound(HomeSetRequestPacket.class, (packet, handler) -> {
            PetData pd = PetTracker.get(handler.player().getServer()).get(packet.uuid());
            if (pd != null) {
                pd.homePosition = packet.pos();
                pd.homeDimension = ResourceKey.create(Registries.DIMENSION, packet.dimId());
            }
        });

        // Set default home block
        Net.CHANNEL.registerServerbound(DefaultSetHomeRequestPacket.class, (packet, handler) -> {
            HomeBlock.get(handler.player().getServer()).setHome(handler.player(), packet.pos(),
                    ResourceKey.create(Registries.DIMENSION, packet.dimId()));
        });

        /** Update client with new home position if respawn point changes and home is not set */
        Net.CHANNEL.registerServerbound(RespawnUpdatePacket.class, (packet, handler) -> {
            if (!HomeBlock.get(handler.player().getServer()).hasHome(handler.player().getUUID())) {

                Net.CHANNEL.serverHandle(handler.player()).send(new HomeUpdatePacket(packet.pos(), packet.dimId()));
            }
        });

        Net.CHANNEL.registerClientboundDeferred(HomeUpdatePacket.class);
        Net.CHANNEL.registerClientboundDeferred(OcarinaUpdatePacket.class);
    }
}