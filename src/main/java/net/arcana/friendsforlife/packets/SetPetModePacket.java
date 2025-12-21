package net.arcana.friendsforlife.packets;

import java.util.UUID;

public record SetPetModePacket(UUID uuid, int mode) {
    
}
