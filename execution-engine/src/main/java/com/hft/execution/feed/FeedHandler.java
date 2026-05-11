package com.hft.execution.feed;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hft.execution.event.TickEvent;
import com.lmax.disruptor.RingBuffer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class FeedHandler {

    private static final Logger log = LoggerFactory.getLogger(FeedHandler.class);

    static final String WS_URL = "wss://stream.data.alpaca.markets/v2/iex";

    private final String keyId;
    private final String secretKey;
    private final Set<String> symbols;
    private final RingBuffer<TickEvent> tickBuffer;
    private final ObjectMapper mapper;
    private final HttpClient http;

    private volatile WebSocket socket;
    private final AtomicBoolean running = new AtomicBoolean(false);

    public FeedHandler(String keyId, String secretKey, Set<String> symbols,
                       RingBuffer<TickEvent> tickBuffer) {
        this(keyId, secretKey, symbols, tickBuffer, HttpClient.newHttpClient(), new ObjectMapper());
    }

    FeedHandler(String keyId, String secretKey, Set<String> symbols,
                RingBuffer<TickEvent> tickBuffer, HttpClient http, ObjectMapper mapper) {
        this.keyId = keyId;
        this.secretKey = secretKey;
        this.symbols = symbols;
        this.tickBuffer = tickBuffer;
        this.http = http;
        this.mapper = mapper;
    }

    public void start() {
        if (!running.compareAndSet(false, true)) {
            return;
        }
        CountDownLatch connected = new CountDownLatch(1);
        try {
            socket = http.newWebSocketBuilder()
                    .buildAsync(URI.create(WS_URL), new Listener(connected))
                    .get(10, TimeUnit.SECONDS);
            connected.await(10, TimeUnit.SECONDS);
            log.info("FeedHandler connected to Alpaca WebSocket");
        } catch (InterruptedException e) {
            running.set(false);
            Thread.currentThread().interrupt();
            throw new IllegalStateException("FeedHandler interrupted while connecting", e);
        } catch (Exception e) {
            running.set(false);
            log.error("FeedHandler failed to connect: {}", e.getMessage());
            throw new IllegalStateException("FeedHandler connect failed", e);
        }
    }

    public void stop() {
        running.set(false);
        if (socket != null) {
            socket.sendClose(WebSocket.NORMAL_CLOSURE, "shutdown");
        }
    }

    public boolean isRunning() {
        return running.get();
    }

    void handleMessage(String text) {
        try {
            JsonNode root = mapper.readTree(text);
            if (!root.isArray()) return;
            for (JsonNode node : root) {
                String type = node.path("T").asText();
                if ("q".equals(type)) {
                    publishTick(node);
                } else if ("connected".equals(type)) {
                    sendAuth();
                } else if ("authenticated".equals(type)) {
                    sendSubscribe();
                } else if ("error".equals(type)) {
                    if (log.isErrorEnabled()) {
                        log.error("Alpaca WS error: {}", node.path("msg").asText());
                    }
                }
            }
        } catch (Exception e) {
            log.warn("FeedHandler message parse error: {}", e.getMessage());
        }
    }

    private void publishTick(JsonNode node) {
        double ask = node.path("ap").asDouble();
        double bid = node.path("bp").asDouble();
        if (ask <= 0 && bid <= 0) return;

        String symbol = node.path("S").asText();
        long seq = tickBuffer.next();
        try {
            TickEvent event = tickBuffer.get(seq);
            event.symbol = symbol;
            event.ask = ask;
            event.bid = bid;
            event.last = node.path("lp").asDouble();
            event.volume = node.path("ls").asLong();
            event.timestamp = System.nanoTime();
        } finally {
            tickBuffer.publish(seq);
        }
    }

    private void sendAuth() {
        String auth = String.format("{\"action\":\"auth\",\"key\":\"%s\",\"secret\":\"%s\"}", keyId, secretKey);
        socket.sendText(auth, true);
    }

    private void sendSubscribe() {
        String syms = String.join("\",\"", symbols);
        String sub = String.format("{\"action\":\"subscribe\",\"quotes\":[\"%s\"]}", syms);
        socket.sendText(sub, true);
        log.info("FeedHandler subscribed to quotes for: {}", symbols);
    }

    private class Listener implements WebSocket.Listener {

        private final CountDownLatch connected;
        private final StringBuilder buffer = new StringBuilder();

        Listener(CountDownLatch connected) {
            this.connected = connected;
        }

        @Override
        public void onOpen(WebSocket ws) {
            connected.countDown();
            ws.request(1);
        }

        @Override
        public CompletionStage<?> onText(WebSocket ws, CharSequence data, boolean last) {
            buffer.append(data);
            if (last) {
                handleMessage(buffer.toString());
                buffer.setLength(0);
            }
            ws.request(1);
            return CompletableFuture.completedFuture(null);
        }

        @Override
        public void onError(WebSocket ws, Throwable error) {
            log.error("FeedHandler WebSocket error: {}", error.getMessage());
            running.set(false);
        }

        @Override
        public CompletionStage<?> onClose(WebSocket ws, int statusCode, String reason) {
            log.info("FeedHandler WebSocket closed: {} {}", statusCode, reason);
            running.set(false);
            return CompletableFuture.completedFuture(null);
        }
    }
}
