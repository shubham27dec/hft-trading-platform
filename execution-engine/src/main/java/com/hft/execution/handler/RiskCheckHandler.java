package com.hft.execution.handler;

import com.hft.execution.dedup.BloomFilterDedup;
import com.hft.execution.dedup.SymbolWatchlist;
import com.hft.execution.event.OrderEvent;
import com.hft.execution.risk.HaltBit;
import com.lmax.disruptor.EventHandler;
import com.lmax.disruptor.RingBuffer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RiskCheckHandler implements EventHandler<OrderEvent> {

    private static final Logger log = LoggerFactory.getLogger(RiskCheckHandler.class);

    private final HaltBit haltBit;
    private final BloomFilterDedup dedup;
    private final SymbolWatchlist symbolWatchlist;
    private final RingBuffer<OrderEvent> routingBuffer;

    public RiskCheckHandler(HaltBit haltBit, BloomFilterDedup dedup,
                            SymbolWatchlist symbolWatchlist,
                            RingBuffer<OrderEvent> routingBuffer) {
        this.haltBit = haltBit;
        this.dedup = dedup;
        this.symbolWatchlist = symbolWatchlist;
        this.routingBuffer = routingBuffer;
    }

    @Override
    public void onEvent(OrderEvent event, long sequence, boolean endOfBatch) {
        if (!symbolWatchlist.isWatched(event.symbol)) {
            // Bloom Filter fast-reject: definitely not in watchlist — skip all further processing
            reject(event, "Symbol not in watchlist: " + event.symbol);
        } else if (haltBit.isHalted()) {
            reject(event, "Trading halted");
        } else if (dedup.isDuplicate(event.clientOrderId)) {
            reject(event, "Duplicate clientOrderId: " + event.clientOrderId);
        } else if (event.quantity <= 0) {
            reject(event, "Invalid quantity: " + event.quantity);
        } else {
            dedup.markSeen(event.clientOrderId);
            event.riskPassed = true;
            log.debug("Risk passed for order {} symbol={}", event.orderId, event.symbol);
        }

        // All events — passed or rejected — forward to Disruptor #3 (routing)
        publishToRouting(event);
    }

    private void publishToRouting(OrderEvent src) {
        long seq = routingBuffer.next();
        try {
            routingBuffer.get(seq).copyFrom(src);
        } finally {
            routingBuffer.publish(seq);
        }
    }

    private void reject(OrderEvent event, String reason) {
        event.riskPassed = false;
        event.rejectionReason = reason;
        log.warn("Risk rejected order {}: {}", event.orderId, reason);
    }
}
