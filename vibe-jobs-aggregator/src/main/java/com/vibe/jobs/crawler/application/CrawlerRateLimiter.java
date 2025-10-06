package com.vibe.jobs.crawler.application;

import com.vibe.jobs.crawler.domain.CrawlBlueprint;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.LockSupport;

@Component
public class CrawlerRateLimiter {

    private final ConcurrentMap<String, Limiter> limiters = new ConcurrentHashMap<>();

    public Permit acquire(CrawlBlueprint blueprint) {
        Objects.requireNonNull(blueprint, "blueprint");
        Limiter limiter = limiters.computeIfAbsent(blueprint.code(), code -> new Limiter(blueprint));
        limiter.acquire();
        return limiter::release;
    }

    public interface Permit extends AutoCloseable {
        @Override
        void close();
    }

    private static final class Limiter {
        private final Semaphore concurrency;
        private final RateLimiterState rateLimiter;

        private Limiter(CrawlBlueprint blueprint) {
            int concurrencyLimit = Math.max(1, blueprint.concurrencyLimit());
            this.concurrency = new Semaphore(concurrencyLimit, true);
            this.rateLimiter = RateLimiterState.fromBlueprint(blueprint.rateLimit());
        }

        private void acquire() {
            try {
                concurrency.acquire();
                long waitNanos = rateLimiter.acquirePermit();
                if (waitNanos > 0) {
                    LockSupport.parkNanos(waitNanos);
                }
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                concurrency.release();
                throw new IllegalStateException("Interrupted while acquiring crawler permit", ex);
            }
        }

        private void release() {
            concurrency.release();
        }
    }

    private static final class RateLimiterState {
        private final double refillPerSecond;
        private final double maxTokens;
        private double tokens;
        private final AtomicReference<Long> lastRefill = new AtomicReference<>(0L);

        private RateLimiterState(double refillPerSecond, double maxTokens) {
            this.refillPerSecond = refillPerSecond;
            this.maxTokens = maxTokens;
            this.tokens = maxTokens;
        }

        static RateLimiterState fromBlueprint(CrawlBlueprint.RateLimit rateLimit) {
            if (rateLimit == null || !rateLimit.isLimited()) {
                return new RateLimiterState(0, Double.MAX_VALUE);
            }
            double refill = Math.max(0.0001d, rateLimit.requestsPerMinute() / 60.0d);
            double burst = Math.max(1, rateLimit.burst());
            return new RateLimiterState(refill, burst);
        }

        long acquirePermit() throws InterruptedException {
            if (refillPerSecond <= 0) {
                return 0L;
            }
            synchronized (this) {
                long now = System.nanoTime();
                long previous = lastRefill.get();
                if (previous == 0L) {
                    lastRefill.set(now);
                    tokens = Math.max(0, maxTokens - 1);
                    return 0L;
                }
                refillTokens(now, previous);
                if (tokens >= 1d) {
                    tokens -= 1d;
                    lastRefill.set(now);
                    return 0L;
                }
                double missing = 1d - tokens;
                double secondsToWait = missing / refillPerSecond;
                long waitNanos = (long) (secondsToWait * TimeUnit.SECONDS.toNanos(1));
                tokens = 0d;
                lastRefill.set(now + waitNanos);
                return waitNanos;
            }
        }

        private void refillTokens(long now, long previous) {
            long elapsed = Math.max(0, now - previous);
            if (elapsed <= 0) {
                return;
            }
            double add = (elapsed / (double) TimeUnit.SECONDS.toNanos(1)) * refillPerSecond;
            if (add <= 0) {
                return;
            }
            tokens = Math.min(maxTokens, tokens + add);
            lastRefill.set(now);
        }
    }
}
