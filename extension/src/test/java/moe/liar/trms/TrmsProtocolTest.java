package moe.liar.trms;

import static org.junit.jupiter.api.Assertions.assertEquals;

import moe.liar.trms.common.TrmsProtocol;
import org.junit.jupiter.api.Test;

class TrmsProtocolTest {
    @Test
    void declaresTheSharedNamespaceAndSupportedProtocolVersion() {
        assertEquals("trms", TrmsProtocol.NAMESPACE);
        assertEquals(1, TrmsProtocol.VERSION);
        assertEquals("trms-handshake-1", TrmsProtocol.HANDSHAKE_TRANSPORT_VERSION);
        assertEquals("trms-carving-1", TrmsProtocol.CARVING_TRANSPORT_VERSION);
    }
}
