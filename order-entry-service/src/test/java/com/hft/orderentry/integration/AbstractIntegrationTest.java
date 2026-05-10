package com.hft.orderentry.integration;

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

import java.util.Optional;

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

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setupBaseMocks() {
        ValueOperations<String, String> valueOps = mock(ValueOperations.class);
        lenient().when(stringRedisTemplate.opsForValue()).thenReturn(valueOps);
        lenient().when(valueOps.get(anyString())).thenReturn(null);

        TraderAccount testAccount = new TraderAccount();
        testAccount.setAccountId("test-account-001");
        testAccount.setUsername("testuser");
        testAccount.setApiKey("test-api-key-001");
        testAccount.setBuyingPower(100000.0);
        testAccount.setMarginLimit(50000.0);
        lenient().when(traderAccountRepository.findByApiKey("test-api-key-001"))
                .thenReturn(Optional.of(testAccount));
    }
}
