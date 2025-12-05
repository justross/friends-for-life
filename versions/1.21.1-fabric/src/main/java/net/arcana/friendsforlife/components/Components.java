package net.arcana.friendsforlife.components;

import com.mojang.serialization.Codec;
import net.arcana.friendsforlife.loaders.fabric.FabricEntrypoint;
import net.arcana.friendsforlife.util.CompatResourceLocation;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import org.intellij.lang.annotations.Identifier;

public class Components {
    //? if >=1.21 {
    public static final DataComponentType<Long> HOME_POS =
            Registry.register(
                    BuiltInRegistries.DATA_COMPONENT_TYPE,
                    new CompatResourceLocation(FabricEntrypoint.MOD_ID, "home_pos").unwrap(),
                    DataComponentType.<Long>builder().persistent(Codec.LONG).build()
            );

    public static final DataComponentType<String> HOME_DIM =
            Registry.register(
                    BuiltInRegistries.DATA_COMPONENT_TYPE,
                    new CompatResourceLocation(FabricEntrypoint.MOD_ID, "home_dim").unwrap(),
                    DataComponentType.<String>builder().persistent(Codec.STRING).build()
            );

    public static final DataComponentType<String> OCARINA_UUID =
            Registry.register(
                    BuiltInRegistries.DATA_COMPONENT_TYPE,
                    new CompatResourceLocation(FabricEntrypoint.MOD_ID, "ocarina_uuid").unwrap(),
                    DataComponentType.<String>builder().persistent(Codec.STRING).build()
            );
    //? } else {
    // no-op. this is just a convenience to avoid commenting out import statements on 1.20.1
    //? }

    public static void initialize() {
        // call from mod init to ensure registration
    }
}
