package net.arcana.friendsforlife.items;

import net.arcana.friendsforlife.Init;
import net.arcana.friendsforlife.util.CompatResourceLocation;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;

import java.util.EnumMap;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Function;

public class Items {
    public static final Map<DyeColor, Item> COLORED_OCARINAS = new EnumMap<>(DyeColor.class);

    public static Item COLLAR;
    public static String COLLAR_ID = "collar";

    public static Item ROSTER;
    public static String ROSTER_ID = "pet_roster";

    public static Item OCARINA;
    public static String OCARINA_ID = "ocarina";

    //? if >=1.21.10 {
    /*private static <GenericItem extends Item> GenericItem register(String name, Function<Item.Properties, GenericItem> itemFactory, Item.Properties settings) {
        // Create the item key.
        ResourceKey<Item> itemKey = ResourceKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath(Init.MOD_ID, name));

        // Create the item instance.
        GenericItem item = itemFactory.apply(settings.setId(itemKey));

        // Register the item.
        Registry.register(BuiltInRegistries.ITEM, itemKey, item);

        return item;
    }
    *///?} else {

    private static Item register(BiConsumer<ResourceLocation, Item> registrar, String name, Item item) {
        registrar.accept(id(name), item);
        return item;
    }
 
    //?}

    public static void registerAll(BiConsumer<ResourceLocation, Item> registrar) {
//        for (DyeColor color : DyeColor.values()) {
//            String name = color.getName() + "_" + OCARINA_ID;
//
//            //? if >=1.21.10 {
//            Item item = Items.register(name, Item::new, new Item.Properties());
//            //? } else {
//            /*
//
//            Item item = new OcarinaItem(ItemSettings.create().stacksTo(1));
//             */
//            //? }
//            registrar.accept(id(name), item);
//            COLORED_OCARINAS.put(color, item);
//        }

        //? if >=1.21.10 {
        /*COLLAR = Items.register(COLLAR_ID, CollarItem::new, new Item.Properties());
        ROSTER = Items.register(ROSTER_ID, RosterItem::new, new Item.Properties());
        OCARINA = Items.register(OCARINA_ID, OcarinaItem::new, new Item.Properties());
        *///?} else {
       

        COLLAR = register(registrar, "collar",
                new CollarItem(ItemSettings.create().stacksTo(1)));
        ROSTER = register(registrar, "pet_roster",
                new RosterItem(ItemSettings.create().stacksTo(1)));
        OCARINA = register(registrar, "ocarina",
                new OcarinaItem(ItemSettings.create().stacksTo(1)));
        
        //?}
    }


    private static ResourceLocation id(String name) {
        return new CompatResourceLocation(Init.MOD_ID, name).unwrap();
    }
}