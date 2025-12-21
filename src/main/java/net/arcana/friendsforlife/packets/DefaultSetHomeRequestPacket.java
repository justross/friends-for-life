package net.arcana.friendsforlife.packets;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;

/** Packet for handling the default home block */
public record DefaultSetHomeRequestPacket(BlockPos defaultSetHomeRequestPos, ResourceLocation dimId) {

}
