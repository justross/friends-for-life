package net.arcana.friendsforlife;

import net.arcana.friendsforlife.accessors.TameableEntityDecorator;
import net.arcana.friendsforlife.accessors.TrackableEntityDecorator;
import net.arcana.friendsforlife.items.CollarItem;
import net.arcana.friendsforlife.items.OcarinaItem;
import net.arcana.friendsforlife.petdatabase.PetData;
import net.arcana.friendsforlife.petdatabase.PetTracker;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.Objects;
import java.util.Set;
import java.util.UUID;

public class EventHandler {


    // cause dogs are annoying and return from a interaction early, we have to
    // overwrite nbt methods
    public static InteractionResult handleEntityUse(Player player, Level world, InteractionHand hand, Entity entity) {
        if (world.isClientSide) return InteractionResult.PASS;

        if (entity instanceof TamableAnimal tame) {
            if (!tame.isOwnedBy(player)) return InteractionResult.PASS;

            ItemStack stack = player.getItemInHand(hand);
            PetTracker tracker = PetTracker.get(player.getServer());
            PetData pd = tracker.get(((TameableEntityDecorator) tame).friendsforlife_getUuid());
            // Play heart particle when set to sit. if the player is removing the collar, don't 😭
            if (pd != null && !(stack.isEmpty() && player.isShiftKeyDown())) {
                ((ServerLevel) tame.level()).sendParticles(
                        ParticleTypes.HEART,
                        tame.getX(),
                        tame.getY(1),
                        tame.getZ(),
                        1,
                        0.3, 0.3, 0.3,
                        0.02);
            }
            if (stack.getItem() instanceof CollarItem collarItem && pd == null) {
                return collarItem.applyCollar(stack, (ServerPlayer) player, tame);
            } else if (stack.isEmpty() && player.isShiftKeyDown()) {
                ItemStack collar = CollarItem.getCollar(player, tame, pd);
                if (collar == null) return InteractionResult.PASS;
                if (pd == null) {
                    Init.LOGGER.error("Unable to find PetData for tracked entity " + tame.getStringUUID());
                    return InteractionResult.PASS;
                }
                CollarItem.removePet(tame, pd);

                player.addItem(collar);
                return InteractionResult.CONSUME;
            }

            if (stack.getItem() instanceof OcarinaItem ocarina && pd != null) {
                return ocarina.useOcarinaFromPet(world, player, stack, pd, tame);
            }

        } else if (entity instanceof AbstractHorse horse) {
            if (Objects.equals(horse.getOwnerUUID(), player.getUUID())) {
                ItemStack stack = player.getItemInHand(hand);
                PetTracker tracker = PetTracker.get(player.getServer());
                PetData pd = tracker.get(((TrackableEntityDecorator) horse).friendsforlife_getUuid());
                // Play heart particle when set to sit. if the player is removing the collar, don't 😭
                if (pd != null && !(stack.isEmpty() && player.isShiftKeyDown())) {
                    ((ServerLevel) horse.level()).sendParticles(
                            ParticleTypes.HEART,
                            horse.getX(),
                            horse.getY(1),
                            horse.getZ(),
                            1,
                            0.3, 0.3, 0.3,
                            0.02);
                }
                if (stack.getItem() instanceof CollarItem collarItem && pd == null) {
                    return collarItem.applyCollar(stack, (ServerPlayer) player, horse);
                } else if (stack.isEmpty() && player.isShiftKeyDown()) {
                    ItemStack collar = CollarItem.getCollar(player, horse, pd);
                    if (collar == null) return InteractionResult.PASS;
                    if (pd == null) {
                        Init.LOGGER.error("Unable to find PetData for tracked entity " + horse.getStringUUID());
                        return InteractionResult.PASS;
                    }
                    CollarItem.removePet(horse, pd);

                    player.addItem(collar);
                    return InteractionResult.CONSUME;
                }

                if (stack.getItem() instanceof OcarinaItem ocarina && pd != null) {
                    return ocarina.useOcarinaFromPet(world, player, stack, pd, horse);
                }
            }
        }
        return InteractionResult.PASS;
    }


    public static void handleEntityUnload(Entity entity, Level world) {
        if (entity instanceof TrackableEntityDecorator trackable && trackable.friendsforlife_getUuid() != null && !world.isClientSide) {
            PetTracker tracker = PetTracker.get(world.getServer());
            PetData pd = tracker.get(trackable.friendsforlife_getUuid());
            if (pd != null) {
                pd.updateEntityData(entity::saveWithoutId);
            }
        }
    }

    public static void handleEntityLoad(Entity entity, ServerLevel world) {

        if (entity instanceof OwnableEntity ownable) {
            UUID uuid = ((TrackableEntityDecorator) ownable).friendsforlife_getUuid();
            PetTracker tracker = PetTracker.get(world.getServer());
            if (uuid != null) {

                // Remove entities that have been duplicated and marked as such
                Set<UUID> recreatedSet = tracker.getRecreatedEntityMap();

                if (recreatedSet.remove(entity.getUUID())) {
                    tracker.setDirty();
                    world.getServer().execute(entity::discard); // run next tick
                }
            }
            // Set attributes for new entity in case it doesn't have one yet
            else {
                PetData pd = tracker.get(uuid);
                if (pd != null) {
                    // entityUuid may be null if PetData was deserialized
                    if (pd.entityUuid == null) {
                        pd.entityUuid = ((Entity) ownable).getUUID();
                    }
                    pd.trySetMode(pd.mode, (Entity) ownable);

                    ((TrackableEntityDecorator) ownable).friendsforlife_setDowned(pd.isDowned);
                }

            }
        }
    }
}
