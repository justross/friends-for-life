package net.arcana.friendsforlife.accessors;

import net.arcana.friendsforlife.petdatabase.PetData;

import java.util.UUID;

public interface TrackableEntityDecorator {
    PetData.FollowMode friendsforlife_getMode();
    void friendsforlife_setMode(PetData.FollowMode mode);

    boolean friendsforlife_getDowned();
    void friendsforlife_setDowned(boolean value);

    UUID friendsforlife_getUuid();
    void friendsforlife_setUuid(UUID uuid);

    /**
     * Which summon of the pet this copy is (see {@link net.arcana.friendsforlife.petdatabase.PetData#generation}).
     */
    int friendsforlife_getGeneration();
    void friendsforlife_setGeneration(int generation);
}