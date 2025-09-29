package com.vibe.jobs.auth.infrastructure;

import com.vibe.jobs.auth.domain.EmailAddress;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.junit.jupiter.api.Assertions.*;

class AsyncEmailSenderTest {
    private static final Logger log = LoggerFactory.getLogger(AsyncEmailSenderTest.class);

    @Test
    void testConsoleEmailSenderAsync() throws ExecutionException, InterruptedException, TimeoutException {
        // Given
        ConsoleEmailSender emailSender = new ConsoleEmailSender();
        EmailAddress email = EmailAddress.of("test@example.com");
        String code = "123456";

        // When
        long startTime = System.currentTimeMillis();
        CompletableFuture<Void> future = emailSender.sendVerificationCode(email, code);
        long apiReturnTime = System.currentTimeMillis();
        
        // Then - API should return quickly (async behavior)
        long apiDuration = apiReturnTime - startTime;
        log.info("API returned in {}ms", apiDuration);
        assertTrue(apiDuration < 100, "API should return quickly due to async execution");
        
        // Wait for async operation to complete
        assertDoesNotThrow(() -> future.get(5, TimeUnit.SECONDS));
        
        // Verify the future completed successfully
        assertTrue(future.isDone());
        assertFalse(future.isCompletedExceptionally());
    }
}