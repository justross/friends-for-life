package net.arcana.friendsforlife.util;

import net.minecraft.server.MinecraftServer;

public final class ServerContext {
    private static MinecraftServer server;

    public static void setServer(MinecraftServer s) {
        server = s;
    }

    public static MinecraftServer getServer() {
        return server;
    }
}
