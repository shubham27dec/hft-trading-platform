package com.hft.position.service;

import com.hft.core.enums.OrderSide;
import com.hft.core.event.OrderFilledEvent;
import com.hft.core.model.Position;
import com.hft.core.model.Tick;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import tools.jackson.databind.ObjectMapper;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PositionServiceTest {

    @Mock private StringRedisTemplate redis;
    @Mock private ObjectMapper objectMapper;
    @Mock private ValueOperations<String, String> valueOps;
    @Mock private SetOperations<String, String> setOps;
    @InjectMocks private PositionService service;

    private Position emptyPosition;

    @BeforeEach
    void setUp() {
        emptyPosition = new Position();
        emptyPosition.setAccountId("acc-1");
        emptyPosition.setSymbol("AAPL");
        lenient().when(redis.opsForValue()).thenReturn(valueOps);
        lenient().when(redis.opsForSet()).thenReturn(setOps);
    }

    // ── computeNewPosition (pure math, no Redis) ──────────────────────────

    @Test
    void newLong_setsQtyAndCostBasis() {
        Position result = service.computeNewPosition(emptyPosition, fill("acc-1", "AAPL", OrderSide.BUY, 100, 150.0));

        assertEquals(100, result.getNetQty());
        assertEquals(150.0, result.getAvgCostBasis());
        assertEquals(0.0, result.getRealizedPnL());
    }

    @Test
    void addToLong_updatesWeightedAvgCostBasis() {
        emptyPosition.setNetQty(100);
        emptyPosition.setAvgCostBasis(150.0);

        Position result = service.computeNewPosition(emptyPosition, fill("acc-1", "AAPL", OrderSide.BUY, 100, 160.0));

        assertEquals(200, result.getNetQty());
        assertEquals(155.0, result.getAvgCostBasis(), 0.001);
        assertEquals(0.0, result.getRealizedPnL());
    }

    @Test
    void reduceLong_realizesCorrectPnL() {
        emptyPosition.setNetQty(100);
        emptyPosition.setAvgCostBasis(150.0);

        Position result = service.computeNewPosition(emptyPosition, fill("acc-1", "AAPL", OrderSide.SELL, 50, 160.0));

        assertEquals(50, result.getNetQty());
        assertEquals(500.0, result.getRealizedPnL(), 0.001);
        assertEquals(150.0, result.getAvgCostBasis());
    }

    @Test
    void newShort_setsNegativeQtyAndCostBasis() {
        Position result = service.computeNewPosition(emptyPosition, fill("acc-1", "AAPL", OrderSide.SELL, 50, 200.0));

        assertEquals(-50, result.getNetQty());
        assertEquals(200.0, result.getAvgCostBasis());
        assertEquals(0.0, result.getRealizedPnL());
    }

    @Test
    void coverShort_realizesCorrectPnL() {
        emptyPosition.setNetQty(-100);
        emptyPosition.setAvgCostBasis(200.0);

        Position result = service.computeNewPosition(emptyPosition, fill("acc-1", "AAPL", OrderSide.BUY, 100, 180.0));

        assertEquals(0, result.getNetQty());
        assertEquals(2000.0, result.getRealizedPnL(), 0.001);
    }

    @Test
    void crossZero_closesLongAndOpensShort() {
        emptyPosition.setNetQty(50);
        emptyPosition.setAvgCostBasis(150.0);

        Position result = service.computeNewPosition(emptyPosition, fill("acc-1", "AAPL", OrderSide.SELL, 100, 160.0));

        assertEquals(-50, result.getNetQty());
        assertEquals(500.0, result.getRealizedPnL(), 0.001);
        assertEquals(160.0, result.getAvgCostBasis());
    }

    // ── applyFill (Redis integration) ─────────────────────────────────────

    @Test
    void applyFill_newPosition_savesToRedisAndUpdatesIndex() throws Exception {
        when(valueOps.get("position:acc-1:AAPL")).thenReturn(null);

        service.applyFill(fill("acc-1", "AAPL", OrderSide.BUY, 100, 150.0));

        verify(valueOps).set(eq("position:acc-1:AAPL"), any(), eq(24L), eq(TimeUnit.HOURS));
        verify(setOps).add("account:positions:acc-1", "AAPL");
        verify(setOps).add("symbol:accounts:AAPL", "acc-1");
    }

    @Test
    void applyFill_existingPosition_loadsDeserializesAndSaves() throws Exception {
        String existingJson = "{\"netQty\":100}";
        Position existing = new Position();
        existing.setAccountId("acc-1");
        existing.setSymbol("AAPL");
        existing.setNetQty(100);
        existing.setAvgCostBasis(150.0);

        when(valueOps.get("position:acc-1:AAPL")).thenReturn(existingJson);
        when(objectMapper.readValue(existingJson, Position.class)).thenReturn(existing);

        service.applyFill(fill("acc-1", "AAPL", OrderSide.SELL, 50, 160.0));

        verify(objectMapper).readValue(existingJson, Position.class);
        verify(valueOps).set(eq("position:acc-1:AAPL"), any(), eq(24L), eq(TimeUnit.HOURS));
    }

    // ── applyTick ─────────────────────────────────────────────────────────

    @Test
    void applyTick_updatesUnrealizedPnL() throws Exception {
        String json = "{\"netQty\":100,\"avgCostBasis\":150.0}";
        Position pos = new Position();
        pos.setNetQty(100);
        pos.setAvgCostBasis(150.0);

        when(setOps.members("symbol:accounts:AAPL")).thenReturn(Set.of("acc-1"));
        when(valueOps.get("position:acc-1:AAPL")).thenReturn(json);
        when(objectMapper.readValue(json, Position.class)).thenReturn(pos);

        Tick tick = new Tick();
        tick.setSymbol("AAPL");
        tick.setLastPrice(160.0);

        service.applyTick(tick);

        assertEquals(1000.0, pos.getUnrealizedPnL(), 0.001);
        verify(valueOps).set(eq("position:acc-1:AAPL"), any(), eq(24L), eq(TimeUnit.HOURS));
    }

    @Test
    void applyTick_noAccountsForSymbol_doesNothing() {
        when(setOps.members("symbol:accounts:MSFT")).thenReturn(Collections.emptySet());

        Tick tick = new Tick();
        tick.setSymbol("MSFT");
        tick.setLastPrice(300.0);

        service.applyTick(tick);

        verify(valueOps, never()).get(any());
    }

    @Test
    void applyTick_zeroQtyPosition_skipsUpdate() throws Exception {
        String json = "{\"netQty\":0}";
        Position pos = new Position();
        pos.setNetQty(0);

        when(setOps.members("symbol:accounts:AAPL")).thenReturn(Set.of("acc-1"));
        when(valueOps.get("position:acc-1:AAPL")).thenReturn(json);
        when(objectMapper.readValue(json, Position.class)).thenReturn(pos);

        Tick tick = new Tick();
        tick.setSymbol("AAPL");
        tick.setLastPrice(160.0);

        service.applyTick(tick);

        verify(valueOps, never()).set(any(), any(), anyLong(), any());
    }

    // ── getPositions ──────────────────────────────────────────────────────

    @Test
    void getPositions_returnsNonZeroPositions() throws Exception {
        String json = "{\"netQty\":100}";
        Position pos = new Position();
        pos.setAccountId("acc-1");
        pos.setSymbol("AAPL");
        pos.setNetQty(100);

        when(setOps.members("account:positions:acc-1")).thenReturn(Set.of("AAPL"));
        when(valueOps.get("position:acc-1:AAPL")).thenReturn(json);
        when(objectMapper.readValue(json, Position.class)).thenReturn(pos);

        List<Position> result = service.getPositions("acc-1");

        assertEquals(1, result.size());
        assertEquals("AAPL", result.get(0).getSymbol());
    }

    @Test
    void getPositions_emptyAccount_returnsEmptyList() {
        when(setOps.members("account:positions:acc-empty")).thenReturn(Collections.emptySet());

        List<Position> result = service.getPositions("acc-empty");

        assertTrue(result.isEmpty());
    }

    @Test
    void getPositions_filtersOutZeroQtyPositions() throws Exception {
        String json = "{\"netQty\":0}";
        Position pos = new Position();
        pos.setNetQty(0);

        when(setOps.members("account:positions:acc-1")).thenReturn(Set.of("AAPL"));
        when(valueOps.get("position:acc-1:AAPL")).thenReturn(json);
        when(objectMapper.readValue(json, Position.class)).thenReturn(pos);

        List<Position> result = service.getPositions("acc-1");

        assertTrue(result.isEmpty());
    }

    @Test
    void applyTick_nullJsonForAccount_skipsUpdate() {
        when(setOps.members("symbol:accounts:AAPL")).thenReturn(Set.of("acc-1"));
        when(valueOps.get("position:acc-1:AAPL")).thenReturn(null);

        Tick tick = new Tick();
        tick.setSymbol("AAPL");
        tick.setLastPrice(160.0);

        service.applyTick(tick);

        verify(valueOps, never()).set(any(), any(), anyLong(), any());
    }

    @Test
    void applyFill_serializationFailure_logsAndContinues() throws Exception {
        when(valueOps.get("position:acc-1:AAPL")).thenReturn(null);
        doThrow(new RuntimeException("serialize error")).when(objectMapper).writeValueAsString(any());

        // should not throw — catch block swallows the error
        assertDoesNotThrow(() -> service.applyFill(fill("acc-1", "AAPL", OrderSide.BUY, 100, 150.0)));
    }

    @Test
    void deserialize_badJson_returnsEmptyPosition() throws Exception {
        String badJson = "{bad}";
        when(setOps.members("account:positions:acc-1")).thenReturn(Set.of("AAPL"));
        when(valueOps.get("position:acc-1:AAPL")).thenReturn(badJson);
        doThrow(new RuntimeException("parse error")).when(objectMapper).readValue(eq(badJson), eq(Position.class));

        // deserialize catch returns new Position() with netQty=0, filtered out by getPositions
        List<Position> result = service.getPositions("acc-1");

        assertTrue(result.isEmpty());
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
