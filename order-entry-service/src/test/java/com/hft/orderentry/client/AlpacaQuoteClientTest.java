package com.hft.orderentry.client;

import com.hft.orderentry.exception.QuoteUnavailableException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.web.client.RestClient;

import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AlpacaQuoteClientTest {

    @Mock private RestClient.Builder builder;
    @Mock private RestClient restClient;
    @Mock private RestClient.RequestHeadersUriSpec<?> uriSpec;
    @Mock private RestClient.RequestHeadersSpec<?> headersSpec;
    @Mock private RestClient.ResponseSpec responseSpec;

    private AlpacaQuoteClient client;

    @BeforeEach
    void setUp() {
        when(builder.baseUrl(anyString())).thenReturn(builder);
        when(builder.build()).thenReturn(restClient);
        client = new AlpacaQuoteClient(builder, "test-key", "test-secret", "https://data.alpaca.markets");
    }

    @Test
    void getSnapshots_successfulResponse_returnsSnapshotMap() {
        AlpacaSnapshotEntry entry = buildEntry(150.10, 149.90);
        stubRestClient(Map.of("AAPL", entry));

        Map<String, AlpacaSnapshotEntry> result = client.getSnapshots(Set.of("AAPL"));

        assertEquals(1, result.size());
        assertNotNull(result.get("AAPL"));
        assertEquals(150.10, result.get("AAPL").getLatestQuote().getAp(), 0.001);
        assertEquals(149.90, result.get("AAPL").getLatestQuote().getBp(), 0.001);
    }

    @Test
    void getSnapshots_nullResponse_throwsQuoteUnavailableException() {
        stubRestClient(null);

        assertThrows(QuoteUnavailableException.class,
                () -> client.getSnapshots(Set.of("AAPL")));
    }

    @Test
    void getSnapshots_httpCallThrows_throwsQuoteUnavailableException() {
        when(restClient.get()).thenThrow(new RuntimeException("connection refused"));

        assertThrows(QuoteUnavailableException.class,
                () -> client.getSnapshots(Set.of("AAPL")));
    }

    @Test
    void getSnapshots_multipleSymbols_returnsAllEntries() {
        Map<String, AlpacaSnapshotEntry> response = Map.of(
                "AAPL", buildEntry(150.10, 149.90),
                "TSLA", buildEntry(200.20, 199.80)
        );
        stubRestClient(response);

        Map<String, AlpacaSnapshotEntry> result = client.getSnapshots(Set.of("AAPL", "TSLA"));

        assertEquals(2, result.size());
    }

    private AlpacaSnapshotEntry buildEntry(double ask, double bid) {
        AlpacaSnapshotEntry entry = new AlpacaSnapshotEntry();
        AlpacaSnapshotEntry.Quote quote = new AlpacaSnapshotEntry.Quote();
        quote.setAp(ask);
        quote.setBp(bid);
        entry.setLatestQuote(quote);
        return entry;
    }

    @SuppressWarnings("unchecked")
    private void stubRestClient(Map<String, AlpacaSnapshotEntry> response) {
        doReturn(uriSpec).when(restClient).get();
        doReturn(headersSpec).when(uriSpec).uri(anyString(), anyString());
        doReturn(headersSpec).when(headersSpec).header(anyString(), anyString());
        doReturn(responseSpec).when(headersSpec).retrieve();
        doReturn(responseSpec).when(responseSpec).onStatus(any(), any());
        doReturn(response).when(responseSpec).body(any(ParameterizedTypeReference.class));
    }
}
