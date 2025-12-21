package net.arcana.friendsforlife.packets;

import java.util.UUID;

public record PetModeUpdatePacket(UUID uuid, int mode) {
}
