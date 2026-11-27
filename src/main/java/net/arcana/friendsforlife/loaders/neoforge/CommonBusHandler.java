//? if neoforge {
/*package net.arcana.friendsforlife.loaders.neoforge;

import net.arcana.friendsforlife.EventHandler;
import net.arcana.friendsforlife.Init;
import net.arcana.friendsforlife.TaskScheduler;
import net.arcana.friendsforlife.accessors.TrackableEntityDecorator;
import net.arcana.friendsforlife.petdatabase.PetData;
import net.arcana.friendsforlife.petdatabase.PetTracker;
import net.arcana.friendsforlife.petdatabase.PetWatcher;
import net.arcana.friendsforlife.util.ServerContext;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.EntityInteract;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

// Registered on the game bus by NeoforgeEntrypoint, on both sides (see ModBusHandler for why it isn't annotated)
public class CommonBusHandler {
    // cause dogs are annoying and return from a interaction early, we have to
    // overwrite nbt methods
    @SubscribeEvent
    public static void onEntityInteract(EntityInteract event) {
        Player player = event.getEntity();
        Level world = event.getLevel();
        InteractionHand hand = event.getHand();
        Entity entity = event.getTarget();

        var result = EventHandler.handleEntityUse(player, world, hand, entity);
        // InteractionResult is a sealed interface since 1.21.2, so compare instead of switching
        if (result != InteractionResult.PASS) {
            event.setCanceled(true);
            event.setCancellationResult(result);
        }
    }

    // Helper to provide server context for classes
    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) {
        ServerContext.setServer(event.getServer());
    }

    // Remove entities that have been duplicated and marked as such
    @SubscribeEvent
    public static void onEntityLoad(EntityJoinLevelEvent event) {
        if (!(event.getLevel() instanceof ServerLevel serverLevel)) {
            return;
        }
        var entity = event.getEntity();
        if (EventHandler.handleEntityLoad(entity, serverLevel, true)) {
            event.setCanceled(true);
        }
    }

    // Snapshot entity to PetData NBT when unloaded
    @SubscribeEvent
    public static void onEntityUnload(EntityLeaveLevelEvent event) {
        var entity = event.getEntity();
        var world = event.getLevel();
        EventHandler.handleEntityUnload(entity, world);
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        TaskScheduler.tick();
        PetWatcher.onServerTick(event.getServer());
    }
}
*///?}
