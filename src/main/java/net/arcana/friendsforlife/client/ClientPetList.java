package net.arcana.friendsforlife.client;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import net.arcana.friendsforlife.petdatabase.PetListUpdateEvents;
import net.minecraft.client.player.LocalPlayer;

public class ClientPetList {

    // Maps pet UUID -> ClientPetData
    private static final Map<UUID, ClientPetData> pets = new ConcurrentHashMap<>();
    private static final List<Runnable> listeners = new ArrayList<>();

    static {
        // Subscribe to server updates once when class is loaded
        PetListUpdateEvents.subscribe((playerUuid, newPetList) -> {
            setPets(newPetList);
        });
    }

    public static void reset() {
        pets.clear();
        listeners.clear();
    }

    public static synchronized void setPets(List<ClientPetData> newPets) {
        pets.clear();
        for (ClientPetData pet : newPets) {
            pets.put(pet.uuid, pet);
        }
        notifyListeners();
    }

    public static synchronized List<ClientPetData> getAllPets() {
        return List.copyOf(pets.values());
    }

    public static synchronized List<ClientPetData> getAllForPlayer(LocalPlayer player) {
        return List.copyOf(pets.values().stream()
                .filter(pet -> pet.getOwnerUuid().equals(player.getUUID()))
                .toList());
    }

    public static ClientPetData getPet(UUID petUuid) {
        return pets.get(petUuid);
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
