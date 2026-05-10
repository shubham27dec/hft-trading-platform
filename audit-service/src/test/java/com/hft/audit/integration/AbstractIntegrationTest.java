package com.hft.audit.integration;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@EmbeddedKafka(partitions = 1, topics = {"orders.filled", "orders.rejected"})
@DirtiesContext
public abstract class AbstractIntegrationTest {
    // Uses real H2 database — no mocks on the repository layer
}
