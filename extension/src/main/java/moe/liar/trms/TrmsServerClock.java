package moe.liar.trms;

import java.util.concurrent.atomic.AtomicLong;
import moe.liar.horizon.extension.ExtensionContext;
import moe.liar.horizon.extension.event.ServerTickEvent;

/** Process-local monotonic clock shared by region-owned packet handlers. */
final class TrmsServerClock {
    private static final AtomicLong CURRENT_TICK = new AtomicLong();

    private TrmsServerClock() {
    }

    static void register(ExtensionContext context) {
        context.events().listen(ServerTickEvent.class,
                event -> update(event.tickCount()));
    }

    static long update(int tickCount) {
        long tick = Integer.toUnsignedLong(tickCount);
        CURRENT_TICK.set(tick);
        return tick;
    }

    static long currentTick() {
        return CURRENT_TICK.get();
    }
}
