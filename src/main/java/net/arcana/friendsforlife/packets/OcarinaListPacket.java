package net.arcana.friendsforlife.packets;

import java.util.List;
import java.util.UUID;

public record OcarinaListPacket(List<Entry> ocarinaToPet) {
    public record Entry(UUID uuid, UUID petUuid) {}
}
