package net.arcana.friendsforlife;

import com.mojang.logging.LogUtils;
import net.arcana.friendsforlife.util.CompatResourceLocation;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Init {
    public static final String MOD_ID = "friendsforlife";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    //? if >=1.21.10 {
    /*public static final KeyMapping.Category FRIENDS_FOR_LIFE =
            new KeyMapping.Category(new CompatResourceLocation("friendsforlife", "controls").unwrap());
    *///?} else {

    //?}

}
