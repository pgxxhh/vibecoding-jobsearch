package com.vibe.jobs.web.rate;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class RateLimiterServiceTest {

    @Test
    void rejectsRequestsBeyondLimitWithinWindow() {
        RateLimiterService service = new RateLimiterService(Duration.ofSeconds(60), 2);

        assertThat(service.tryAcquire("ip:1.2.3.4")).isTrue();
        assertThat(service.tryAcquire("ip:1.2.3.4")).isTrue();
        assertThat(service.tryAcquire("ip:1.2.3.4")).isFalse();
    }

    @Test
    void resetsCounterWhenWindowExpires() throws InterruptedException {
        RateLimiterService service = new RateLimiterService(Duration.ofMillis(100), 1);

        assertThat(service.tryAcquire("ip:5.6.7.8")).isTrue();
        assertThat(service.tryAcquire("ip:5.6.7.8")).isFalse();
        Thread.sleep(120);
        assertThat(service.tryAcquire("ip:5.6.7.8")).isTrue();
    }
}

