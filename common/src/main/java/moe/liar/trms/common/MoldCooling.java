package moe.liar.trms.common;

/**
 * Side-neutral cooling schedule and presentation curve for a molten Mold.
 *
 * <p>The Extension advances a filled Mold once every {@link #TICK_INTERVAL}
 * server ticks.  The ten visible stages therefore occupy exactly
 * {@link #TOTAL_TICKS} ticks before the casting is produced.  Client code may
 * use the same stage for colour and ambience without owning any gameplay
 * timing.</p>
 */
public final class MoldCooling {
    /** One server-authoritative cooling update each second at the normal 20 TPS rate. */
    public static final int TICK_INTERVAL = 20;
    /** Number of visible molten stages before a casting completes. */
    public static final int STAGE_COUNT = 10;
    /** Total loaded-server time from pouring to automatic casting completion. */
    public static final int TOTAL_TICKS = TICK_INTERVAL * STAGE_COUNT;
    /** The light level of a newly poured mold. */
    public static final int INITIAL_LIGHT_LEVEL = 15;
    /** The final visible stage remains faintly emissive until the casting drops. */
    public static final int FINAL_LIGHT_LEVEL = 1;
    /** The final visible stage keeps enough contrast for its metal hue to remain recognizable. */
    public static final float FINAL_BRIGHTNESS = 0.35F;

    private MoldCooling() {
    }

    /** Returns whether a persisted or synchronized visible stage is in range. */
    public static boolean isValidStage(int stage) {
        return stage >= 0 && stage < STAGE_COUNT;
    }

    /** Rejects an invalid visible stage with a stable diagnostic for both endpoints. */
    public static int requireValidStage(int stage) {
        if (!isValidStage(stage)) {
            throw new IllegalArgumentException("TRMS cooling stage must be 0.." + (STAGE_COUNT - 1) + ": " + stage);
        }
        return stage;
    }

    /** Maps a visible stage to the server block-light emission level. */
    public static int lightLevel(int stage) {
        requireValidStage(stage);
        return INITIAL_LIGHT_LEVEL - stage * (INITIAL_LIGHT_LEVEL - FINAL_LIGHT_LEVEL) / (STAGE_COUNT - 1);
    }

    /** Maps a visible stage to a client-side multiplier for the molten material tint. */
    public static float brightness(int stage) {
        requireValidStage(stage);
        return 1.0F - stage * (1.0F - FINAL_BRIGHTNESS) / (STAGE_COUNT - 1);
    }

    /** Returns whether the next scheduled update completes the casting rather than exposing another stage. */
    public static boolean completesOnNextUpdate(int stage) {
        return requireValidStage(stage) == STAGE_COUNT - 1;
    }

    /** Advances one visible stage; callers must handle completion separately at the final stage. */
    public static int advanceStage(int stage) {
        if (completesOnNextUpdate(stage)) {
            throw new IllegalStateException("The final TRMS cooling stage completes instead of advancing");
        }
        return stage + 1;
    }
}
