package com.hft.execution.aeron;

import com.hft.core.enums.OrderSide;
import com.hft.core.enums.OrderType;
import com.hft.execution.event.OrderEvent;
import com.hft.execution.handler.ExecutionHandler;
import io.aeron.Subscription;
import io.aeron.logbuffer.FragmentHandler;
import io.aeron.logbuffer.Header;
import org.agrona.DirectBuffer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.hft.execution.aeron.AeronPublishHandler.*;

public class AeronOrderSubscriber implements Runnable {

    private static final Logger log = LoggerFactory.getLogger(AeronOrderSubscriber.class);
    private static final int FRAGMENT_LIMIT = 10;

    private final Subscription subscription;
    private final ExecutionHandler executionHandler;
    private final AtomicBoolean running = new AtomicBoolean(true);

    // Pre-allocated — no GC on hot path
    private final OrderEvent reusableEvent = new OrderEvent();

    public AeronOrderSubscriber(Subscription subscription, ExecutionHandler executionHandler) {
        this.subscription = subscription;
        this.executionHandler = executionHandler;
    }

    @Override
    public void run() {
        FragmentHandler handler = this::onFragment;
        log.info("AeronOrderSubscriber polling started");
        while (running.get()) {
            int fragments = subscription.poll(handler, FRAGMENT_LIMIT);
            if (fragments == 0) {
                Thread.onSpinWait();
            }
        }
        log.info("AeronOrderSubscriber polling stopped");
    }

    public void stop() {
        running.set(false);
    }

    private void onFragment(DirectBuffer buffer, int offset, int length, Header header) {
        if (length < MSG_SIZE) {
            log.warn("Received undersized Aeron message: {} bytes", length);
            return;
        }
        decode(buffer, offset, reusableEvent);
        executionHandler.handle(reusableEvent);
        reusableEvent.reset();
    }

    private void decode(DirectBuffer buffer, int offset, OrderEvent event) {
        int sideOrdinal = buffer.getInt(offset + SIDE_OFFSET);
        event.side = (sideOrdinal >= 0 && sideOrdinal < OrderSide.values().length)
                ? OrderSide.values()[sideOrdinal] : null;

        int typeOrdinal = buffer.getInt(offset + TYPE_OFFSET);
        event.type = (typeOrdinal >= 0 && typeOrdinal < OrderType.values().length)
                ? OrderType.values()[typeOrdinal] : null;

        event.quantity = buffer.getLong(offset + QTY_OFFSET);
        event.routedAsk = buffer.getDouble(offset + ROUTED_ASK_OFFSET);
        event.routedBid = buffer.getDouble(offset + ROUTED_BID_OFFSET);
        event.riskPassed = buffer.getByte(offset + RISK_PASSED_OFFSET) == 1;
        event.orderId = getFixedString(buffer, offset + ORDER_ID_OFFSET, 36);
        event.symbol = getFixedString(buffer, offset + SYMBOL_OFFSET, 8);
        event.venue = getFixedString(buffer, offset + VENUE_OFFSET, 16);
        event.rejectionReason = getFixedString(buffer, offset + REJECTION_OFFSET, 64);
    }

    private String getFixedString(DirectBuffer buffer, int offset, int length) {
        byte[] bytes = new byte[length];
        buffer.getBytes(offset, bytes);
        String s = new String(bytes, StandardCharsets.US_ASCII).trim();
        return s.isEmpty() ? null : s;
    }
}
