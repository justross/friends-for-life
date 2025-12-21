package net.arcana.friendsforlife;

import net.arcana.friendsforlife.util.CompatResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;

import java.util.Objects;
import java.util.function.BiConsumer;

public class Sounds {
    public static SoundEvent OCARINA;
    public static SoundEvent OCARINA_TWO;

    private static SoundEvent createSound(String id) {
        ResourceLocation identifier = new CompatResourceLocation(Init.MOD_ID, id).unwrap();
        return SoundEvent.createVariableRangeEvent(
                Objects.requireNonNull(identifier, "Sound ResourceLocation cannot be null: " + id)
        );
    }

    public static void registerAll(BiConsumer<ResourceLocation, SoundEvent> registrar) {
        OCARINA = createSound("ocarina");
        OCARINA_TWO = createSound("ocarinatwo");

        registrar.accept(id("ocarina"), OCARINA);
        registrar.accept(id("ocarinatwo"), OCARINA_TWO);
    }

    private static ResourceLocation id(String name) {
        return new CompatResourceLocation(Init.MOD_ID, name).unwrap();
    }
}
