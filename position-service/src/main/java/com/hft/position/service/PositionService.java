package com.hft.position.service;

import com.hft.core.enums.OrderSide;
import com.hft.core.event.OrderFilledEvent;
import com.hft.core.model.Position;
import com.hft.core.model.Tick;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class PositionService {

    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;

    private static final String POS_KEY         = "position:%s:%s";
    private static final String ACCOUNT_POS_KEY = "account:positions:%s";
    private static final String SYMBOL_ACC_KEY  = "symbol:accounts:%s";

    public void applyFill(OrderFilledEvent fill) {
        String key = posKey(fill.getAccountId(), fill.getSymbol());
        Position position = load(fill.getAccountId(), fill.getSymbol());
        position = computeNewPosition(position, fill);
        save(key, position);
        redis.opsForSet().add(String.format(ACCOUNT_POS_KEY, fill.getAccountId()), fill.getSymbol());
        redis.opsForSet().add(String.format(SYMBOL_ACC_KEY, fill.getSymbol()), fill.getAccountId());
        log.info("Position updated: account={} symbol={} netQty={} realizedPnL={}",
                fill.getAccountId(), fill.getSymbol(), position.getNetQty(), position.getRealizedPnL());
    }

    public void applyTick(Tick tick) {
        Set<String> accounts = redis.opsForSet().members(String.format(SYMBOL_ACC_KEY, tick.getSymbol()));
        if (accounts == null || accounts.isEmpty()) return;
        for (String accountId : accounts) {
            String key = posKey(accountId, tick.getSymbol());
            String json = redis.opsForValue().get(key);
            if (json == null) continue;
            Position pos = deserialize(json);
            if (pos.getNetQty() == 0) continue;
            // unrealizedPnL = (currentPrice - avgCost) * netQty
            // Works for shorts too: negative netQty flips the sign correctly
            pos.setUnrealizedPnL((tick.getLastPrice() - pos.getAvgCostBasis()) * pos.getNetQty());
            save(key, pos);
        }
    }

    public List<Position> getPositions(String accountId) {
        Set<String> symbols = redis.opsForSet().members(String.format(ACCOUNT_POS_KEY, accountId));
        if (symbols == null || symbols.isEmpty()) return Collections.emptyList();
        return symbols.stream()
                .map(symbol -> redis.opsForValue().get(posKey(accountId, symbol)))
                .filter(Objects::nonNull)
                .map(this::deserialize)
                .filter(p -> p.getNetQty() != 0)
                .toList();
    }

    // Package-private for unit testing
    Position computeNewPosition(Position pos, OrderFilledEvent fill) {
        // Signed quantity: positive = long, negative = short
        long delta = fill.getSide() == OrderSide.BUY ? fill.getFilledQty() : -fill.getFilledQty();
        long oldQty = pos.getNetQty();
        long newQty = oldQty + delta;

        if (oldQty == 0) {
            // New position — cost basis is simply the fill price
            pos.setAvgCostBasis(fill.getFillPrice());
        } else if (Long.signum(oldQty) == Long.signum(newQty) || newQty == 0) {
            if (Math.abs(newQty) > Math.abs(oldQty)) {
                // Adding to existing position — weighted average cost basis
                pos.setAvgCostBasis(
                        (Math.abs(oldQty) * pos.getAvgCostBasis() + fill.getFilledQty() * fill.getFillPrice())
                        / (Math.abs(oldQty) + fill.getFilledQty()));
            } else {
                // Reducing position — realize P&L on the closed portion
                double pnlPerUnit = oldQty > 0
                        ? fill.getFillPrice() - pos.getAvgCostBasis()   // long: sell high, buy low
                        : pos.getAvgCostBasis() - fill.getFillPrice();  // short: buy back lower
                pos.setRealizedPnL(pos.getRealizedPnL() + pnlPerUnit * fill.getFilledQty());
            }
        } else {
            // Crossing zero — close entire old position at fill price, open new side
            long closingQty = Math.abs(oldQty);
            double pnlPerUnit = oldQty > 0
                    ? fill.getFillPrice() - pos.getAvgCostBasis()
                    : pos.getAvgCostBasis() - fill.getFillPrice();
            pos.setRealizedPnL(pos.getRealizedPnL() + pnlPerUnit * closingQty);
            pos.setAvgCostBasis(fill.getFillPrice());
        }

        pos.setNetQty(newQty);
        pos.setLastUpdatedAt(System.currentTimeMillis() * 1_000);
        return pos;
    }

    private Position load(String accountId, String symbol) {
        String json = redis.opsForValue().get(posKey(accountId, symbol));
        if (json == null) {
            Position p = new Position();
            p.setAccountId(accountId);
            p.setSymbol(symbol);
            return p;
        }
        return deserialize(json);
    }

    private void save(String key, Position position) {
        try {
            redis.opsForValue().set(key, objectMapper.writeValueAsString(position));
        } catch (Exception e) {
            log.error("Failed to save position {}: {}", key, e.getMessage());
        }
    }

    private Position deserialize(String json) {
        try {
            return objectMapper.readValue(json, Position.class);
        } catch (Exception e) {
            log.error("Failed to deserialize position: {}", e.getMessage());
            return new Position();
        }
    }

    private String posKey(String accountId, String symbol) {
        return String.format(POS_KEY, accountId, symbol);
    }
}
