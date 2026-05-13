package com.hft.execution.aeron;

import com.hft.execution.event.OrderEvent;
import com.lmax.disruptor.EventHandler;
import io.aeron.Publication;
import org.agrona.concurrent.UnsafeBuffer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

public class AeronPublishHandler implements EventHandler<OrderEvent> {

    private static final Logger log = LoggerFactory.getLogger(AeronPublishHandler.class);

    // Message layout (fixed-size, zero-copy, no GC)
    static final int SIDE_OFFSET          = 0;   // int   4 bytes
    static final int TYPE_OFFSET          = 4;   // int   4 bytes
    static final int QTY_OFFSET           = 8;   // long  8 bytes
    static final int ROUTED_ASK_OFFSET    = 16;  // double 8 bytes
    static final int ROUTED_BID_OFFSET    = 24;  // double 8 bytes
    static final int RISK_PASSED_OFFSET   = 32;  // byte  1 byte
    static final int ORDER_ID_OFFSET      = 33;  // 36 bytes (UUID)
    static final int SYMBOL_OFFSET        = 69;  // 8 bytes
    static final int VENUE_OFFSET         = 77;  // 16 bytes
    static final int REJECTION_OFFSET     = 93;  // 64 bytes
    static final int MSG_SIZE             = 157;

    private final Publication publication;
    private final UnsafeBuffer buffer = new UnsafeBuffer(ByteBuffer.allocateDirect(MSG_SIZE));

    public AeronPublishHandler(Publication publication) {
        this.publication = publication;
    }

    @Override
    public void onEvent(OrderEvent event, long sequence, boolean endOfBatch) {
        encode(event);
        long result = publication.offer(buffer, 0, MSG_SIZE);
        if (result < 0) {
            log.warn("Aeron publish back-pressure for order {}: result={}", event.orderId, result);
        }
    }

    private void encode(OrderEvent event) {
        buffer.putInt(SIDE_OFFSET, event.side != null ? event.side.ordinal() : -1);
        buffer.putInt(TYPE_OFFSET, event.type != null ? event.type.ordinal() : -1);
        buffer.putLong(QTY_OFFSET, event.quantity);
        buffer.putDouble(ROUTED_ASK_OFFSET, event.routedAsk);
        buffer.putDouble(ROUTED_BID_OFFSET, event.routedBid);
        buffer.putByte(RISK_PASSED_OFFSET, event.riskPassed ? (byte) 1 : (byte) 0);
        putFixedString(ORDER_ID_OFFSET, event.orderId, 36);
        putFixedString(SYMBOL_OFFSET, event.symbol, 8);
        putFixedString(VENUE_OFFSET, event.venue, 16);
        putFixedString(REJECTION_OFFSET, event.rejectionReason, 64);
    }

    private void putFixedString(int offset, String value, int length) {
        byte[] bytes = new byte[length];
        if (value != null) {
            byte[] src = value.getBytes(StandardCharsets.US_ASCII);
            System.arraycopy(src, 0, bytes, 0, Math.min(src.length, length));
        }
        buffer.putBytes(offset, bytes);
    }
}
