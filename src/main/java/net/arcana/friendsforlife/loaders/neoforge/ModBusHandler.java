//? if neoforge {

/*package net.arcana.friendsforlife.loaders.neoforge;

import net.arcana.friendsforlife.Init;
import net.arcana.friendsforlife.Sounds;
import net.arcana.friendsforlife.components.components.Components;
import net.arcana.friendsforlife.items.Items;
import net.minecraft.core.registries.Registries;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.registries.RegisterEvent;

@EventBusSubscriber(modid = Init.MOD_ID, value = Dist.DEDICATED_SERVER)
public class ModBusHandler {

    @SubscribeEvent
    public static void onRegister(RegisterEvent event) {
        if (event.getRegistryKey().equals(Registries.ITEM)) {
            Items.registerAll((id, item) ->
                    event.register(Registries.ITEM, id, () -> item)
            );
        }
        if (event.getRegistryKey().equals(Registries.SOUND_EVENT)) {
            Sounds.registerAll((id, soundEvent) ->
                    event.register(Registries.SOUND_EVENT, id, () -> soundEvent)
            );
        }
        if (event.getRegistryKey().equals(Registries.DATA_COMPONENT_TYPE)) {
            Components.registerAll((id, type) ->
                    event.register(Registries.DATA_COMPONENT_TYPE, id, () -> type)
            );
        }
    }
}
*///?}
