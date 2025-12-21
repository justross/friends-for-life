package net.arcana.friendsforlife.packets;

import java.util.UUID;

public record PetDownedUpdatePacket(UUID uuid, boolean isDowned) {
}
