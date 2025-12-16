//? if neoforge {
/*package net.arcana.friendsforlife.loaders.neoforge;

import net.arcana.friendsforlife.*;
import net.arcana.friendsforlife.petdatabase.PetWatcher;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;

@Mod("friendsforlife")
public class NeoforgeEntrypoint {
    public NeoforgeEntrypoint(IEventBus modBus) {
        PetWatcher.register();
        Commands.register();

        Net.register();
        TaskScheduler.register();

        NeoForge.EVENT_BUS.register(CommonBusHandler.class);
        modBus.register(ModBusHandler.class);
    }


}
*///?}
