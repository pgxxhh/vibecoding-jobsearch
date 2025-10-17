package com.vibe.jobs.jobposting.interfaces.rest.rate;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class RateLimiterService {

    private final Duration window;
    private final int limit;
    private final Map<String, RequestWindow> requests = new ConcurrentHashMap<>();

    public RateLimiterService(Duration window, int limit) {
        this.window = window;
        this.limit = limit;
    }

    public boolean tryAcquire(String key) {
        if (key == null) {
            return true;
        }
        RequestWindow windowCounter = requests.computeIfAbsent(key, k -> new RequestWindow());
        return windowCounter.tryAcquire(window, limit);
    }

    private static final class RequestWindow {
        private Instant start = Instant.MIN;
        private int count = 0;

        synchronized boolean tryAcquire(Duration window, int limit) {
            Instant now = Instant.now();
            if (start.equals(Instant.MIN) || now.isAfter(start.plus(window))) {
                start = now;
                count = 1;
                return true;
            }
            if (count < limit) {
                count++;
                return true;
            }
            return false;
        }
    }
}

