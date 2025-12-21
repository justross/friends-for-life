package net.arcana.friendsforlife.packets;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;

import java.util.UUID;

/** Packet for handling requests to set individual pets' home blocks
 * @uuid The pet's UUID
 */
public record HomeSetRequestPacket(UUID uuid, BlockPos homeSetRequestPos, ResourceLocation dimId) {

}
