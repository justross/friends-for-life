package net.arcana.friendsforlife.packets;

import java.util.Map;
import java.util.UUID;

public record OcarinaRequestPacket (Map<UUID, UUID> bindings) {
    
}
