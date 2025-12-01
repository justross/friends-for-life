package net.arcana.friendsforlife.client;

import net.arcana.friendsforlife.Net;
import net.arcana.friendsforlife.homeblock.HomeBlockUpdateEvents;
import net.arcana.friendsforlife.packets.HomeRequestPacket;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import java.util.ArrayList;
import java.util.List;

public class ClientHomeBlock {

    private static final List<Runnable> listeners = new ArrayList<>();
    private static BlockPos blockPos;
    private static ResourceKey<Level> dimension;

    static {
        // Subscribe to server updates once when class is loaded
        HomeBlockUpdateEvents.subscribe((playerUuid, blockPos, dim) -> {
            set(blockPos, dim);
        });
    }

    /**
     * Sends a request to get the home block position
     */
    public static void initialize() {
        Net.CHANNEL.clientHandle().send(new HomeRequestPacket()); // Request home position from server
    }

    public static void reset() {
        blockPos = null;
        listeners.clear();
    }

    public static synchronized void set(BlockPos blockPos, ResourceKey<Level> dim) {
        ClientHomeBlock.blockPos = blockPos;
        ClientHomeBlock.dimension = dim;
        notifyListeners();
    }

    public static synchronized BlockPos get() {
        return blockPos;
    }

    public static synchronized ResourceKey<Level> getDimKey() {
        return dimension;
    }

    // --- subscription methods ---
    @SuppressWarnings("unused")
    public static void addListener(Runnable listener) {
        listeners.add(listener);
    }

    @SuppressWarnings("unused")
    public static void removeListener(Runnable listener) {
        listeners.remove(listener);
    }

    private static void notifyListeners() {
        for (Runnable listener : List.copyOf(listeners)) {
            listener.run();
        }
    }
}
