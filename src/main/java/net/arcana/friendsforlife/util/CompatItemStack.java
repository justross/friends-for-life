package net.arcana.friendsforlife.util;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.stream.Stream;

public class CompatItemStack {
    /**
     * Cross-version getTag helper.
     * Returns null if no tag in pre-1.21, otherwise always returns a tag.
     */
    @Nullable
    public static CompoundTag getTag(ItemStack stack, HolderLookup.Provider provider) {
        //? if >=1.21 {
        return (CompoundTag) stack.save(provider, new CompoundTag());
         //?} else {
        /*return stack.hasTag() ? stack.getTag() : null;
        *///?}
    }

    @Nullable
    public static CompoundTag getTag(ItemStack stack) {
        //? if >=1.21 {
        return (CompoundTag) stack.save(HolderLookup.Provider.create(Stream.empty()), new CompoundTag());
         //?} else {
        /*return stack.hasTag() ? stack.getTag() : null;
        *///?}
    }

    //? if >=1.21 {
    public static ItemStack setTag(ItemStack stack, CompoundTag tag) {
        //? if >=1.21 {
        if (stack.isEmpty()) return stack;

        Tag serialized = stack.save(HolderLookup.Provider.create(Stream.empty()), tag);

        ItemStack newStack = ItemStack.CODEC.decode(NbtOps.INSTANCE, serialized)
                .getOrThrow()
                .getFirst();

        return newStack;
    }
    //?} else {
    /*public static ItemStack setTag(ItemStack stack, CompoundTag tag) {
        stack.setTag(tag);
        return stack;
    }
    *///?}


    public static boolean hasTag(ItemStack stack) {
        //? if > 1.21 {
        return true;
         //?} else {
        /*return stack.hasTag();
        *///?}
    }

    public static CompoundTag getOrCreateTag(ItemStack stack) {
        //? if >=1.21 {
        return (CompoundTag) stack.save(HolderLookup.Provider.create(Stream.empty()), new CompoundTag());
         //?} else {
        /*return stack.getOrCreateTag();
        *///?}
    }

    public static CompoundTag save(ItemStack stack, CompoundTag tag, Level level) {
        //? if >=1.21 {
        return (CompoundTag) stack.save(level.registryAccess());
         //?} else {
        /*return stack.save(tag);
        *///?}
    }

    //? if >=1.21 {
    public static ItemStack of(CompoundTag tag) {
        return ItemStack.CODEC
                .parse(NbtOps.INSTANCE, tag)
                .result()
                .orElse(ItemStack.EMPTY);
    }
    //?} else {
    /*public static ItemStack of(CompoundTag tag) {
        return ItemStack.of(tag);
    }
    *///?}
}