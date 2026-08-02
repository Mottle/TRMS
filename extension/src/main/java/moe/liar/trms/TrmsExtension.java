package moe.liar.trms;

import moe.liar.trms.common.TrmsProtocol;
import moe.liar.horizon.extension.ExtensionContext;
import moe.liar.horizon.extension.HorizonExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Server-authoritative entrypoint for the TRMS Horizon Extension. */
public final class TrmsExtension implements HorizonExtension {
    private static final Logger LOGGER = LoggerFactory.getLogger("TRMS");

    @Override
    public void onInitialize(ExtensionContext context) {
        TrmsContent.register(context);
        TrmsProtocolHandshake.register(context);
        TrmsMoldCarvingNetwork.register(context);
        LOGGER.info("TRMS Extension initialized (protocol v{})", TrmsProtocol.VERSION);
    }
}
