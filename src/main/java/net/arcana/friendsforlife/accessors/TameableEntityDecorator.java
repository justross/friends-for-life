package net.arcana.friendsforlife.accessors;

import java.util.UUID;

public interface TameableEntityDecorator {
    boolean friendsforlife_isForceWander();
    UUID friendsforlife_uuid();

    void friendsforlife_setForceWander(boolean value);
    void friendsforlife_setUuid(UUID uuid);
    UUID friendsforlife_getUuid();
}