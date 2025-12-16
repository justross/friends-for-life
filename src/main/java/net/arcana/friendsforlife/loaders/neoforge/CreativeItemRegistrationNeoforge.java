//? if neoforge {


/*package net.arcana.friendsforlife.loaders.neoforge;

import net.arcana.friendsforlife.Init;
import net.arcana.friendsforlife.items.Items;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

@Mod(value = Init.MOD_ID, dist = Dist.CLIENT)
public class CreativeItemRegistrationNeoforge {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, Init.MOD_ID);

    public static final Supplier<CreativeModeTab> ITEMS =
            CREATIVE_MODE_TABS.register(Init.MOD_ID, () ->
                    CreativeModeTab.builder()
                            .title(Component.translatable("itemgroup." + Init.MOD_ID ))
                            .icon(() -> new ItemStack(Items.COLLAR))
                            .displayItems((params, output) -> {
                                output.accept(Items.COLLAR);
                                output.accept(Items.ROSTER);
                                output.accept(Items.OCARINA);
                                for (Item dyed : Items.COLORED_OCARINAS.values()) {
                                    output.accept(dyed);
                                }
                            })
                            .build()
            );

    public CreativeItemRegistrationNeoforge(IEventBus modBus) {
        CREATIVE_MODE_TABS.register(modBus);
    }
}
*///?}
