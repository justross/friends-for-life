package net.arcana.friendsforlife;

import net.arcana.friendsforlife.accessors.TameableEntityDecorator;
import net.arcana.friendsforlife.accessors.TrackableEntityDecorator;
import net.arcana.friendsforlife.items.CollarItem;
import net.arcana.friendsforlife.items.OcarinaItem;
import net.arcana.friendsforlife.petdatabase.PetData;
import net.arcana.friendsforlife.petdatabase.PetTracker;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.core.registries.BuiltInRegistries;
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
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.Set;
import java.util.UUID;

public class EventHandler {


    // cause dogs are annoying and return from a interaction early, we have to
    // overwrite nbt methods
    public static InteractionResult handleEntityUse(Player player, Level world, InteractionHand hand, Entity entity) {
        if (world.isClientSide()) return InteractionResult.PASS;

        if (entity instanceof TamableAnimal tame) {
            if (!tame.isOwnedBy(player)) return InteractionResult.PASS;

            ItemStack stack = player.getItemInHand(hand);
            PetTracker tracker = PetTracker.get(player.level().getServer());
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
            //? if >=1.21.10 {
            /*UUID horseOwner = horse.getOwnerReference() != null ? horse.getOwnerReference().getUUID() : null;
            *///?} else {
            UUID horseOwner = horse.getOwnerUUID();
            //?}
            if (Objects.equals(horseOwner, player.getUUID())) {
                ItemStack stack = player.getItemInHand(hand);
                PetTracker tracker = PetTracker.get(player.level().getServer());
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
        if (entity instanceof TrackableEntityDecorator trackable && trackable.friendsforlife_getUuid() != null && !world.isClientSide()) {
            PetTracker tracker = PetTracker.get(world.getServer());
            PetData pd = tracker.get(trackable.friendsforlife_getUuid());
            if (pd != null) {
                pd.updateEntityData(world.getServer(), entity);
            }
        }
    }

    /**
     * @param canCancel whether the caller can stop the entity from being added (NeoForge's EntityJoinLevelEvent). If not
     *                  (Fabric, which reports entities once they are already tracked), unwanted entities are discarded
     *                  next tick instead.
     * @return true if the caller should cancel adding the entity
     */
    public static boolean handleEntityLoad(Entity entity, ServerLevel world, boolean canCancel) {
        MinecraftServer server = world.getServer();
        PetTracker tracker = PetTracker.get(server);

        // Remove entities that have been recreated elsewhere, or left over from an untracked pet
        if (tracker.getRecreatedEntityMap().remove(entity.getUUID())) {
            tracker.setDirty();
            return removeLoadedEntity(server, entity, canCancel);
        }

        if (entity instanceof TrackableEntityDecorator trackable) {
            UUID uuid = trackable.friendsforlife_getUuid();
            PetData pd = uuid != null ? tracker.get(uuid) : null;
            if (pd != null) {
                int generation = trackable.friendsforlife_getGeneration();
                if (generation < pd.generation) {
                    // Saved before the pet was last summoned: a leftover copy, whatever its entity UUID
                    Init.LOGGER.info("Removing an old copy of pet {} owned by {} (summon {} of {})", pd.uuid,
                            pd.OwnerName, generation, pd.generation);
                    return removeLoadedEntity(server, entity, canCancel);
                }
                if (generation > pd.generation) {
                    // The pet data is older than the entity (e.g. the pet data wasn't saved before a crash)
                    pd.generation = generation;
                    tracker.setDirty();
                }
                if (!entity.getUUID().equals(pd.entityUuid)) {
                    Entity known = pd.entityUuid != null ? findLoadedEntity(server, pd.entityUuid) : null;
                    if (known == null) {
                        // The pet under a new entity UUID (another mod rebuilt or copied it), or the entity UUID
                        // wasn't known yet (saves from before 1.4.0)
                        pd.entityUuid = entity.getUUID();
                        tracker.setDirty();
                    } else {
                        // Another loaded copy is the pet as far as we know. Mods that rebuild entities (e.g. Via
                        // Romana) add the new copy before discarding the old one, so decide once that has happened.
                        TaskScheduler.schedule(() -> resolveSameSummonCopies(server, pd, known, entity), 2);
                    }
                }
                pd.trySetMode(pd.mode, entity);
                trackable.friendsforlife_setDowned(pd.isDowned);
            }
        } else {
            // A tracked pet's entity that isn't a pet anymore e.g. a pig that 1.3.x recreated a pet as
            PetData pd = tracker.getByEntityId(entity.getUUID());
            if (pd != null) {
                Init.LOGGER.warn("Untracking pet {} owned by {} and removing its leftover {}", pd.uuid, pd.OwnerName,
                        BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()));
                tracker.untrack(server, pd);
                return removeLoadedEntity(server, entity, canCancel);
            }
        }
        return false;
    }

    /**
     * Two copies of a pet from the same summon are loaded: {@code known} (the pet's recorded entity) and
     * {@code newcomer}. If the known copy has gone by now, the newcomer replaced it and becomes the pet; otherwise the
     * newcomer is a duplicate.
     */
    private static void resolveSameSummonCopies(MinecraftServer server, PetData pd, Entity known, Entity newcomer) {
        PetTracker tracker = PetTracker.get(server);
        if (tracker.get(pd.uuid) != pd || newcomer.isRemoved()) {
            return; // untracked or resummoned meanwhile, or the newcomer is gone anyway
        }
        if (known.isRemoved()) {
            pd.entityUuid = newcomer.getUUID();
            tracker.setDirty();
        } else if (!newcomer.getUUID().equals(pd.entityUuid)) {
            Init.LOGGER.warn("Removing a duplicate of pet {} owned by {} ({} at {})", pd.uuid, pd.OwnerName,
                    newcomer.getUUID(), newcomer.blockPosition().toShortString());
            newcomer.discard();
        }
    }

    @Nullable
    private static Entity findLoadedEntity(MinecraftServer server, UUID entityUuid) {
        for (ServerLevel level : server.getAllLevels()) {
            Entity entity = level.getEntity(entityUuid);
            if (entity != null && !entity.isRemoved()) {
                return entity;
            }
        }
        return null;
    }

    private static boolean removeLoadedEntity(MinecraftServer server, Entity entity, boolean canCancel) {
        if (canCancel) {
            // Not added at all. Discarding it instead would leave a ghost on clients: the entity isn't tracked yet
            // at this point, so when tracking starts the client is sent an already-removed entity and never told
            // it's gone.
            return true;
        }
        server.execute(entity::discard); // next tick, after it has been sent to clients, so they get the removal
        return false;
    }

    // Snapshot entity to PetData NBT when unloaded
    public static void handleEntityUnload(Entity entity, ServerLevel world) {
        if (entity instanceof TamableAnimal tame && !world.isClientSide()) {
            PetTracker tracker = PetTracker.get(world.getServer());
            PetData pd = tracker.getByEntityId(tame.getUUID());
            if (pd != null) {
                pd.updateEntityData(world.getServer(), tame);
            }
        }
    }
}
