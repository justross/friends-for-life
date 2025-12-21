package net.arcana.friendsforlife.client.petdatabase;

import net.arcana.friendsforlife.petdatabase.OcarinaRegistry;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ClientOcarinaRegistry {

    // Maps each ocarina to the pet it's bound to (or null if unbound)
    private static final Map<UUID, UUID> MAP = new HashMap<>();

    public static void initialize() {
    }

    public static void set(UUID ocarinaId, @Nullable UUID petId) {
        if (ocarinaId != null) {
            MAP.put(ocarinaId, petId);
        }
    }

    @Nullable
    public static UUID getPet(UUID ocarinaId) {
        if (ocarinaId == null)
            return null;

        UUID petId = MAP.get(ocarinaId);

        if (petId == null || petId.equals(OcarinaRegistry.EMPTY_UUID)) {
            return null;
        }

        return petId;
    }

    public static boolean hasPet(UUID ocarinaId) {
        return getPet(ocarinaId) != null;
    }

    public static void clear() {
        MAP.clear();
    }
}
