//? if fabric {

package net.arcana.friendsforlife.loaders.fabric;

import net.arcana.friendsforlife.Init;
import net.arcana.friendsforlife.items.Items;
import net.arcana.friendsforlife.util.CompatResourceLocation;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroupEntries;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import static net.arcana.friendsforlife.items.Items.*;

public class CreativeItemRegistrationFabric {

    // Creates a new tab for creative mode
    public static final CreativeModeTab ITEMS = Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB,
            new CompatResourceLocation(Init.MOD_ID, "friendsforlife").unwrap(), FabricItemGroup.builder()
                    .title(Component.literal("Friends For Life"))
                    .icon(() -> new ItemStack(COLLAR)).displayItems(((displayContext, entries) -> {
                        entries.accept(COLLAR);
                        entries.accept(ROSTER);
                        entries.accept(OCARINA);
                        for (Item dyed : COLORED_OCARINAS.values()) {
                            entries.accept(dyed);
                        }
                    })).build());

    // Adds items to existing tools and utilities tab
    public static void addItemsToTab(FabricItemGroupEntries entries) {
        entries.accept(COLLAR);
        entries.accept(ROSTER);
        entries.accept(OCARINA);

        for (Item dyed : COLORED_OCARINAS.values()) {
            entries.accept(dyed);
        }
    }

    public static void register() {
        ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.TOOLS_AND_UTILITIES).register(CreativeItemRegistrationFabric::addItemsToTab);
    }
}
//?}
