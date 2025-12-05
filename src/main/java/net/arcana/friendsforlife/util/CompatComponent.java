package net.arcana.friendsforlife.util;

import net.minecraft.core.HolderLookup;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.MinecraftServer;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public class CompatComponent {
    /**
     * Version-neutral Component deserialization from JSON string
     */
    @Nullable
    public static MutableComponent fromJson(String string, @Nullable HolderLookup.Provider provider) {
        //? if >=1.21 {
        if (provider == null) {
            MinecraftServer server = ServerContext.getServer();
            provider = server.registryAccess();
        }
        return string == null ? null : Component.Serializer.fromJson(string, provider);
        //?} else {
        /*return string == null ? null : Component.Serializer.fromJson(string);
         *///?}
    }

    /**
     * Version-neutral serialization to JSON string
     */
    public static String toJson(Component component, @Nullable HolderLookup.Provider provider) {
        //? if >=1.21 {
        if (provider == null) {
            MinecraftServer server = ServerContext.getServer();
            provider = server.registryAccess();
        }
        return Component.Serializer.toJson(component, provider);
        //?} else {
        /*return Component.Serializer.toJson(component);
         *///?}
    }

    /**
     * Convenience overloads without provider (for older versions)
     */
    @Nullable
    public static MutableComponent fromJson(String string) {
        return fromJson(string, null);
    }

    public static String toJson(Component component) {
        return toJson(component, null);
    }
}
