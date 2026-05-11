package com.hft.orderentry.integration;

import com.hft.orderentry.client.AlpacaQuoteClient;
import com.hft.orderentry.client.AlpacaSnapshotEntry;
import com.hft.orderentry.entity.TraderAccount;
import com.hft.orderentry.repository.TraderAccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@EmbeddedKafka(partitions = 1, topics = {"orders.submitted"})
@DirtiesContext
public abstract class AbstractIntegrationTest {

    @MockitoBean
    protected JwtDecoder jwtDecoder;

    @MockitoBean
    protected StringRedisTemplate stringRedisTemplate;

    @MockitoBean
    protected TraderAccountRepository traderAccountRepository;

    @MockitoBean
    protected AlpacaQuoteClient alpacaQuoteClient;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setupBaseMocks() {
        ValueOperations<String, String> valueOps = mock(ValueOperations.class);
        lenient().when(stringRedisTemplate.opsForValue()).thenReturn(valueOps);
        lenient().when(valueOps.get(anyString())).thenReturn(null);
        lenient().when(stringRedisTemplate.keys(anyString())).thenReturn(null);

        TraderAccount testAccount = new TraderAccount();
        testAccount.setAccountId("test-account-001");
        testAccount.setUsername("testuser");
        testAccount.setApiKey("test-api-key-001");
        testAccount.setBuyingPower(100000.0);
        testAccount.setMarginLimit(50000.0);
        lenient().when(traderAccountRepository.findByApiKey("test-api-key-001"))
                .thenReturn(Optional.of(testAccount));

        lenient().when(alpacaQuoteClient.getSnapshots(anySet())).thenAnswer(inv -> {
            Set<String> syms = inv.getArgument(0);
            Map<String, AlpacaSnapshotEntry> result = new HashMap<>();
            AlpacaSnapshotEntry entry = new AlpacaSnapshotEntry();
            AlpacaSnapshotEntry.Quote quote = new AlpacaSnapshotEntry.Quote();
            quote.setAp(150.0);
            quote.setBp(150.0);
            entry.setLatestQuote(quote);
            syms.forEach(s -> result.put(s, entry));
            return result;
        });
    }
}
