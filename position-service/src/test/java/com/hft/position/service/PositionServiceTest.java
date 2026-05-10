package com.hft.position.service;

import com.hft.core.enums.OrderSide;
import com.hft.core.event.OrderFilledEvent;
import com.hft.core.model.Position;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import tools.jackson.databind.ObjectMapper;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class PositionServiceTest {

    @Mock private StringRedisTemplate redis;
    @Mock private ObjectMapper objectMapper;
    @InjectMocks private PositionService service;

    private Position emptyPosition;

    @BeforeEach
    void setUp() {
        emptyPosition = new Position();
        emptyPosition.setAccountId("acc-1");
        emptyPosition.setSymbol("AAPL");
    }

    @Test
    void newLong_setsQtyAndCostBasis() {
        OrderFilledEvent fill = fill("acc-1", "AAPL", OrderSide.BUY, 100, 150.0);

        Position result = service.computeNewPosition(emptyPosition, fill);

        assertEquals(100, result.getNetQty());
        assertEquals(150.0, result.getAvgCostBasis());
        assertEquals(0.0, result.getRealizedPnL());
    }

    @Test
    void addToLong_updatesWeightedAvgCostBasis() {
        // Start with 100 shares @ $150
        emptyPosition.setNetQty(100);
        emptyPosition.setAvgCostBasis(150.0);

        // Buy 100 more @ $160 → new avg = (100*150 + 100*160) / 200 = $155
        OrderFilledEvent fill = fill("acc-1", "AAPL", OrderSide.BUY, 100, 160.0);

        Position result = service.computeNewPosition(emptyPosition, fill);

        assertEquals(200, result.getNetQty());
        assertEquals(155.0, result.getAvgCostBasis(), 0.001);
        assertEquals(0.0, result.getRealizedPnL());
    }

    @Test
    void reduceLong_realizesCorrectPnL() {
        // Long 100 shares @ $150, sell 50 @ $160 → realized = (160-150)*50 = $500
        emptyPosition.setNetQty(100);
        emptyPosition.setAvgCostBasis(150.0);
        OrderFilledEvent fill = fill("acc-1", "AAPL", OrderSide.SELL, 50, 160.0);

        Position result = service.computeNewPosition(emptyPosition, fill);

        assertEquals(50, result.getNetQty());
        assertEquals(500.0, result.getRealizedPnL(), 0.001);
        assertEquals(150.0, result.getAvgCostBasis()); // cost basis unchanged on reduction
    }

    @Test
    void newShort_setsNegativeQtyAndCostBasis() {
        // Short sell 50 @ $200
        OrderFilledEvent fill = fill("acc-1", "AAPL", OrderSide.SELL, 50, 200.0);

        Position result = service.computeNewPosition(emptyPosition, fill);

        assertEquals(-50, result.getNetQty());
        assertEquals(200.0, result.getAvgCostBasis());
        assertEquals(0.0, result.getRealizedPnL());
    }

    @Test
    void coverShort_realizesCorrectPnL() {
        // Short 100 shares @ $200, cover 100 @ $180 → profit = (200-180)*100 = $2000
        emptyPosition.setNetQty(-100);
        emptyPosition.setAvgCostBasis(200.0);
        OrderFilledEvent fill = fill("acc-1", "AAPL", OrderSide.BUY, 100, 180.0);

        Position result = service.computeNewPosition(emptyPosition, fill);

        assertEquals(0, result.getNetQty());
        assertEquals(2000.0, result.getRealizedPnL(), 0.001);
    }

    @Test
    void crossZero_closesLongAndOpensShort() {
        // Long 50 @ $150, sell 100 → close 50 (realize $500 profit at $160) + short 50 @ $160
        emptyPosition.setNetQty(50);
        emptyPosition.setAvgCostBasis(150.0);
        OrderFilledEvent fill = fill("acc-1", "AAPL", OrderSide.SELL, 100, 160.0);

        Position result = service.computeNewPosition(emptyPosition, fill);

        assertEquals(-50, result.getNetQty());
        assertEquals(500.0, result.getRealizedPnL(), 0.001);
        assertEquals(160.0, result.getAvgCostBasis());
    }

    private OrderFilledEvent fill(String accountId, String symbol, OrderSide side, long qty, double price) {
        OrderFilledEvent e = new OrderFilledEvent();
        e.setAccountId(accountId);
        e.setSymbol(symbol);
        e.setSide(side);
        e.setFilledQty(qty);
        e.setFillPrice(price);
        e.setFillId("fill-" + System.nanoTime());
        return e;
    }
}
