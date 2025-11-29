package net.arcana.friendsforlife.homeblock;

import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import org.apache.logging.log4j.util.TriConsumer;

public class HomeBlockUpdateEvents {
    public interface IListener extends TriConsumer<UUID, BlockPos, ResourceKey<Level>> {}
    // Thread-safe list for concurrent modifications
    private static final CopyOnWriteArrayList<IListener> listeners = new CopyOnWriteArrayList<>();

    /** Register a listener to be called whenever a home is updated */
    public static void subscribe(IListener listener) {
        listeners.add(listener);
    }

    /** Remove a listener if it’s no longer needed */
    public static void unsubscribe(IListener listener) {
        listeners.remove(listener);
    }

    /** Notify all listeners that a home position changed */
    public static void fire(UUID playerUuid, BlockPos pos, ResourceKey<Level> dim) {
        for (var listener : listeners) {
            listener.accept(playerUuid, pos, dim);
        }
    }
}
