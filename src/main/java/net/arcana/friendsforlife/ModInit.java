package net.arcana.friendsforlife;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
import net.minecraft.entity.passive.TameableEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;

public class ModInit implements ModInitializer {
    public static final String MOD_ID = "friendsforlife";
    // public static final FriendsForLifeConfig CONFIG = FriendsForLifeConfig.createAndLoad();
    private final Map<UUID, Object> teleportLocks = new ConcurrentHashMap<>();

    // This logger is used to write text to the console and the log file.
    // It is considered best practice to use your mod id as the logger's name.
    // That way, it's clear which mod wrote info, warnings, and errors.
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

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
                return ActionResult.PASS;
            }
            if (world.isClient)
                return ActionResult.PASS;

            if (entity instanceof TameableEntity tame) {
                if (!tame.isOwner(playerEntity))
                    return ActionResult.PASS;

                ItemStack stack = playerEntity.getStackInHand(hand);
                PetTracker tracker = PetTracker.get(playerEntity.getServer());
                PetData pd = tracker.get(((TameableEntityDecorator) tame).friendsforlife_getUuid());
                if (stack.getItem() instanceof CollarItem collarItem && pd == null) {
                    return collarItem.applyCollar(stack, (ServerPlayerEntity) playerEntity, tame);
                } else if (stack.isEmpty() && playerEntity.isSneaking()) {
                    ItemStack collar = CollarItem.getCollar(playerEntity, tame, pd);
                    if (collar == null)
                        return ActionResult.PASS;
                    if (pd == null) {
                        ModInit.LOGGER.error(MOD_ID, "Unable to find PetData for tracked entity " + tame.getUuidAsString());
                        return ActionResult.PASS;
                    }
                    CollarItem.removePet(tame, pd);

                    playerEntity.giveItemStack(collar);
                    return ActionResult.CONSUME;
                }

                if (stack.getItem() instanceof OcarinaItem ocarina && pd != null) {
                    return ocarina.useOcarinaFromPet(world, playerEntity, stack, pd, tame);
                }

            }
            return ActionResult.PASS;
        }));

        // Register a global receiver to handle pet tracker requests from clients
        ServerPlayNetworking.registerGlobalReceiver(Packets.REQUEST_PET_TRACKER,
                (server, player, handler, buf, responseSender) -> {
                    server.execute(() -> {
                        var petList = PetTracker.serializePetList(server);

                        // send back to the client
                        ServerPlayNetworking.send(player, Packets.RESPONSE_PET_TRACKER, petList);
                    });
                });

        // Register a global receiver to handle pet summon requests from clients
        ServerPlayNetworking.registerGlobalReceiver(Packets.REQUEST_PET_TELEPORT,
                (server, player, handler, buf, responseSender) -> {
                    try {
                        UUID petUuid = buf.readUuid();
                        int x = buf.readInt();
                        int y = buf.readInt();
                        int z = buf.readInt();
                        Identifier dim = buf.readIdentifier();
                        Boolean shouldWander = buf.readBoolean();

                        server.execute(() -> {
                            Object lock = teleportLocks.computeIfAbsent(petUuid, k -> new Object());
                            synchronized (lock) {

                                PetTracker tracker = PetTracker.get(server);
                                PetData petData = tracker.getTrackedMap().get(petUuid);
                                TameableEntity entity = null;
                                RegistryKey<World> targetWorldKey = RegistryKey.of(RegistryKeys.WORLD, dim);

                                if (petData != null) {
                                    entity = petData.recreateEntity(server, targetWorldKey, x, y, z);

                                    if (entity == null) {
                                        ModInit.LOGGER.error(MOD_ID, "Failed to recreate entity for pet UUID: " + petUuid);
                                        return;
                                    }

                                    if (shouldWander) {
                                        TameableWanderHelper.makeTameableWander(entity);
                                    } else {
                                        entity.setSitting(false);
                                        TameableWanderHelper.stopWandering(entity);
                                    }

                                } else {
                                    ModInit.LOGGER.error(MOD_ID, "No pet data found for UUID: " + petUuid);
                                }

                                teleportLocks.remove(petUuid);
                            }
                        });
                    } catch (Exception e) {
                        ModInit.LOGGER.error(MOD_ID, "Bad summon packet from " + player.getName().getString() + ": " + e);
                    }
                });

                ServerPlayNetworking.registerGlobalReceiver(Packets.REQUEST_OCARINA_REGISTRY, 
                (server, player, handler, buf, responseSender) -> {
                    server.execute(() -> {
                        var ocarinaList = OcarinaRegistry.get(server).serializeList();

                        // send back to the client
                        ServerPlayNetworking.send(player, Packets.REQUEST_OCARINA_REGISTRY, ocarinaList);
                    });
                });
                
        // Remove entities that have been duplicated and marked as such
        ServerEntityEvents.ENTITY_LOAD.register((entity, world) -> {
            if (entity instanceof TameableEntity) {
                PetTracker tracker = PetTracker.get(world.getServer());
                Set<UUID> recreatedSet = tracker.getRecreatedMap();
                if (recreatedSet.remove(entity.getUuid())) {
                    tracker.markDirty();
                    world.getServer().execute(() -> entity.discard()); // run next tick
                }
            }
        });

        // Snapshot entity to PetData NBT when unloaded
        ServerEntityEvents.ENTITY_UNLOAD.register((entity, world) -> {
            if (entity instanceof TameableEntity tame && !world.isClient) {
                PetTracker tracker = PetTracker.get(world.getServer());
                PetData pd = tracker.getByEntityId(tame.getUuid());
                if (pd != null) {
                    pd.updateEntityData(nbt -> tame.writeNbt(nbt));
                }
            }
        });
    }
}