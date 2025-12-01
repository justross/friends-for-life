//? if fabric {
package net.arcana.friendsforlife.loaders.fabric;

import net.arcana.friendsforlife.*;
import net.arcana.friendsforlife.accessors.TameableEntityDecorator;
import net.arcana.friendsforlife.items.CollarItem;
import net.arcana.friendsforlife.items.Items;
import net.arcana.friendsforlife.items.OcarinaItem;
import net.arcana.friendsforlife.petdatabase.OcarinaRegistry;
import net.arcana.friendsforlife.petdatabase.PetData;
import net.arcana.friendsforlife.petdatabase.PetTracker;
import net.arcana.friendsforlife.petdatabase.PetWatcher;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.item.ItemStack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class FabricEntrypoint implements ModInitializer {
    public static final String MOD_ID = "friendsforlife";
    // public static final FriendsForLifeConfig CONFIG = FriendsForLifeConfig.createAndLoad();
    private final Map<UUID, Object> teleportLocks = new ConcurrentHashMap<>();

    // This logger is used to write text to the console and the log file.
    // It is considered best practice to use your mod id as the logger's customName.
    // That way, it's clear which mod wrote info, warnings, and errors.
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static void initialize() {
    }

    @Override
    public void onInitialize() {
        // This code runs as soon as Minecraft is in a mod-load-ready state.
        // However, some things (like resources) may still be uninitialized.
        // Proceed with mild caution.

        // Register sounds/items
        Items.register();
        Sounds.register();

        PetWatcher.register();
        Commands.register();

        Net.register();
        TaskScheduler.register();

        // cause dogs are annoying and return from a interaction early, we have to
        // overwrite nbt methods
        UseEntityCallback.EVENT.register(((playerEntity, world, hand, entity, entityHitResult) -> {
            if (entityHitResult != null) {
                return InteractionResult.PASS;
            }
            if (world.isClientSide)
                return InteractionResult.PASS;

            if (entity instanceof TamableAnimal tame) {
                if (!tame.isOwnedBy(playerEntity))
                    return InteractionResult.PASS;

                ItemStack stack = playerEntity.getItemInHand(hand);
                PetTracker tracker = PetTracker.get(playerEntity.getServer());
                PetData pd = tracker.get(((TameableEntityDecorator) tame).friendsforlife_getUuid());
                if (stack.getItem() instanceof CollarItem collarItem && pd == null) {
                    return collarItem.applyCollar(stack, (ServerPlayer) playerEntity, tame);
                } else if (stack.isEmpty() && playerEntity.isShiftKeyDown()) {
                    ItemStack collar = CollarItem.getCollar(playerEntity, tame, pd);
                    if (collar == null)
                        return InteractionResult.PASS;
                    if (pd == null) {
                        FabricEntrypoint.LOGGER.error(MOD_ID, "Unable to find PetData for tracked entity " + tame.getStringUUID());
                        return InteractionResult.PASS;
                    }
                    CollarItem.removePet(tame, pd);

                    playerEntity.addItem(collar);
                    return InteractionResult.CONSUME;
                }

                if (stack.getItem() instanceof OcarinaItem ocarina && pd != null) {
                    return ocarina.useOcarinaFromPet(world, playerEntity, stack, pd, tame);
                }

            }
            return InteractionResult.PASS;
        }));

        // Remove entities that have been duplicated and marked as such
        ServerEntityEvents.ENTITY_LOAD.register((entity, world) -> {
            if (entity instanceof TamableAnimal) {
                PetTracker tracker = PetTracker.get(world.getServer());
                Set<UUID> recreatedSet = tracker.getRecreatedEntityMap();
                if (recreatedSet.remove(entity.getUUID())) {
                    tracker.setDirty();
                    world.getServer().execute(() -> entity.discard()); // run next tick
                }
            }
        });

        // Snapshot entity to PetData NBT when unloaded
        ServerEntityEvents.ENTITY_UNLOAD.register((entity, world) -> {
            if (entity instanceof TamableAnimal tame && !world.isClientSide) {
                PetTracker tracker = PetTracker.get(world.getServer());
                PetData pd = tracker.getByEntityId(tame.getUUID());
                if (pd != null) {
                    pd.updateEntityData(nbt -> tame.saveWithoutId(nbt));
                }
            }
        });
    }
}
//?}
