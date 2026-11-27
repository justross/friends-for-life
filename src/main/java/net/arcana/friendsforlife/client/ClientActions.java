package net.arcana.friendsforlife.client;

import net.minecraft.client.Minecraft;

/**
 * Client-only calls made from common classes (items etc.).
 * <p>
 * Keeping them here so a dedicated server never loads client classes when it loads the common class. Passing a
 * RosterScreen where a Screen is expected makes the JVM load both while verifying the caller, and Screen doesn't exist
 * on a server. Fabric and NeoForge 1.21.1 hid this by stripping @Environment/@OnlyIn(CLIENT) methods; NeoForge
 * 1.21.10 doesn't, and crashed on startup.
 */
public final class ClientActions {
    private ClientActions() {
    }

    public static void openRosterScreen() {
        Minecraft.getInstance().setScreen(new RosterScreen());
    }
}
