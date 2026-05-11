package com.hft.execution;

import com.hft.execution.aeron.AeronOrderSubscriber;
import com.hft.execution.aeron.AeronPublishHandler;
import com.hft.execution.dedup.BloomFilterDedup;
import com.hft.execution.event.OrderEvent;
import com.hft.execution.event.OrderEventFactory;
import com.hft.execution.event.TickEvent;
import com.hft.execution.event.TickEventFactory;
import com.hft.execution.feed.FeedHandler;
import com.hft.execution.feed.PriceCache;
import com.hft.execution.handler.ExecutionHandler;
import com.hft.execution.handler.OrderBookHandler;
import com.hft.execution.handler.RiskCheckHandler;
import com.hft.execution.handler.RoutingHandler;
import com.hft.execution.kafka.FillKafkaProducer;
import com.hft.execution.kafka.OrderEventConsumer;
import com.hft.execution.risk.HaltBit;
import com.hft.execution.venue.AlpacaExecutionVenue;
import com.hft.execution.venue.SimulatedExecutionVenue;
import com.hft.execution.wal.ChronicleWAL;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.util.DaemonThreadFactory;
import io.aeron.Aeron;
import io.aeron.Publication;
import io.aeron.Subscription;
import io.aeron.driver.MediaDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class ExecutionEngineMain {

    private static final Logger log = LoggerFactory.getLogger(ExecutionEngineMain.class);
    private static final String AERON_CHANNEL = "aeron:ipc";
    private static final int AERON_STREAM_ID = 1;

    public static void main(String[] args) throws Exception {
        String bootstrapServers = System.getenv().getOrDefault("KAFKA_BOOTSTRAP_SERVERS", "localhost:9092");
        String alpacaKeyId     = System.getenv().getOrDefault("ALPACA_KEY_ID", "");
        String alpacaSecretKey = System.getenv().getOrDefault("ALPACA_SECRET_KEY", "");
        String walPath         = System.getenv().getOrDefault("WAL_PATH", "/tmp/hft-wal");
        String symbolsEnv      = System.getenv().getOrDefault("WATCHED_SYMBOLS", "AAPL,TSLA,NVDA,MSFT,AMZN");
        Set<String> watchedSymbols = new HashSet<>(Arrays.asList(symbolsEnv.split(",")));

        log.info("Starting execution engine — kafka={} walPath={}", bootstrapServers, walPath);

        // Shared state
        HaltBit haltBit = new HaltBit();
        haltBit.startWatchdog();
        BloomFilterDedup dedup = new BloomFilterDedup();
        PriceCache priceCache = new PriceCache();

        // Venues
        AlpacaExecutionVenue alpaca = new AlpacaExecutionVenue(alpacaKeyId, alpacaSecretKey);
        SimulatedExecutionVenue simulated = new SimulatedExecutionVenue(alpaca);

        // Infrastructure
        ChronicleWAL wal = new ChronicleWAL(walPath);
        FillKafkaProducer kafkaProducer = new FillKafkaProducer(bootstrapServers);

        // Aeron embedded media driver + IPC channel
        MediaDriver mediaDriver = MediaDriver.launchEmbedded();
        Aeron aeron = Aeron.connect(new Aeron.Context()
                .aeronDirectoryName(mediaDriver.aeronDirectoryName()));
        Publication publication = aeron.addPublication(AERON_CHANNEL, AERON_STREAM_ID);
        Subscription subscription = aeron.addSubscription(AERON_CHANNEL, AERON_STREAM_ID);

        // Disruptor #3 — routing pipeline (RoutingHandler → AeronPublishHandler)
        Disruptor<OrderEvent> routingDisruptor = new Disruptor<>(
                new OrderEventFactory(), 1024, DaemonThreadFactory.INSTANCE);
        routingDisruptor.handleEventsWith(new RoutingHandler(alpaca, simulated, priceCache))
                        .then(new AeronPublishHandler(publication));
        routingDisruptor.start();

        // Disruptor #2 — risk pipeline (RiskCheckHandler → publishes to D3)
        Disruptor<OrderEvent> riskDisruptor = new Disruptor<>(
                new OrderEventFactory(), 1024, DaemonThreadFactory.INSTANCE);
        riskDisruptor.handleEventsWith(
                new RiskCheckHandler(haltBit, dedup, routingDisruptor.getRingBuffer()));
        riskDisruptor.start();

        // Disruptor #1 — tick pipeline (OrderBookHandler updates PriceCache)
        Disruptor<TickEvent> tickDisruptor = new Disruptor<>(
                new TickEventFactory(), 4096, DaemonThreadFactory.INSTANCE);
        tickDisruptor.handleEventsWith(new OrderBookHandler(priceCache));
        tickDisruptor.start();

        // Feed handler: Alpaca WebSocket → Disruptor #1
        FeedHandler feedHandler = new FeedHandler(
                alpacaKeyId, alpacaSecretKey, watchedSymbols, tickDisruptor.getRingBuffer());
        feedHandler.start();
        log.info("FeedHandler started — subscribing to {} symbols", watchedSymbols.size());

        // Aeron subscriber: polls IPC channel → ExecutionHandler
        ExecutionHandler executionHandler = new ExecutionHandler(alpaca, simulated, wal, kafkaProducer);
        AeronOrderSubscriber aeronSubscriber = new AeronOrderSubscriber(subscription, executionHandler);
        Thread aeronThread = new Thread(aeronSubscriber, "aeron-execution");
        aeronThread.setDaemon(false);
        aeronThread.start();

        // Kafka consumer: orders.submitted → Disruptor #2
        Thread consumerThread = new Thread(
                new OrderEventConsumer(bootstrapServers, riskDisruptor.getRingBuffer()),
                "kafka-consumer");
        consumerThread.setDaemon(false);
        consumerThread.start();

        log.info("Execution engine started — D1(ticks/4096) D2(risk/1024) D3(routing/1024) Aeron(IPC)");

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            log.info("Shutting down execution engine");
            haltBit.halt("JVM shutdown");
            feedHandler.stop();
            aeronSubscriber.stop();
            tickDisruptor.shutdown();
            riskDisruptor.shutdown();
            routingDisruptor.shutdown();
            haltBit.stop();
            publication.close();
            subscription.close();
            aeron.close();
            mediaDriver.close();
            try { wal.close(); } catch (Exception ignored) {}
            try { kafkaProducer.close(); } catch (Exception ignored) {}
        }));

        consumerThread.join();
    }
}
