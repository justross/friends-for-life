package net.arcana.friendsforlife.packets;

import java.util.UUID;

/** Request petdata for uuid */
public record PetDataRequestPacket(UUID petUuid) {

}
