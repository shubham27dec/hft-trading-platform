package com.hft.riskdashboard.service;

import com.hft.core.enums.OrderSide;
import com.hft.core.event.OrderFilledEvent;
import com.hft.core.event.OrderRejectedEvent;
import com.hft.riskdashboard.model.RiskMetrics;
import com.hft.riskdashboard.state.RiskState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RiskServiceTest {

    @Mock private RiskState riskState;
    @InjectMocks private RiskService service;

    private RiskMetrics metrics;

    @BeforeEach
    void setUp() {
        metrics = new RiskMetrics();
        metrics.setAccountId("acc-1");
        lenient().when(riskState.getOrCreate("acc-1")).thenReturn(metrics);
        lenient().when(riskState.isHalted()).thenReturn(false);
    }

    @Test
    void processFill_incrementsFillCountAndExposure() {
        OrderFilledEvent fill = fill("acc-1", 100, 150.0);

        service.processFill(fill);

        assertEquals(1, metrics.getFillCount());
        assertEquals(15000.0, metrics.getGrossExposure(), 0.001);
        assertFalse(metrics.isHaltActive());
    }

    @Test
    void processFill_accumulatesExposureAcrossMultipleFills() {
        service.processFill(fill("acc-1", 100, 150.0));
        service.processFill(fill("acc-1", 50, 200.0));

        assertEquals(2, metrics.getFillCount());
        assertEquals(25000.0, metrics.getGrossExposure(), 0.001);
    }

    @Test
    void processRejection_incrementsRejectCount() {
        OrderRejectedEvent reject = new OrderRejectedEvent();
        reject.setAccountId("acc-1");
        reject.setOrderId("ord-1");
        reject.setReason("INSUFFICIENT_FUNDS");

        service.processRejection(reject);

        assertEquals(1, metrics.getRejectCount());
    }

    @Test
    void simulatePartition_setsHaltActiveOnNextSnapshot() {
        when(riskState.getOrCreate("acc-1")).thenReturn(metrics);
        when(riskState.isHalted()).thenReturn(true);

        RiskMetrics snapshot = service.getSnapshot("acc-1");

        assertTrue(snapshot.isHaltActive());
    }

    @Test
    void simulatePartition_delegatesToRiskState() {
        service.simulatePartition();
        verify(riskState).simulatePartition();
    }

    @Test
    void restore_clearsHalt() {
        service.restore();
        verify(riskState).restore();
    }

    private OrderFilledEvent fill(String accountId, long qty, double price) {
        OrderFilledEvent e = new OrderFilledEvent();
        e.setAccountId(accountId);
        e.setSymbol("AAPL");
        e.setSide(OrderSide.BUY);
        e.setFilledQty(qty);
        e.setFillPrice(price);
        e.setFillId("fill-" + System.nanoTime());
        return e;
    }
}
