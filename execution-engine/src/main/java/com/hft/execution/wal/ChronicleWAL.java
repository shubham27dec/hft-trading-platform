package com.hft.execution.wal;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hft.execution.event.OrderEvent;
import net.openhft.chronicle.queue.ExcerptAppender;
import net.openhft.chronicle.queue.impl.single.SingleChronicleQueue;
import net.openhft.chronicle.queue.impl.single.SingleChronicleQueueBuilder;
import net.openhft.chronicle.wire.DocumentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashMap;
import java.util.Map;

public class ChronicleWAL implements AutoCloseable {

    private static final Logger log = LoggerFactory.getLogger(ChronicleWAL.class);

    private final SingleChronicleQueue queue;
    private final ExcerptAppender appender;
    private final ObjectMapper mapper;

    public ChronicleWAL(String path) {
        this.queue = SingleChronicleQueueBuilder.binary(path).build();
        this.appender = queue.acquireAppender();
        this.mapper = new ObjectMapper();
    }

    public void append(String state, OrderEvent event) {
        try (DocumentContext dc = appender.writingDocument()) {
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("state", state);
            entry.put("orderId", event.orderId);
            entry.put("clientOrderId", event.clientOrderId);
            entry.put("symbol", event.symbol);
            entry.put("side", event.side != null ? event.side.name() : null);
            entry.put("quantity", event.quantity);
            entry.put("venue", event.venue);
            entry.put("fillPrice", event.fillPrice);
            entry.put("filledQty", event.filledQty);
            entry.put("rejectionReason", event.rejectionReason);
            entry.put("ts", System.currentTimeMillis());
            dc.wire().write("entry").text(mapper.writeValueAsString(entry));
        } catch (Exception e) {
            log.error("WAL write failed for order {}: {}", event.orderId, e.getMessage());
        }
    }

    @Override
    public void close() {
        queue.close();
    }
}
