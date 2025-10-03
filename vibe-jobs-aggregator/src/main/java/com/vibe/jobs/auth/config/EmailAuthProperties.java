package com.vibe.jobs.auth.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "auth.email")
public class EmailAuthProperties {
    /**
     * OTP validity duration.
     */
    private Duration challengeTtl = Duration.ofMinutes(10);

    /**
     * Minimum wait time before requesting a new code for the same email.
     */
    private Duration resendCooldown = Duration.ofSeconds(60);

    /**
     * Lifespan of the session token created after verification succeeds.
     */
    private Duration sessionTtl = Duration.ofDays(30);

    /**
     * When true, include the generated code in API responses (development only).
     */
    private boolean exposeCodeInResponse = false;

    /**
     * When true, use a fixed verification code for local testing (development only).
     */
    private boolean useFixedCode = false;

    /**
     * Fixed verification code to use when useFixedCode is true (development only).
     */
    private String fixedCode = "123456";

    /**
     * Sender email address used in outbound OTP messages.
     */
    private String fromAddress;


    public Duration getChallengeTtl() {
        return challengeTtl;
    }

    public void setChallengeTtl(Duration challengeTtl) {
        this.challengeTtl = challengeTtl;
    }

    public Duration getResendCooldown() {
        return resendCooldown;
    }

    public void setResendCooldown(Duration resendCooldown) {
        this.resendCooldown = resendCooldown;
    }

    public boolean isExposeCodeInResponse() {
        return exposeCodeInResponse;
    }

    public void setExposeCodeInResponse(boolean exposeCodeInResponse) {
        this.exposeCodeInResponse = exposeCodeInResponse;
    }

    public Duration getSessionTtl() {
        return sessionTtl;
    }

    public void setSessionTtl(Duration sessionTtl) {
        this.sessionTtl = sessionTtl;
    }

    public String getFromAddress() {
        return fromAddress;
    }

    public void setFromAddress(String fromAddress) {
        this.fromAddress = fromAddress;
    }

    public boolean isUseFixedCode() {
        return useFixedCode;
    }

    public void setUseFixedCode(boolean useFixedCode) {
        this.useFixedCode = useFixedCode;
    }

    public String getFixedCode() {
        return fixedCode;
    }

    public void setFixedCode(String fixedCode) {
        this.fixedCode = fixedCode;
    }
}
