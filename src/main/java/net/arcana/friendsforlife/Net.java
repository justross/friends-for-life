package net.arcana.friendsforlife;

import io.wispforest.owo.network.OwoNetChannel;
import net.arcana.friendsforlife.homeblock.HomeBlock;
import net.arcana.friendsforlife.homeblock.HomeBlock.HomeBlockHandle;
import net.arcana.friendsforlife.packets.*;
import net.arcana.friendsforlife.petdatabase.OcarinaRegistry;
import net.arcana.friendsforlife.petdatabase.PetData;
import net.arcana.friendsforlife.petdatabase.PetTracker;
import net.arcana.friendsforlife.util.CompatResourceLocation;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class Net {
    public static final OwoNetChannel CHANNEL = OwoNetChannel.create(
            new CompatResourceLocation("friendsforlife", "main").unwrap());

    private static final Map<UUID, Object> teleportLocks = new ConcurrentHashMap<>();

    public static void register() {

        Net.CHANNEL.registerServerbound(HomeRequestPacket.class, (packet, handler) -> {
            ServerPlayer player = handler.player();

            // Get the persistent HomeBlock instance for this world
            HomeBlock homes = HomeBlock.get(Objects.requireNonNull(player.getServer()));
            HomeBlockHandle hbh = homes.getHome(player.getServer(), player.getUUID());

            if (hbh != null) {
                // Send it back to the client
                Net.CHANNEL.serverHandle(player).send(
                        new HomeUpdatePacket(hbh.pos(), hbh.dim().location()));
            }
        });

        // Send client pet list
        Net.CHANNEL.registerServerbound(PetListRequestPacket.class, (message, access) -> {
            ServerPlayer player = access.player();
            Net.CHANNEL.serverHandle(player).send(PetTracker.toEntries(player.getServer()));
        });

        // Send client ocarina registry
        Net.CHANNEL.registerServerbound(OcarinaListRequestPacket.class, ((message, access) -> {
            var player = access.player();
            var ocarinaRegistry = OcarinaRegistry.get(access.runtime());

            List<OcarinaListPacket.Entry> entries = ocarinaRegistry.getMap().entrySet()
                    .stream()
                    .map(e -> new OcarinaListPacket.Entry(e.getKey(), e.getValue()))
                    .toList();

            Net.CHANNEL.serverHandle(player).send(new OcarinaListPacket(entries));
        }));

        // Handle pet summon requests from clients
        Net.CHANNEL.registerServerbound(PetTeleportRequestPacket.class, (packet, handler) -> {
            try {
                UUID petUuid = packet.uuid();
                int x = packet.petTeleportRequestPos().getX();
                int y = packet.petTeleportRequestPos().getY();
                int z = packet.petTeleportRequestPos().getZ();
                ResourceLocation dim = packet.dim();
                Boolean shouldWander = packet.shouldWander();

                var server = handler.runtime();

                server.execute(() -> {
                    Object lock = teleportLocks.computeIfAbsent(petUuid, k -> new Object());
                    synchronized (lock) {

                        PetTracker tracker = PetTracker.get(server);
                        PetData petData = tracker.getTrackedMap().get(petUuid);
                        LivingEntity entity = null;
                        ResourceKey<Level> targetWorldKey = ResourceKey.create(Registries.DIMENSION, dim);

                        if (petData != null) {
                            entity = petData.recreateEntity(server, targetWorldKey, x, y, z);

                            if (entity == null) {
                                Init.LOGGER.error("Failed to recreate entity for pet UUID: " + petUuid);
                                return;
                            }

                            // Only tameable pets (i.e. not mountable animals) should implement custom wandering behavior
                            if (entity instanceof TamableAnimal tame) {
                                if (shouldWander) {
                                    TameableWanderHelper.makeTameableWander(tame);
                                } else {
                                    tame.setOrderedToSit(false);
                                    TameableWanderHelper.stopWandering(entity);
                                }
                            }

                        } else {
                            Init.LOGGER.error("No pet data found for UUID: " + petUuid);
                        }

                        teleportLocks.remove(petUuid);
                    }
                });
            } catch (Exception e) {
                Init.LOGGER.error("Bad summon packet from " + handler.player().getName().getString() + ": " + e);
            }
        });

        // Update PetData mode
        Net.CHANNEL.registerServerbound(SetPetModePacket.class, (packet, handler) -> {
            PetData pd = PetTracker.get(Objects.requireNonNull(handler.player().getServer())).get(packet.uuid());
            if (pd != null) {
                var mode = PetData.FollowMode.fromId(packet.mode());
                mode = pd.trySetMode(mode, handler.player().getServer());
                Net.CHANNEL.serverHandle(handler.player().getServer()).send(new PetModeUpdatePacket(packet.uuid(), mode.id));
            }
        });

        // Set individual pet home block
        Net.CHANNEL.registerServerbound(HomeSetRequestPacket.class, (packet, handler) -> {
            PetData pd = PetTracker.get(Objects.requireNonNull(handler.player().getServer())).get(packet.uuid());
            if (pd != null) {
                pd.homePosition = packet.homeSetRequestPos();
                pd.homeDimension = ResourceKey.create(Registries.DIMENSION, packet.dimId());
            }
        });

        // Set default home block
        Net.CHANNEL.registerServerbound(DefaultSetHomeRequestPacket.class, (packet, handler) -> {
            HomeBlock.get(Objects.requireNonNull(handler.player().getServer())).setHome(handler.player(), packet.defaultSetHomeRequestPos(),
                    ResourceKey.create(Registries.DIMENSION, packet.dimId()));
        });

        /* Update client with new home position if respawn point changes and home is not set */
        Net.CHANNEL.registerServerbound(RespawnUpdatePacket.class, (packet, handler) -> {
            if (!HomeBlock.get(Objects.requireNonNull(handler.player().getServer())).hasHome(handler.player().getUUID())) {

                Net.CHANNEL.serverHandle(handler.player()).send(new HomeUpdatePacket(packet.respawnUpdatePos(), packet.dimId()));
            }
        });

        Net.CHANNEL.registerClientboundDeferred(PetListPacket.class);
        Net.CHANNEL.registerClientboundDeferred(PetDataUpdatePacket.class);
        Net.CHANNEL.registerClientboundDeferred(PetTrackerUpdatePacket.class);
        Net.CHANNEL.registerClientboundDeferred(PetModeUpdatePacket.class);
        Net.CHANNEL.registerClientboundDeferred(PetDownedUpdatePacket.class);
        Net.CHANNEL.registerClientboundDeferred(HomeUpdatePacket.class);
        Net.CHANNEL.registerClientboundDeferred(OcarinaListPacket.class);
        Net.CHANNEL.registerClientboundDeferred(OcarinaUpdatePacket.class);
        Net.CHANNEL.registerClientboundDeferred(PetMissingMessagePacket.class);
    }
}