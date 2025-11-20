package net.arcana.friendsforlife;

import net.minecraft.util.Identifier;

public class Packets {
    public static final Identifier REQUEST_PET_TRACKER = new Identifier("friendsforlife", "request_pet_tracker");
    public static final Identifier RESPONSE_PET_TRACKER = new Identifier("friendsforlife", "response_pet_tracker");
    public static final Identifier RESPONSE_PET_UPDATE = new Identifier("friendsforlife", "response_pet_update");
    public static final Identifier REQUEST_PET_TELEPORT = new Identifier("friendsforlife", "request_pet_summon");
    public static final Identifier RESPONSE_PET_TELEPORT = new Identifier("friendsforlife", "response_pet_summon");
    public static final Identifier REQUEST_OCARINA_REGISTRY = new Identifier("friendsforlife", "ocarina_registry");
}