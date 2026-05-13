package com.hft.execution.handler;

import com.hft.execution.event.OrderEvent;
import com.hft.execution.kafka.FillKafkaProducer;
import com.hft.execution.venue.ExecutionResult;
import com.hft.execution.venue.ExecutionVenue;
import com.hft.execution.wal.ChronicleWAL;
import com.lmax.disruptor.EventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ExecutionHandler implements EventHandler<OrderEvent> {

    private static final Logger log = LoggerFactory.getLogger(ExecutionHandler.class);

    private final ExecutionVenue alpaca;
    private final ExecutionVenue simulated;
    private final ChronicleWAL wal;
    private final FillKafkaProducer kafkaProducer;

    public ExecutionHandler(ExecutionVenue alpaca, ExecutionVenue simulated,
                            ChronicleWAL wal, FillKafkaProducer kafkaProducer) {
        this.alpaca = alpaca;
        this.simulated = simulated;
        this.wal = wal;
        this.kafkaProducer = kafkaProducer;
    }

    @Override
    public void onEvent(OrderEvent event, long sequence, boolean endOfBatch) {
        handle(event);
    }

    public void handle(OrderEvent event) {
        if (!event.riskPassed) {
            wal.append("REJECTED", event);
            kafkaProducer.publishRejected(event);
            return;
        }

        wal.append("EXECUTING", event);

        try {
            ExecutionResult result = "ALPACA".equals(event.venue)
                    ? alpaca.execute(event)
                    : simulated.execute(event);

            event.fillId = result.fillId();
            event.fillPrice = result.fillPrice();
            event.filledQty = result.filledQty();
            event.filledAt = System.currentTimeMillis() * 1_000;
            event.filled = true;

            wal.append("FILLED", event);
            kafkaProducer.publishFilled(event);
            log.info("Filled order {} via {} at price={} qty={}", event.orderId, event.venue, event.fillPrice, event.filledQty);

        } catch (Exception e) {
            log.error("Execution failed for order {}: {}", event.orderId, e.getMessage());
            event.riskPassed = false;
            event.rejectionReason = "Execution failed: " + e.getMessage();
            wal.append("REJECTED", event);
            kafkaProducer.publishRejected(event);
        }
    }
}
