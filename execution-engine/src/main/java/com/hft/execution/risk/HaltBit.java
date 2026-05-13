package com.hft.execution.risk;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class HaltBit {

    private static final Logger log = LoggerFactory.getLogger(HaltBit.class);
    private static final int MAX_MISSED_BEATS = 3;

    private final AtomicBoolean halted = new AtomicBoolean(false);
    private final AtomicInteger missedBeats = new AtomicInteger(0);
    private final ScheduledExecutorService watchdog = Executors.newSingleThreadScheduledExecutor(
            r -> { Thread t = new Thread(r, "halt-watchdog"); t.setDaemon(true); return t; });

    public void startWatchdog() {
        watchdog.scheduleAtFixedRate(this::checkHeartbeat, 100, 100, TimeUnit.MILLISECONDS);
    }

    public void heartbeat() {
        missedBeats.set(0);
    }

    private void checkHeartbeat() {
        int missed = missedBeats.incrementAndGet();
        if (missed >= MAX_MISSED_BEATS && !halted.get()) {
            halted.set(true);
            log.error("Heartbeat missed {} times — trading halted", missed);
        }
    }

    public boolean isHalted() {
        return halted.get();
    }

    public void halt(String reason) {
        halted.set(true);
        log.error("Manual halt triggered: {}", reason);
    }

    public void resume() {
        missedBeats.set(0);
        halted.set(false);
        log.info("Trading resumed");
    }

    public void stop() {
        watchdog.shutdown();
    }
}
