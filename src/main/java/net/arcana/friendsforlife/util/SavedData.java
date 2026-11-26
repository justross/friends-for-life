package net.arcana.friendsforlife.util;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.Level;
//? if >=1.21.10 {
/*import net.minecraft.world.level.saveddata.SavedDataType;
*///?}
import net.minecraft.world.level.storage.DimensionDataStorage;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;

public abstract class SavedData extends net.minecraft.world.level.saveddata.SavedData {
    protected SavedData() {
        super();
    }

    //? if >=1.21.10 {
    /*public final CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        write(tag);
        // Encode to DataResult<Tag> using NbtOps
        DataResult<CompoundTag> result = Codec.unit(this)
                .encodeStart(NbtOps.INSTANCE, this)
                .flatMap(tagged -> DataResult.success(tag));
        return result.result().orElse(tag); // fallback to tag if encoding fails
    }
    *///?} else if >=1.21 {
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

    //? if >=1.21.10 {

    /*/^*
     * A SavedDataType whose codec wraps write()/fromNbt, so data is stored in the same layout on every
     * version and worlds upgraded from older versions keep it.
     ^/
    public static <T extends SavedData> SavedDataType<T> nbtType(String key, Supplier<T> constructor,
                                                                 Function<CompoundTag, T> fromNbt) {
        Codec<T> codec = CompoundTag.CODEC.xmap(fromNbt, data -> {
            CompoundTag tag = new CompoundTag();
            data.write(tag);
            return tag;
        });
        return new SavedDataType<>(key, constructor, codec, null);
    }

    public static <T extends SavedData> T get(
            MinecraftServer server,
            SavedDataType<T> type
    ) {
        Objects.requireNonNull(server, "server cannot be null");

        DimensionDataStorage manager = Objects.requireNonNull(server.getLevel(Level.OVERWORLD)).getDataStorage();

        return manager.computeIfAbsent(type);
    }
    *///?} else {
    
    public static <T extends SavedData> T get(
            MinecraftServer server,
            String key,
            Function<CompoundTag, T> fromNbt,
            Supplier<T> constructor    ) {
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
     //?}
}