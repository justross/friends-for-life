package net.arcana.friendsforlife.packets;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;

public record HomeUpdatePacket(BlockPos homeUpdatePos, ResourceLocation dimId) {

}
