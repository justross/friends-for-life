package net.arcana.friendsforlife.client;

import net.arcana.friendsforlife.Net;
import net.arcana.friendsforlife.client.homeblock.ClientHomeBlock;
import net.arcana.friendsforlife.client.petdatabase.ClientOcarinaRegistry;
import net.arcana.friendsforlife.client.petdatabase.ClientPetList;
import net.arcana.friendsforlife.packets.OcarinaListRequestPacket;
import net.arcana.friendsforlife.packets.PetListRequestPacket;

public class ClientEventHandler {

    public static void handleClientJoin() {
        Net.CHANNEL.clientHandle().send(new PetListRequestPacket());
        ClientHomeBlock.initialize();
        ClientOcarinaRegistry.initialize();
        Net.CHANNEL.clientHandle().send(new OcarinaListRequestPacket());
    }

    public static void handleClientDisconnect() {
        ClientHomeBlock.reset();
        ClientPetList.reset();
        ClientOcarinaRegistry.clear();
    }
}
