package net.arcana.friendsforlife.packets;

import java.util.List;
import java.util.UUID;

import net.minecraft.util.math.BlockPos;


public record PetUpdatePacket(List<Entry> pets) {
    public static record Entry(UUID uuid, String name, BlockPos pos) {}
}

