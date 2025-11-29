package net.arcana.friendsforlife.packets;

import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;

/** Packet for handling requests to set individual pets' home blocks
 * @uuid The pet's UUID
 */
public record HomeSetRequestPacket(UUID uuid, BlockPos pos, ResourceLocation dimId) {

}
