package net.arcana.friendsforlife.packets;

import net.minecraft.nbt.CompoundTag;

import java.util.UUID;

public record EntityUpdatePacket(UUID uuid, CompoundTag entityData) {

}
