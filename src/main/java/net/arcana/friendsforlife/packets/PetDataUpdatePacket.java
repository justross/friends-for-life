package net.arcana.friendsforlife.packets;

import net.arcana.friendsforlife.petdatabase.PetData;

public record PetDataUpdatePacket(PetData.Entry pet) {
}
