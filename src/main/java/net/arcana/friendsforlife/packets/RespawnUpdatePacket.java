package net.arcana.friendsforlife.packets;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;

/** Packet for updating the server whenever the player sets a spawn point */
public record RespawnUpdatePacket(BlockPos pos, ResourceLocation dimId) {

}