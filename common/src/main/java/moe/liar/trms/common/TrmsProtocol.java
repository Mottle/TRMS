package moe.liar.trms.common;

/**
 * Version-pinned, side-neutral TRMS protocol identity.
 *
 * <p>This class intentionally has no Minecraft, NeoForge, Horizon, rendering,
 * or networking-framework dependency. Both runtime artifacts embed this exact
 * definition, while their endpoint-specific adapters own payload registration
 * and validation.</p>
 */
public final class TrmsProtocol {
    public static final String NAMESPACE = "trms";
    /**
     * Wire protocol revision. Changes to native Minecraft block states do not
     * alter this project's custom payload schema.
     */
    public static final int VERSION = 1;
    public static final String HANDSHAKE_TRANSPORT_VERSION = "trms-handshake-1";
    public static final String CARVING_TRANSPORT_VERSION = "trms-carving-1";

    private TrmsProtocol() {
    }
}
