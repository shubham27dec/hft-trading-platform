package com.hft.orderentry.client;

import com.hft.orderentry.exception.QuoteUnavailableException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Map;
import java.util.Set;

@Slf4j
@Component
public class AlpacaQuoteClient {

    private final RestClient restClient;
    private final String keyId;
    private final String secretKey;

    public AlpacaQuoteClient(
            RestClient.Builder builder,
            @Value("${alpaca.api.key-id}") String keyId,
            @Value("${alpaca.api.secret-key}") String secretKey,
            @Value("${alpaca.data.base-url:https://data.alpaca.markets}") String baseUrl) {
        this.restClient = builder.baseUrl(baseUrl).build();
        this.keyId = keyId;
        this.secretKey = secretKey;
    }

    public Map<String, AlpacaSnapshotEntry> getSnapshots(Set<String> symbols) {
        String symbolList = String.join(",", symbols);
        try {
            Map<String, AlpacaSnapshotEntry> result = restClient.get()
                    .uri("/v2/stocks/snapshots?symbols={symbols}", symbolList)
                    .header("APCA-API-KEY-ID", keyId)
                    .header("APCA-API-SECRET-KEY", secretKey)
                    .retrieve()
                    .onStatus(HttpStatusCode::is4xxClientError, (req, resp) -> {
                        int status = resp.getStatusCode().value();
                        if (status == 401 || status == 403) {
                            log.error("Alpaca auth failure — check API key configuration. Status: {}", status);
                            throw new QuoteUnavailableException(symbolList);
                        }
                        if (status == 429) {
                            log.warn("Alpaca rate limit hit");
                            throw new QuoteUnavailableException("rate limit exceeded — please retry shortly");
                        }
                        log.warn("Alpaca returned {} for symbols: {}", status, symbolList);
                        throw new QuoteUnavailableException(symbolList);
                    })
                    .body(new ParameterizedTypeReference<>() {});

            if (result == null) {
                throw new QuoteUnavailableException(symbolList);
            }
            return result;

        } catch (QuoteUnavailableException e) {
            throw e;
        } catch (Exception e) {
            log.warn("Alpaca snapshot fetch failed for {}: {}", symbolList, e.getMessage());
            throw new QuoteUnavailableException(symbolList);
        }
    }
}
