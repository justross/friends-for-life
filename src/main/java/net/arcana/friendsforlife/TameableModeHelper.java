package net.arcana.friendsforlife;

import net.arcana.friendsforlife.accessors.MobEntityAccessor;
import net.arcana.friendsforlife.accessors.TameableEntityDecorator;
import net.arcana.friendsforlife.petdatabase.PetData;
import net.minecraft.world.entity.TamableAnimal;

public class TameableModeHelper {
    public static void setTameableMode(PetData.FollowMode mode, TamableAnimal entity) {
        if (entity == null || entity.level().isClientSide)
            return;
        MobEntityAccessor accessor = (MobEntityAccessor) entity;

        if (accessor instanceof TameableEntityDecorator ted) {
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
