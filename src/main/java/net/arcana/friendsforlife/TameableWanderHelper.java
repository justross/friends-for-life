package net.arcana.friendsforlife;

import net.arcana.friendsforlife.accessors.MobEntityAccessor;
import net.arcana.friendsforlife.accessors.TameableEntityDecorator;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;

import java.util.ArrayList;

public class TameableWanderHelper {

    /**
     * Makes a tameable entity wander around temporarily as if untamed.
     * Does not permanently change tamed status.
     */
    public static void makeTameableWander(TamableAnimal entity) {
        if (entity == null || entity.level().isClientSide())
            return;

        entity.setOrderedToSit(false);
        entity.getNavigation().stop();

        try {
            MobEntityAccessor accessor = (MobEntityAccessor) entity;
            // Enable wandering
            if (accessor instanceof TameableEntityDecorator wanderable) {
                wanderable.friendsforlife_setForceWander(true);
            }
            accessor.friendsforlife_getGoalSelector().addGoal(5, new RandomStrollGoal(entity, 1.0, 20));

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void stopWandering(Entity entity) {
        if (entity instanceof TameableEntityDecorator wanderable) {
            wanderable.friendsforlife_setForceWander(false);

            try {
                MobEntityAccessor accessor = (MobEntityAccessor) entity;

                var goalsToRemove = new ArrayList<Goal>();

                for (var prioritizedGoal : accessor.friendsforlife_getGoalSelector().getAvailableGoals()) {
                    Goal goal = prioritizedGoal.getGoal();
                    if (goal instanceof RandomStrollGoal) {
                        goalsToRemove.add(goal);

                        if (prioritizedGoal.isRunning()) {
                            prioritizedGoal.stop();
                        }
                    }
                }

                for (var goal : goalsToRemove) {
                    accessor.friendsforlife_getGoalSelector().removeGoal(goal);
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

}