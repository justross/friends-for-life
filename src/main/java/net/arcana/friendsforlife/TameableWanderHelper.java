package net.arcana.friendsforlife;

import java.util.ArrayList;

import net.arcana.friendsforlife.accessors.MobEntityAccessor;
import net.arcana.friendsforlife.accessors.TameableEntityDecorator;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.ai.goal.WanderAroundGoal;
import net.minecraft.entity.passive.TameableEntity;

public class TameableWanderHelper {

    /**
     * Makes a tameable entity wander around temporarily as if untamed.
     * Does not permanently change tamed status.
     */
    public static void makeTameableWander(TameableEntity entity) {
        if (entity == null || entity.getWorld().isClient)
            return;

        entity.setSitting(false);
        entity.getNavigation().stop();

        try {
            MobEntityAccessor accessor = (MobEntityAccessor) entity;
            // Enable wandering
            if (accessor instanceof TameableEntityDecorator wanderable) {
                wanderable.friendsforlife_setForceWander(true);
            }
            accessor.friendsforlife_getGoalSelector().add(5, new WanderAroundGoal(entity, 1.0, 20));

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void stopWandering(TameableEntity entity) {
        if (entity instanceof TameableEntityDecorator wanderable) {
            wanderable.friendsforlife_setForceWander(false);

            try {
                MobEntityAccessor accessor = (MobEntityAccessor) entity;

                var goalsToRemove = new ArrayList<Goal>();

                for (var prioritizedGoal : accessor.friendsforlife_getGoalSelector().getGoals()) {
                    Goal goal = prioritizedGoal.getGoal();
                    if (goal instanceof WanderAroundGoal) {
                        goalsToRemove.add(goal);

                        if (prioritizedGoal.isRunning()) {
                            prioritizedGoal.stop();
                        }
                    }
                }

                for (var goal : goalsToRemove) {
                    accessor.friendsforlife_getGoalSelector().remove(goal);
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

}