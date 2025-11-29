package net.arcana.friendsforlife.items;

import net.arcana.friendsforlife.ModInit;
import net.fabricmc.fabric.api.item.v1.FabricItemSettings;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroupEntries;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import java.util.EnumMap;
import java.util.Map;

public class Items {
    public static final Map<DyeColor, Item> COLORED_OCARINAS = new EnumMap<>(DyeColor.class);

    static {
        for (DyeColor color : DyeColor.values()) {
            String name = color.getName() + "_ocarina";

            Item item = registerItem(
                    name,
                    new OcarinaItem(new FabricItemSettings().stacksTo(1)));

            COLORED_OCARINAS.put(color, item);
        }
    }

    public static final Item COLLAR = registerItem("collar",
            new CollarItem(new FabricItemSettings().stacksTo(1)));
    public static final Item ROSTER = registerItem("pet_roster",
            new RosterItem(new FabricItemSettings().stacksTo(1)));
    public static final Item OCARINA = registerItem("ocarina",
            new OcarinaItem(new FabricItemSettings().stacksTo(1)));

    public static final CreativeModeTab ITEMS = Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB,
            new ResourceLocation(ModInit.MOD_ID, "friendsforlife"), FabricItemGroup.builder()
                    .title(Component.literal("Friends For Life"))
                    .icon(() -> new ItemStack(COLLAR)).displayItems(((displayContext, entries) -> {
                        entries.accept(COLLAR);
                        entries.accept(ROSTER);
                        entries.accept(OCARINA);
                        for (Item dyed : COLORED_OCARINAS.values()) {
                            entries.accept(dyed);
                        }
                    })).build());

    private static void addItemsToTab(FabricItemGroupEntries entries) {
        entries.accept(COLLAR);
        entries.accept(ROSTER);
        entries.accept(OCARINA);

        for (Item dyed : COLORED_OCARINAS.values()) {
            entries.accept(dyed);
        }
    }

    private static Item registerItem(String name, Item item) {
        return Registry.register(BuiltInRegistries.ITEM, new ResourceLocation(ModInit.MOD_ID, name), item);
    }

    public static void register() {
        ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.FUNCTIONAL_BLOCKS).register(Items::addItemsToTab);
    }
}
