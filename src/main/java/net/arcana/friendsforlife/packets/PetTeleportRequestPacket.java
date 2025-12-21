package net.arcana.friendsforlife.packets;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;

import java.util.UUID;

public record PetTeleportRequestPacket(UUID uuid, BlockPos petTeleportRequestPos, ResourceLocation dim, Boolean shouldWander) {
}
