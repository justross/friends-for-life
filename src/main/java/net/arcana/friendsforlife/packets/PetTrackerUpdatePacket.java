package net.arcana.friendsforlife.packets;

import net.minecraft.core.BlockPos;

import java.util.List;
import java.util.UUID;


/**
 * Updates all pets positions and names
 * Runs frequently
 */
public record PetTrackerUpdatePacket(List<Entry> pets) {
    /**
     * @param uuid
     * @param customName - The JSON string customName that is stored in the CustomName field in NBT
     * @param petTrackerUpdatePos
     */
    public record Entry(UUID uuid, String customName, BlockPos petTrackerUpdatePos) {
    }
}

