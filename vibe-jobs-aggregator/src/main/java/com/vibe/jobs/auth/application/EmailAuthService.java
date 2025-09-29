package com.vibe.jobs.auth.application;

import com.vibe.jobs.auth.config.EmailAuthProperties;
import com.vibe.jobs.auth.domain.AuthSession;
import com.vibe.jobs.auth.domain.EmailAddress;
import com.vibe.jobs.auth.domain.LoginChallenge;
import com.vibe.jobs.auth.domain.UserAccount;
import com.vibe.jobs.auth.domain.VerificationCodeGenerator;
import com.vibe.jobs.auth.repo.AuthSessionRepository;
import com.vibe.jobs.auth.repo.LoginChallengeRepository;
import com.vibe.jobs.auth.repo.UserAccountRepository;
import com.vibe.jobs.auth.spi.EmailSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
public class EmailAuthService {
    private static final Logger log = LoggerFactory.getLogger(EmailAuthService.class);
    
    private final LoginChallengeRepository challengeRepository;
    private final UserAccountRepository userRepository;
    private final AuthSessionRepository sessionRepository;
    private final VerificationCodeGenerator codeGenerator;
    private final EmailSender emailSender;
    private final EmailAuthProperties properties;
    private final Clock clock;

    public EmailAuthService(LoginChallengeRepository challengeRepository,
                            UserAccountRepository userRepository,
                            AuthSessionRepository sessionRepository,
                            VerificationCodeGenerator codeGenerator,
                            EmailSender emailSender,
                            EmailAuthProperties properties,
                            Optional<Clock> clock) {
        this.challengeRepository = challengeRepository;
        this.userRepository = userRepository;
        this.sessionRepository = sessionRepository;
        this.codeGenerator = codeGenerator;
        this.emailSender = emailSender;
        this.properties = properties;
        this.clock = clock.orElse(Clock.systemUTC());
    }

    public ChallengeResult requestChallenge(String rawEmail) {
        EmailAddress email;
        try {
            email = EmailAddress.of(rawEmail);
        } catch (IllegalArgumentException ex) {
            throw new AuthFlowException(HttpStatus.BAD_REQUEST, "INVALID_EMAIL", ex.getMessage());
        }
        Instant now = Instant.now(clock);
        String code = codeGenerator.generate();
        String codeHash = hash(code);

        LoginChallenge challenge = challengeRepository
                .findTopByEmail_ValueOrderByCreatedAtDesc(email.value())
                .filter(existing -> !existing.isExpired(now))
                .orElse(null);

        if (challenge == null) {
            challenge = LoginChallenge.create(email, codeHash, now, properties.getChallengeTtl());
        } else {
            Duration cooldown = properties.getResendCooldown();
            Instant availableAt = challenge.getLastSentAt().plus(cooldown);
            if (availableAt.isAfter(now)) {
                long seconds = Math.max(1, Duration.between(now, availableAt).getSeconds());
                throw new AuthFlowException(HttpStatus.TOO_MANY_REQUESTS, "RESEND_TOO_SOON",
                        "Please wait " + seconds + " seconds before requesting another code.");
            }
            challenge.refreshCode(codeHash, now, properties.getChallengeTtl());
        }

        challengeRepository.save(challenge);
        
        // Start email sending asynchronously - don't wait for completion
        emailSender.sendVerificationCode(email, code)
                .exceptionally(throwable -> {
                    // Log the root cause of the email failure
                    Throwable rootCause = throwable;
                    while (rootCause.getCause() != null && rootCause.getCause() != rootCause) {
                        rootCause = rootCause.getCause();
                    }
                    log.error("Failed to send verification email to {}. Root cause: {}", 
                            email.masked(), rootCause.getMessage(), rootCause);
                    return null;
                });

        Instant resendAvailableAt = challenge.getLastSentAt().plus(properties.getResendCooldown());
        String debugCode = properties.isExposeCodeInResponse() ? code : null;
        return new ChallengeResult(
                challenge.getId(),
                challenge.getEmail().masked(),
                challenge.getExpiresAt(),
                resendAvailableAt,
                debugCode
        );
    }

    public VerificationResult verifyChallenge(UUID challengeId, String code) {
        Objects.requireNonNull(challengeId, "challengeId must not be null");
        if (code == null || !code.matches("\\d{6}")) {
            throw new AuthFlowException(HttpStatus.BAD_REQUEST, "INVALID_CODE_FORMAT", "Verification code must be 6 digits.");
        }
        LoginChallenge challenge = challengeRepository.findById(challengeId)
                .orElseThrow(() -> new AuthFlowException(HttpStatus.NOT_FOUND, "CHALLENGE_NOT_FOUND", "Login challenge not found."));

        Instant now = Instant.now(clock);
        if (challenge.isExpired(now)) {
            throw new AuthFlowException(HttpStatus.GONE, "CHALLENGE_EXPIRED", "Verification code has expired, please request a new one.");
        }

        boolean verified = challenge.verify(hash(code), now);
        if (!verified) {
            throw new AuthFlowException(HttpStatus.UNAUTHORIZED, "INVALID_CODE", "The verification code is invalid or no longer valid.");
        }

        UserAccount user = userRepository.findByEmail_Value(challenge.getEmail().value())
                .orElseGet(() -> UserAccount.create(challenge.getEmail(), now));
        user.markLogin(now);
        userRepository.save(user);

        String rawToken = generateSessionToken();
        String tokenHash = hash(rawToken);
        Instant expiresAt = now.plus(properties.getSessionTtl());

        AuthSession session = AuthSession.create(user, tokenHash, now, expiresAt);
        sessionRepository.save(session);

        return new VerificationResult(user.getId(), user.getEmail().value(), rawToken, expiresAt);
    }

    @Transactional(readOnly = true)
    public Optional<AuthenticatedUser> resolveSession(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            return Optional.empty();
        }
        String tokenHash = hash(rawToken);
        Instant now = Instant.now(clock);
        return sessionRepository.findByTokenHash(tokenHash)
                .filter(session -> session.isActive(now))
                .map(session -> new AuthenticatedUser(
                        session.getUser().getId(),
                        session.getUser().getEmail().value(),
                        session.getExpiresAt()
                ));
    }

    private String generateSessionToken() {
        return UUID.randomUUID().toString().replace("-", "") + UUID.randomUUID().toString().replace("-", "");
    }

    private String hash(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(bytes);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    public record ChallengeResult(UUID challengeId,
                                  String maskedEmail,
                                  Instant expiresAt,
                                  Instant resendAvailableAt,
                                  String debugCode) {}

    public record VerificationResult(UUID userId,
                                     String email,
                                     String sessionToken,
                                     Instant sessionExpiresAt) {}

    public record AuthenticatedUser(UUID userId,
                                    String email,
                                    Instant sessionExpiresAt) {}
}
