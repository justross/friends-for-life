package net.arcana.friendsforlife.items;

import net.arcana.friendsforlife.Init;
import net.arcana.friendsforlife.util.CompatResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;

import java.util.EnumMap;
import java.util.Map;
import java.util.function.BiConsumer;

public class Items {
    public static final Map<DyeColor, Item> COLORED_OCARINAS = new EnumMap<>(DyeColor.class);

    public static Item COLLAR ;
    public static Item ROSTER;
    public static Item OCARINA;

    public static void registerAll(BiConsumer<ResourceLocation, Item> registrar) {
        for (DyeColor color : DyeColor.values()) {
            String name = color.getName() + "_ocarina";
            Item item = new OcarinaItem(ItemSettings.create().stacksTo(1));
            registrar.accept(id(name), item);
            COLORED_OCARINAS.put(color, item);
        }

        COLLAR = register(registrar, "collar",
                new CollarItem(ItemSettings.create().stacksTo(1)));
        ROSTER = register(registrar, "pet_roster",
                new RosterItem(ItemSettings.create().stacksTo(1)));
        OCARINA = register(registrar, "ocarina",
                new OcarinaItem(ItemSettings.create().stacksTo(1)));
    }

    private static Item register(BiConsumer<ResourceLocation, Item> registrar, String name, Item item) {
        registrar.accept(id(name), item);
        return item;
    }

    private static ResourceLocation id(String name) {
        return new CompatResourceLocation(Init.MOD_ID, name).unwrap();
    }
}