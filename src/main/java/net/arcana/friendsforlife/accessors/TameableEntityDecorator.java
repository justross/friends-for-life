package net.arcana.friendsforlife.accessors;

import net.arcana.friendsforlife.petdatabase.PetData;

import java.util.UUID;

public interface TameableEntityDecorator {
    boolean friendsforlife_isForceWander();
    void friendsforlife_setForceWander(boolean value);

    PetData.FollowMode friendsforlife_getMode();
    void friendsforlife_setMode(PetData.FollowMode mode);

    boolean friendsforlife_getDowned();
    void friendsforlife_setDowned(boolean value);

    UUID friendsforlife_getUuid();
    void friendsforlife_setUuid(UUID uuid);
}