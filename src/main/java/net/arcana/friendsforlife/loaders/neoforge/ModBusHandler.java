//? if neoforge {

/*package net.arcana.friendsforlife.loaders.neoforge;

import net.arcana.friendsforlife.Init;
import net.arcana.friendsforlife.Sounds;
import net.arcana.friendsforlife.components.Components;
import net.arcana.friendsforlife.items.Items;
import net.minecraft.core.registries.Registries;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.registries.RegisterEvent;

// Registered on the mod bus by NeoforgeEntrypoint, on both sides. Don't also annotate it with @EventBusSubscriber:
// since NeoForge 21.9 that registers it a second time and every item, sound and component is registered twice.
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

    @SubscribeEvent
    public static void onConfigReload(ModConfigEvent.Reloading event) {
        if (event.getConfig().getType() == ModConfig.Type.SERVER) {
            // rebuild caches, re-evaluate limits, etc.
        }
    }
}
*///?}
