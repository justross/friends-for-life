//? if neoforge {
/*package net.arcana.friendsforlife.loaders.neoforge;

import net.arcana.friendsforlife.*;
import net.arcana.friendsforlife.petdatabase.PetWatcher;
import net.arcana.friendsforlife.util.CompatConfig;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;

@Mod("friendsforlife")
public class NeoforgeEntrypoint {
    public NeoforgeEntrypoint(IEventBus modBus, ModContainer container) {
        PetWatcher.register();
        Commands.register();

        Net.register();
        TaskScheduler.register();

        container.registerConfig(ModConfig.Type.COMMON, ServerConfig.SPEC);

        final class NeoforgeConfigBacking implements CompatConfig.Impl {
            @Override
            public int petRespawnTimer() {
                return ServerConfig.RESPAWN_TIMER.get();
            }
        }

        CompatConfig.setBacking(new NeoforgeConfigBacking());

        NeoForge.EVENT_BUS.register(CommonBusHandler.class);
        modBus.register(ModBusHandler.class);
    }


}
*///?}
