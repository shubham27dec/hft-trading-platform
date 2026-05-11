package com.hft.orderentry.karate;

import com.intuit.karate.Results;
import com.intuit.karate.junit5.Karate;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.web.server.LocalServerPort;

import com.hft.orderentry.integration.AbstractIntegrationTest;

import static org.junit.jupiter.api.Assertions.assertEquals;

class OrderKarateRunner extends AbstractIntegrationTest {

    @LocalServerPort
    private int port;

    @Test
    void testOrders() {
        Results results = Karate.run("classpath:karate/orders.feature")
                .systemProperty("baseUrl", "http://localhost:" + port)
                .parallel(1);
        assertEquals(0, results.getFailCount(), results.getErrorMessages());
    }
}
