//? if neoforge {
/*package net.arcana.friendsforlife.loaders.neoforge;

import net.arcana.friendsforlife.util.CompatConfig;
import net.neoforged.neoforge.common.ModConfigSpec;

public class ServerConfig {
    public static final ModConfigSpec.ConfigValue<Integer> RESPAWN_TIMER;

    public static final ModConfigSpec SPEC;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();

        builder.push("general");

        RESPAWN_TIMER = builder
                .comment("Respawn time for downed pets")
                .define("petRespawnTimer", 60);

        builder.pop();

        SPEC = builder.build();

    }

    private ServerConfig() {
    }
}
*///?}
