package moe.liar.trms.client;

import moe.liar.trms.common.TrmsProtocol;
import net.neoforged.neoforge.client.network.event.RegisterClientPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

/** Strict REQUIRED configuration-channel registration for the immutable TRMS protocol. */
public final class TrmsHandshake {
    public static final String TRANSPORT_VERSION = TrmsProtocol.HANDSHAKE_TRANSPORT_VERSION;

    private TrmsHandshake() {}

    public static void registerPayloads(RegisterPayloadHandlersEvent event) {
        var registrar = event.registrar(TRANSPORT_VERSION);
        // The registrar defaults to required payloads; no optional() call is made.
        registrar.configurationToClient(ProtocolChallenge.TYPE, ProtocolChallenge.STREAM_CODEC);
        // NetworkRegistry requires every serverbound registration to provide a server handler,
        // even when this client-only mod will only ever send this payload.  The Horizon
        // Extension owns the real server handler.  If this local client handler could ever be
        // invoked, packet direction has been violated and the connection must not continue.
        registrar.configurationToServer(
                ProtocolResponse.TYPE,
                ProtocolResponse.STREAM_CODEC,
                TrmsHandshake::rejectUnexpectedClientSideResponse
        );
        // This client-only mod sends the play payload while the paired Horizon
        // Extension owns the actual server handler.
        event.registrar(TrmsProtocol.CARVING_TRANSPORT_VERSION).playToServer(
                CarveMoldPayload.TYPE,
                CarveMoldPayload.STREAM_CODEC,
                TrmsHandshake::rejectUnexpectedClientSideCarve
        );
    }

    public static void registerClientHandlers(RegisterClientPayloadHandlersEvent event) {
        event.register(ProtocolChallenge.TYPE, (challenge, context) -> {
            // Deliberately send our own version even when the server's expected value differs.
            // The server then owns the explicit mismatch rejection and reason.
            // During configuration there is no ClientPacketListener yet, so
            // ClientPacketDistributor cannot be used.  Reply through the active
            // configuration listener to preserve the phase and connection.
            context.reply(
                    new ProtocolResponse(challenge.nonce(), TrmsProtocol.VERSION)
            );
        });
    }

    private static void rejectUnexpectedClientSideResponse(
            ProtocolResponse response,
            IPayloadContext context
    ) {
        throw new IllegalStateException(
                "TRMS protocol_response is serverbound and must never be handled by the client."
        );
    }

    private static void rejectUnexpectedClientSideCarve(
            CarveMoldPayload payload,
            IPayloadContext context
    ) {
        throw new IllegalStateException(
                "TRMS carve_mold is serverbound and must never be handled by the client."
        );
    }
}
