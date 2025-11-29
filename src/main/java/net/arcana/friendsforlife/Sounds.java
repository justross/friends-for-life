package net.arcana.friendsforlife;

import net.arcana.friendsforlife.loaders.fabric.FabricEntrypoint;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;

public class Sounds {
    public static SoundEvent OCARINA = registerSound("ocarina");
    public static SoundEvent OCARINA_TWO = registerSound("ocarinatwo");

	private static SoundEvent registerSound(String id) {
		ResourceLocation identifier = ResourceLocation.tryBuild(FabricEntrypoint.MOD_ID, id);
		return Registry.register(BuiltInRegistries.SOUND_EVENT, identifier, SoundEvent.createVariableRangeEvent(identifier));
	}

	public static void register() {
		FabricEntrypoint.LOGGER.info("Registering " + FabricEntrypoint.MOD_ID + " Sounds");
		// Technically this method can stay empty, but some developers like to notify
		// the console, that certain parts of the mod have been successfully initialized
	}
}
