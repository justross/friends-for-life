package net.arcana.friendsforlife;

import net.arcana.friendsforlife.accessors.MobEntityAccessor;
import net.arcana.friendsforlife.accessors.TrackableEntityDecorator;
import net.arcana.friendsforlife.petdatabase.PetData;
import net.minecraft.world.entity.Entity;

public class TrackableModeHelper {
    public static void setTrackableMode(PetData.FollowMode mode, Entity entity) {
        if (entity == null || entity.level().isClientSide())
            return;
        MobEntityAccessor accessor = (MobEntityAccessor) entity;

        if (accessor instanceof TrackableEntityDecorator ted) {
            ted.friendsforlife_setMode(mode);
        }

        switch (mode) {
            case GUARDIAN -> {
                entity.setInvulnerable(false);
            }
            case COMPANION -> {
                entity.setInvulnerable(true);
            }
        }
    }
}
