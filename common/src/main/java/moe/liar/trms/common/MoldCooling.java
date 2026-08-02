package moe.liar.trms.common;

/**
 * Side-neutral cooling schedule and presentation curve for a molten Mold.
 *
 * <p>The Extension records accumulated loaded-server ticks rather than a
 * synthetic seconds counter. It advances that value by {@link #TICK_INTERVAL}
 * per scheduled update and completes a casting at {@link #TOTAL_TICKS}. Client
 * presentation derives its compact visual stage from the authoritative tick
 * value, without owning any gameplay timing.</p>
 */
public final class MoldCooling {
    /** One server-authoritative cooling update each second at the normal 20 TPS rate. */
    public static final int TICK_INTERVAL = 20;
    /** Total loaded-server time from pouring to automatic casting completion: thirty seconds. */
    public static final int TOTAL_TICKS = 30 * TICK_INTERVAL;
    /** Cooling begins visually only after the first fifteen seconds remain fully molten. */
    public static final int DIMMING_START_TICKS = 15 * TICK_INTERVAL;
    /** Compact derived presentation stages used for light, colour, and ambience. */
    public static final int VISUAL_STAGE_COUNT = 10;
    /** The light level of a newly poured mold. */
    public static final int INITIAL_LIGHT_LEVEL = 15;
    /** The final visible stage remains faintly emissive until the casting drops. */
    public static final int FINAL_LIGHT_LEVEL = 1;
    /** The final visible stage keeps enough contrast for its metal hue to remain recognizable. */
    public static final float FINAL_BRIGHTNESS = 0.35F;

    private MoldCooling() {
    }

    /** Returns whether a persisted elapsed-tick value is a valid scheduled cooling checkpoint. */
    public static boolean isValidElapsedTicks(int elapsedTicks) {
        return elapsedTicks >= 0
                && elapsedTicks < TOTAL_TICKS
                && elapsedTicks % TICK_INTERVAL == 0;
    }

    /** Rejects a non-checkpoint elapsed-tick value with a stable diagnostic for both endpoints. */
    public static int requireValidElapsedTicks(int elapsedTicks) {
        if (!isValidElapsedTicks(elapsedTicks)) {
            throw new IllegalArgumentException("TRMS cooling elapsed ticks must be 0.."
                    + (TOTAL_TICKS - TICK_INTERVAL) + " in increments of " + TICK_INTERVAL + ": " + elapsedTicks);
        }
        return elapsedTicks;
    }

    /** Maps elapsed cooling ticks to the client-visible dimming stage. */
    public static int visualStage(int elapsedTicks) {
        requireValidElapsedTicks(elapsedTicks);
        if (elapsedTicks < DIMMING_START_TICKS) {
            return 0;
        }
        int dimmingRange = TOTAL_TICKS - DIMMING_START_TICKS - TICK_INTERVAL;
        return Math.min(VISUAL_STAGE_COUNT - 1,
                1 + (elapsedTicks - DIMMING_START_TICKS) * (VISUAL_STAGE_COUNT - 2) / dimmingRange);
    }

    /** Maps a visible stage to the server block-light emission level. */
    public static int lightLevel(int stage) {
        if (stage < 0 || stage >= VISUAL_STAGE_COUNT) {
            throw new IllegalArgumentException("TRMS visual cooling stage must be 0.."
                    + (VISUAL_STAGE_COUNT - 1) + ": " + stage);
        }
        return INITIAL_LIGHT_LEVEL - stage * (INITIAL_LIGHT_LEVEL - FINAL_LIGHT_LEVEL) / (VISUAL_STAGE_COUNT - 1);
    }

    /** Maps a visible stage to a client-side multiplier for the molten material tint. */
    public static float brightness(int stage) {
        if (stage < 0 || stage >= VISUAL_STAGE_COUNT) {
            throw new IllegalArgumentException("TRMS visual cooling stage must be 0.."
                    + (VISUAL_STAGE_COUNT - 1) + ": " + stage);
        }
        return 1.0F - stage * (1.0F - FINAL_BRIGHTNESS) / (VISUAL_STAGE_COUNT - 1);
    }

    /** Maps authoritative elapsed ticks directly to the current world light level. */
    public static int lightLevelForElapsedTicks(int elapsedTicks) {
        return lightLevel(visualStage(elapsedTicks));
    }

    /** Maps authoritative elapsed ticks directly to the current molten tint and ambience multiplier. */
    public static float brightnessForElapsedTicks(int elapsedTicks) {
        return brightness(visualStage(elapsedTicks));
    }

    /** Returns whether the next twenty-tick update completes the casting rather than exposing another checkpoint. */
    public static boolean completesOnNextUpdate(int elapsedTicks) {
        return requireValidElapsedTicks(elapsedTicks) + TICK_INTERVAL >= TOTAL_TICKS;
    }

    /** Advances one persisted twenty-tick checkpoint; callers handle completion at the final update. */
    public static int advanceElapsedTicks(int elapsedTicks) {
        if (completesOnNextUpdate(elapsedTicks)) {
            throw new IllegalStateException("The final TRMS cooling update completes instead of advancing elapsed ticks");
        }
        return elapsedTicks + TICK_INTERVAL;
    }
}
