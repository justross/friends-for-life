package net.arcana.friendsforlife.client;

//? if >=1.21.10 {
/*import net.arcana.friendsforlife.util.CompatResourceLocation;
import net.minecraft.client.KeyMapping;
*///?}

/**
 * Client-only key binding constants. KeyMapping only exists on the client: when this category was a field of Init,
 * which server code uses, a 1.21.10 dedicated server crashed the first time anything logged through Init.LOGGER.
 */
public final class Keybinds {
    private Keybinds() {
    }

    //? if >=1.21.10 {
    /*public static final KeyMapping.Category CATEGORY =
            new KeyMapping.Category(new CompatResourceLocation("friendsforlife", "controls").unwrap());
    *///?}
}
