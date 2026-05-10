package com.hft.orderentry.karate;

import com.intuit.karate.junit5.Karate;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.web.server.LocalServerPort;

import com.hft.orderentry.integration.AbstractIntegrationTest;

class OrderKarateRunner extends AbstractIntegrationTest {

    @LocalServerPort
    private int port;

    @Test
    Karate testOrders() {
        return Karate.run("classpath:karate/orders.feature")
                .systemProperty("baseUrl", "http://localhost:" + port);
    }
}
