package moe.liar.trms.common;

/**
 * Side-neutral persistent envelope contract for a placed ceramic mold.
 *
 * <p>The values are plain strings and an integer so both runtime endpoints can
 * share them without a Minecraft, NeoForge, or Horizon dependency.</p>
 */
public final class MoldPersistence {
    public static final int FORMAT_VERSION = MoldPattern.FORMAT_VERSION;
    public static final String FORMAT_KEY = "MoldFormat";
    public static final String PATTERN_KEY = "Pattern";
    public static final String REVISION_KEY = "Revision";
    /**
     * Optional namespaced {@link MoldFillMaterial} ID for a placed mold.
     * Absence means that the mold is unfilled.
     */
    public static final String FILL_MATERIAL_KEY = "FillMaterial";

    private MoldPersistence() {
    }
}
