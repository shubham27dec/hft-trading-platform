package com.hft.notification.integration;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@EmbeddedKafka(partitions = 1, topics = {"orders.filled", "orders.rejected"})
@DirtiesContext
public abstract class AbstractIntegrationTest {

    // WebSocket broker needs a real SimpMessagingTemplate — mock it so tests
    // don't need an active WebSocket session to verify message dispatch.
    @MockitoBean
    protected SimpMessagingTemplate simpMessagingTemplate;
}
