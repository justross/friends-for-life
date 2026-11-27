package net.arcana.friendsforlife;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Used on both sides, so it must not reference client-only classes (see client/Keybinds).
 */
public class Init {
    public static final String MOD_ID = "friendsforlife";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
}
