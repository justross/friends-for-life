package net.arcana.friendsforlife.components.components;

import com.mojang.serialization.Codec;
import net.arcana.friendsforlife.Init;
import net.arcana.friendsforlife.util.CompatResourceLocation;
import net.minecraft.core.Registry;
//? if >=1.21
/*import net.minecraft.core.component.DataComponentType;*/
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import org.intellij.lang.annotations.Identifier;

import java.util.function.BiConsumer;

public class Components {
    //? if >=1.21 {
    /*public static DataComponentType<Long> HOME_POS;
    public static DataComponentType<String> HOME_DIM;
    public static DataComponentType<String> OCARINA_UUID;

    public static void registerAll(BiConsumer<ResourceLocation, DataComponentType<?>> registrar) {
        HOME_POS = register(
                registrar,
                "home_pos",
                DataComponentType.<Long>builder()
                        .persistent(Codec.LONG)
                        .build()
        );

        HOME_DIM = register(
                registrar,
                "home_dim",
                DataComponentType.<String>builder()
                        .persistent(Codec.STRING)
                        .build()
        );

        OCARINA_UUID = register(
                registrar,
                "ocarina_uuid",
                DataComponentType.<String>builder()
                        .persistent(Codec.STRING)
                        .build()
        );
    }

    private static <T> DataComponentType<T> register(
            BiConsumer<ResourceLocation, DataComponentType<?>> registrar,
            String name,
            DataComponentType<T> type
    ) {
        registrar.accept(id(name), type);
        return type;
    }

    private static ResourceLocation id(String name) {
        return new CompatResourceLocation(Init.MOD_ID, name).unwrap();
    }
    *///?} else {
    //?}
}
