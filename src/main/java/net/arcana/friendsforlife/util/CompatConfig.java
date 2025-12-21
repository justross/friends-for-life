package net.arcana.friendsforlife.util;

public final class CompatConfig {

    /* ---- Public API used by common code ---- */

    public static int petRespawnTimer() {
        return backing().petRespawnTimer();
    }

    /* ---- Loader contract ---- */

    public interface Impl {
        int petRespawnTimer();
    }


    private static Impl BACKING;

    private CompatConfig() {}

    /** Called by loader-specific bootstrap */
    public static void setBacking(Impl impl) {
        BACKING = impl;
    }

    private static Impl backing() {
        if (BACKING == null) {
            throw new IllegalStateException("CompatConfig backing not set");
        }
        return BACKING;
    }
}
