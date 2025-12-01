package net.arcana.friendsforlife.util;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.DimensionDataStorage;

import java.util.function.Function;
import java.util.function.Supplier;

public abstract class SavedData extends net.minecraft.world.level.saveddata.SavedData {
    // 1.20-style constructor
    protected SavedData() {
        super();
    }

    // 1.21-style constructor
//    protected SavedData(CompoundTag nbt) {
//        super();
//        load(nbt);
//    }

    /**
     * Wrap load for 1.21+ or 1.20
     */
//    protected void load(CompoundTag nbt) {
//        try {
//            // 1.21+ might expect this behavior
//            super.load(nbt);
//        } catch (NoSuchMethodError e) {
//            // fallback for 1.20
//            super.load(nbt);
//        }
//    }

    /**
     * Wrap save for 1.21+ or 1.20
     */
    public CompoundTag save() {
        try {
            return this.save(); // 1.21+ signature
        } catch (NoSuchMethodError e) {
            CompoundTag tag = new CompoundTag();
            this.save(tag); // 1.20 signature
            return tag;
        }
    }

    /**
     * Helper to get instance, works in both versions
     */
    public static <T extends SavedData> T get(MinecraftServer server, String key, Function<CompoundTag, T> fromNbt, Supplier<T> constructor) {
        //? if >1.21 {
        /*DimensionDataStorage manager = server.getLevel(Level.OVERWORLD).getDataStorage();
        return manager.getOrCreate(factory::apply, key);
        *///?} else {
        DimensionDataStorage manager = server.getLevel(Level.OVERWORLD).getDataStorage();
        return manager.computeIfAbsent(fromNbt::apply, constructor, key);
        //?}
    }
}