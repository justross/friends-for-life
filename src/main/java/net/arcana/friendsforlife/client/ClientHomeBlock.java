package net.arcana.friendsforlife.client;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import net.arcana.friendsforlife.Net;
import net.arcana.friendsforlife.homeblock.HomeBlockUpdateEvents;
import net.arcana.friendsforlife.packets.HomeRequestPacket;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

public class ClientHomeBlock {

    private static final List<Runnable> listeners = new ArrayList<>();
    private static @Nullable BlockPos blockPos;
    private static @Nullable ResourceKey<Level> dimension;

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

    @Nullable
    public static synchronized BlockPos get() {
        return blockPos;
    }

    public static synchronized ResourceKey<Level> getDimKey() {
        return dimension;
    }

    // --- subscription methods ---
    public static void addListener(Runnable listener) {
        listeners.add(listener);
    }

    public static void removeListener(Runnable listener) {
        listeners.remove(listener);
    }

    private static void notifyListeners() {
        for (Runnable listener : List.copyOf(listeners)) {
            listener.run();
        }
    }
}
