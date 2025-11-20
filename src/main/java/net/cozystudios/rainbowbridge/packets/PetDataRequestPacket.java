package net.cozystudios.rainbowbridge.packets;

import java.util.UUID;

/** Request petdata for uuid */
public record PetDataRequestPacket(UUID petUuid) {

}
