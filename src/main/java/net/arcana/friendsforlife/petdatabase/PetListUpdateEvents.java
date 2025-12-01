package net.arcana.friendsforlife.petdatabase;

import net.arcana.friendsforlife.client.ClientPetData;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BiConsumer;

public class PetListUpdateEvents {

    // Thread-safe list for concurrent modifications
    private static final CopyOnWriteArrayList<BiConsumer<UUID, List<ClientPetData>>> listeners = new CopyOnWriteArrayList<>();

    /** Register a listener to be called whenever a home is updated */
    public static void subscribe(BiConsumer<UUID, List<ClientPetData>> listener) {
        listeners.add(listener);
    }

    /** Remove a listener if it’s no longer needed */
    public static void unsubscribe(BiConsumer<UUID, List<ClientPetData>> listener) {
        listeners.remove(listener);
    }

    /** Notify all listeners that the pet lsit changed */
    public static void fire(UUID playerUuid, List<ClientPetData> petList) {
        for (var listener : listeners) {
            listener.accept(playerUuid, petList);
        }
    }
}
