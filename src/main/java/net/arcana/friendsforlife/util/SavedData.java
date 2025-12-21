package net.arcana.friendsforlife.util;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.DimensionDataStorage;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;

public abstract class SavedData extends net.minecraft.world.level.saveddata.SavedData {
    protected SavedData() {
        super();
    }

    //? if >=1.21 {
    /*@Override
    public @NotNull CompoundTag save(@NotNull CompoundTag tag, HolderLookup.@NotNull Provider provider) {
        write(tag);
        return tag;
    }
    *///?} else {
    @Override
    public @NotNull CompoundTag save(CompoundTag tag) {
        write(tag);
        return tag;
    }
    //?}

    /**
     * Version-agnostic wrapper for save method
     */
    protected abstract void write(CompoundTag tag);

    public static <T extends SavedData> T get(
            MinecraftServer server,
            String key,
            Function<CompoundTag, T> fromNbt,
            Supplier<T> constructor
    ) {
        Objects.requireNonNull(server, "server cannot be null");

        DimensionDataStorage manager = Objects.requireNonNull(server.getLevel(Level.OVERWORLD)).getDataStorage();

        //? if >1.21 {
        /*@SuppressWarnings("DataFlowIssue") net.minecraft.world.level.saveddata.SavedData.Factory<T> factory =
                new net.minecraft.world.level.saveddata.SavedData.Factory<>(
                        constructor,
                        (tag, provider) -> fromNbt.apply(tag),
                        null
                );

        return manager.computeIfAbsent(factory, key);
        *///?} else {
        return manager.computeIfAbsent(fromNbt::apply, constructor, key);
        //?}
    }
}