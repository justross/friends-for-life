package net.arcana.friendsforlife.packets;

import net.minecraft.core.BlockPos;

import java.util.List;
import java.util.UUID;


public record PetUpdatePacket(List<Entry> pets) {
    /**
     * @param uuid
     * @param customName - The JSON string customName that is stored in the CustomName field in NBT
     * @param pos
     */
    public static record Entry(UUID uuid, String customName, BlockPos pos) {
    }
}

