package com.hft.execution.venue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hft.execution.event.OrderEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.UUID;

public class AlpacaExecutionVenue implements ExecutionVenue {

    private static final Logger log = LoggerFactory.getLogger(AlpacaExecutionVenue.class);

    static final String DATA_URL = "https://data.alpaca.markets";
    static final String PAPER_URL = "https://paper-api.alpaca.markets";
    static final String KEY_HEADER = "APCA-API-KEY-ID";
    static final String SECRET_HEADER = "APCA-API-SECRET-KEY";

    private final String keyId;
    private final String secretKey;
    private final HttpClient http;
    private final ObjectMapper mapper;

    public AlpacaExecutionVenue(String keyId, String secretKey) {
        this(keyId, secretKey, HttpClient.newHttpClient(), new ObjectMapper());
    }

    AlpacaExecutionVenue(String keyId, String secretKey, HttpClient http, ObjectMapper mapper) {
        this.keyId = keyId;
        this.secretKey = secretKey;
        this.http = http;
        this.mapper = mapper;
    }

    @Override
    public String name() {
        return "ALPACA";
    }

    @Override
    public VenueQuote getQuote(String symbol) {
        try {
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(DATA_URL + "/v2/stocks/snapshots?symbols=" + symbol))
                    .header(KEY_HEADER, keyId)
                    .header(SECRET_HEADER, secretKey)
                    .GET()
                    .build();
            HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
            JsonNode root = mapper.readTree(resp.body());
            JsonNode quote = root.path(symbol).path("latestQuote");
            double ask = quote.path("ap").asDouble();
            double bid = quote.path("bp").asDouble();
            return new VenueQuote(ask, bid);
        } catch (Exception e) {
            log.warn("Alpaca quote fetch failed for {}: {}", symbol, e.getMessage());
            return new VenueQuote(0, 0);
        }
    }

    @Override
    public ExecutionResult execute(OrderEvent event) {
        try {
            String side = event.side.name().toLowerCase();
            String type = event.type.name().toLowerCase();
            String body = String.format(
                    "{\"symbol\":\"%s\",\"qty\":\"%d\",\"side\":\"%s\",\"type\":\"%s\",\"time_in_force\":\"day\"}",
                    event.symbol, event.quantity, side, type);

            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(PAPER_URL + "/v2/orders"))
                    .header(KEY_HEADER, keyId)
                    .header(SECRET_HEADER, secretKey)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();

            HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
            JsonNode node = mapper.readTree(resp.body());

            String fillId = node.path("id").asText(UUID.randomUUID().toString());
            double fillPrice = node.path("filled_avg_price").asDouble(event.routedAsk);
            long filledQty = node.path("filled_qty").asLong(event.quantity);
            return new ExecutionResult(fillId, fillPrice, filledQty);
        } catch (Exception e) {
            log.error("Alpaca order execution failed for {}: {}", event.orderId, e.getMessage());
            throw new RuntimeException("Alpaca execution failed: " + e.getMessage(), e);
        }
    }
}
