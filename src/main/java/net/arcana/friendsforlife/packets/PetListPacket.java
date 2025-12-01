package net.arcana.friendsforlife.packets;


import net.arcana.friendsforlife.petdatabase.PetData;

import java.util.List;

public record PetListPacket(List<PetData.Entry> pets) {
}