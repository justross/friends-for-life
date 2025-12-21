package net.arcana.friendsforlife.client.petdatabase;

import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

public class PetDataUpdateEvent {
    public interface IListener extends Consumer<ClientPetData> {
    }

    private static final CopyOnWriteArrayList<IListener> listeners = new CopyOnWriteArrayList<>();

    public static void subscribe(IListener listener) {
        listeners.add(listener);
    }

    public static void unsubscribe(IListener listener) {
        listeners.remove(listener);
    }

    public static void fire(ClientPetData clientPetData) {
        for (var listener : listeners) {
            listener.accept(clientPetData);
        }
    }
}
