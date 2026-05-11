package com.hft.execution.feed;

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
import java.util.concurrent.atomic.AtomicReference;

public class FeedConnector {

    private static final Logger log = LoggerFactory.getLogger(FeedConnector.class);

    static final String WS_URL = "wss://stream.data.alpaca.markets/v2/iex";

    private final String keyId;
    private final String secretKey;
    private final Set<String> symbols;
    private final FeedHandler handler;
    private final HttpClient http;

    private final AtomicReference<WebSocket> socket = new AtomicReference<>();
    private final AtomicBoolean running = new AtomicBoolean(false);

    public FeedConnector(String keyId, String secretKey, Set<String> symbols, FeedHandler handler) {
        this(keyId, secretKey, symbols, handler, HttpClient.newHttpClient());
    }

    FeedConnector(String keyId, String secretKey, Set<String> symbols, FeedHandler handler, HttpClient http) {
        this.keyId = keyId;
        this.secretKey = secretKey;
        this.symbols = symbols;
        this.handler = handler;
        this.http = http;
    }

    public void start() {
        if (!running.compareAndSet(false, true)) {
            return;
        }
        CountDownLatch connected = new CountDownLatch(1);
        try {
            socket.set(http.newWebSocketBuilder()
                    .buildAsync(URI.create(WS_URL), new Listener(connected))
                    .get(10, TimeUnit.SECONDS));
            if (!connected.await(10, TimeUnit.SECONDS)) {
                log.warn("FeedConnector: timed out waiting for WebSocket auth handshake");
            }
            log.info("FeedConnector connected to Alpaca WebSocket");
        } catch (InterruptedException e) {
            running.set(false);
            Thread.currentThread().interrupt();
            throw new IllegalStateException("FeedConnector interrupted while connecting", e);
        } catch (Exception e) {
            running.set(false);
            log.error("FeedConnector failed to connect: {}", e.getMessage());
            throw new IllegalStateException("FeedConnector connect failed", e);
        }
    }

    public void stop() {
        running.set(false);
        if (socket.get() != null) {
            socket.get().sendClose(WebSocket.NORMAL_CLOSURE, "shutdown");
        }
    }

    public boolean isRunning() {
        return running.get();
    }

    private void sendAuth() {
        String auth = String.format("{\"action\":\"auth\",\"key\":\"%s\",\"secret\":\"%s\"}", keyId, secretKey);
        socket.get().sendText(auth, true);
    }

    private void sendSubscribe() {
        String syms = String.join("\",\"", symbols);
        String sub = String.format("{\"action\":\"subscribe\",\"quotes\":[\"%s\"]}", syms);
        socket.get().sendText(sub, true);
        log.info("FeedConnector subscribed to quotes for: {}", symbols);
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
                String text = buffer.toString();
                buffer.setLength(0);
                if (text.contains("\"connected\"")) {
                    sendAuth();
                } else if (text.contains("\"authenticated\"")) {
                    sendSubscribe();
                } else if (text.contains("\"error\"")) {
                    log.error("Alpaca WS error: {}", text);
                } else {
                    handler.handleMessage(text);
                }
            }
            ws.request(1);
            return CompletableFuture.completedFuture(null);
        }

        @Override
        public void onError(WebSocket ws, Throwable error) {
            log.error("FeedConnector WebSocket error: {}", error.getMessage());
            running.set(false);
        }

        @Override
        public CompletionStage<?> onClose(WebSocket ws, int statusCode, String reason) {
            log.info("FeedConnector WebSocket closed: {} {}", statusCode, reason);
            running.set(false);
            return CompletableFuture.completedFuture(null);
        }
    }
}
