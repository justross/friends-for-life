//? if neoforge {
/*package net.arcana.friendsforlife.loaders.neoforge;

import net.arcana.friendsforlife.Init;
import net.arcana.friendsforlife.Sounds;
import net.arcana.friendsforlife.TaskScheduler;
import net.arcana.friendsforlife.accessors.TameableEntityDecorator;
import net.arcana.friendsforlife.components.components.Components;
import net.arcana.friendsforlife.items.CollarItem;
import net.arcana.friendsforlife.items.Items;
import net.arcana.friendsforlife.items.OcarinaItem;
import net.arcana.friendsforlife.petdatabase.PetData;
import net.arcana.friendsforlife.petdatabase.PetTracker;
import net.arcana.friendsforlife.petdatabase.PetWatcher;
import net.arcana.friendsforlife.util.ServerContext;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.server.ServerLifecycleEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.registries.RegisterEvent;

import java.util.Objects;
import java.util.Set;
import java.util.UUID;

@EventBusSubscriber(modid = Init.MOD_ID, value = Dist.DEDICATED_SERVER)
public class CommonBusHandler {

    // cause dogs are annoying and return from a interaction early, we have to
    // overwrite nbt methods
    @SubscribeEvent
    public static void onEntityInteract(PlayerInteractEvent.EntityInteractSpecific event) {
        Player player = event.getEntity();
        Level world = event.getLevel();
        InteractionHand hand = event.getHand();
        Entity entity = event.getTarget();

        if (world.isClientSide()) {
            return;
        }

        if (entity instanceof TamableAnimal tame) {
            if (!tame.isOwnedBy(player))
                return;

            ItemStack stack = player.getItemInHand(hand);
            PetTracker tracker = PetTracker.get(player.getServer());
            PetData pd = tracker.get(((TameableEntityDecorator) tame).friendsforlife_getUuid());
            if (stack.getItem() instanceof CollarItem collarItem && pd == null) {
                collarItem.applyCollar(stack, (ServerPlayer) player, tame);
                return;
            } else if (stack.isEmpty() && player.isShiftKeyDown()) {
                ItemStack collar = CollarItem.getCollar(player, tame, pd);
                if (collar == null)
                    return;
                if (pd == null) {
                    Init.LOGGER.error("Unable to find PetData for tracked entity " + tame.getStringUUID());
                }
                CollarItem.removePet(tame, pd);

                player.addItem(collar);
            }

            if (stack.getItem() instanceof OcarinaItem ocarina && pd != null) {
                ocarina.useOcarinaFromPet(world, player, stack, pd, tame);
            }

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
        var entity = event.getEntity();
        var world = event.getLevel();
        var reloaded = event.loadedFromDisk();
        if (reloaded && entity instanceof TamableAnimal) {
            PetTracker tracker = PetTracker.get(ServerContext.getServer());
            Set<UUID> recreatedSet = tracker.getRecreatedEntityMap();
            if (recreatedSet.remove(entity.getUUID())) {
                tracker.setDirty();
                Objects.requireNonNull(ServerContext.getServer()).execute(entity::discard); // run next tick
            }
        }
    }

    // Snapshot entity to PetData NBT when unloaded
    @SubscribeEvent
    public static void onEntityUnload(EntityLeaveLevelEvent event) {
        var entity = event.getEntity();
        var world = event.getLevel();
        if (entity instanceof TamableAnimal tame && !world.isClientSide) {
            PetTracker tracker = PetTracker.get(world.getServer());
            PetData pd = tracker.getByEntityId(tame.getUUID());
            if (pd != null) {
                pd.updateEntityData(tame::saveWithoutId);
            }
        }
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        TaskScheduler.tick();
        PetWatcher.onServerTick(event.getServer());
    }
}
*///?}
