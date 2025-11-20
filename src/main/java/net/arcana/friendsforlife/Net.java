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
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

public class Net {
    public static final OwoNetChannel CHANNEL = OwoNetChannel.create(
            new Identifier("friendsforlife", "main"));

    public static void register() {
        Net.CHANNEL.registerServerbound(HomeRequestPacket.class, (packet, handler) -> {
            ServerPlayerEntity player = handler.player();

            // Get the persistent HomeBlock instance for this world
            HomeBlock homes = HomeBlock.get(player.getServer());
            HomeBlockHandle hbh = homes.getHome(player.getServer(), player.getUuid());

            if (hbh != null) {
                // Send it back to the client
                Net.CHANNEL.serverHandle(player).send(
                        new HomeUpdatePacket(hbh.pos(), hbh.dim().getValue()));
            }
        });

        // Set individual pet home block
        Net.CHANNEL.registerServerbound(HomeSetRequestPacket.class, (packet, handler) -> {
            PetData pd = PetTracker.get(handler.player().getServer()).get(packet.uuid());
            if (pd != null) {
                pd.homePosition = packet.pos();
                pd.homeDimension = RegistryKey.of(RegistryKeys.WORLD, packet.dimId());
            }
        });

        // Set default home block
        Net.CHANNEL.registerServerbound(DefaultSetHomeRequestPacket.class, (packet, handler) -> {
            HomeBlock.get(handler.player().getServer()).setHome(handler.player(), packet.pos(),
                    RegistryKey.of(RegistryKeys.WORLD, packet.dimId()));
        });

        /** Update client with new home position if respawn point changes and home is not set */
        Net.CHANNEL.registerServerbound(RespawnUpdatePacket.class, (packet, handler) -> {
            if (!HomeBlock.get(handler.player().getServer()).hasHome(handler.player().getUuid())) {

                Net.CHANNEL.serverHandle(handler.player()).send(new HomeUpdatePacket(packet.pos(), packet.dimId()));
            }
        });

        Net.CHANNEL.registerClientboundDeferred(HomeUpdatePacket.class);
        Net.CHANNEL.registerClientboundDeferred(OcarinaUpdatePacket.class);
    }
}