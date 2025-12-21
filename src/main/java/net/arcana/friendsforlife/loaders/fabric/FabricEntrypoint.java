//? if fabric {
package net.arcana.friendsforlife.loaders.fabric;

import net.arcana.friendsforlife.*;
import net.arcana.friendsforlife.FriendsForLifeConfig;
import net.arcana.friendsforlife.client.EventHandler;
import net.arcana.friendsforlife.components.Components;
import net.arcana.friendsforlife.items.Items;
import net.arcana.friendsforlife.petdatabase.PetData;
import net.arcana.friendsforlife.petdatabase.PetTracker;
import net.arcana.friendsforlife.petdatabase.PetWatcher;
import net.arcana.friendsforlife.util.CompatConfig;
import net.arcana.friendsforlife.util.ServerContext;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.TamableAnimal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FabricEntrypoint implements ModInitializer {
    public static final String MOD_ID = "friendsforlife";
     public static final FriendsForLifeConfig CONFIG = FriendsForLifeConfig.createAndLoad();

    // This logger is used to write text to the console and the log file.
    // It is considered best practice to use your mod id as the logger's customName.
    // That way, it's clear which mod wrote info, warnings, and errors.
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        // This code runs as soon as Minecraft is in a mod-load-ready state.
        // However, some things (like resources) may still be uninitialized.
        // Proceed with mild caution.

        // Register sounds/items
        Items.registerAll((id, item) -> Registry.register(BuiltInRegistries.ITEM, id, item));
        CreativeItemRegistrationFabric.register();
        Sounds.registerAll((id, soundEvent) ->
                Registry.register(BuiltInRegistries.SOUND_EVENT, id, soundEvent)
        );

        PetWatcher.register();
        Commands.register();

        Net.register();
        TaskScheduler.register();
        //? if >=1.21 {
        /*Components.registerAll((id, dataComponentType) -> Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE, id, dataComponentType));
         *///?}

        final class FabricConfigBacking implements CompatConfig.Impl {

            @Override
            public int petRespawnTimer() {
                return CONFIG.petRespawnTimer();
            }
        }

        CompatConfig.setBacking(new FabricConfigBacking());

        UseEntityCallback.EVENT.register(((playerEntity, world, hand, entity, entityHitResult) -> {
            if (entityHitResult != null) {
                return InteractionResult.PASS;
            }
            return EventHandler.handleEntityUse(playerEntity, world, hand, entity);
        }));

        // Helper to provide server context for classes
        ServerLifecycleEvents.SERVER_STARTED.register(ServerContext::setServer);

        ServerEntityEvents.ENTITY_LOAD.register(EventHandler::handleEntityLoad);

        // Snapshot entity to PetData NBT when unloaded
        ServerEntityEvents.ENTITY_UNLOAD.register((entity, world) -> {
            if (entity instanceof TamableAnimal tame && !world.isClientSide) {
                PetTracker tracker = PetTracker.get(world.getServer());
                PetData pd = tracker.getByEntityId(tame.getUUID());
                if (pd != null) {
                    pd.updateEntityData(tame::saveWithoutId);
                }
            }
        });

        // Server tick stuff
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            TaskScheduler.tick();
            PetWatcher.onServerTick(server);
        });

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(
                    // /listpets → lists your own pets
                    net.minecraft.commands.Commands.literal("listpets").executes(Commands::listPetsSelf)
                            // /listpets <player> → lists another player's pets
                            .then(net.minecraft.commands.Commands.argument("player", EntityArgument.player()).executes(Commands::listPetsOther)));
        });
    }
}
//?}
