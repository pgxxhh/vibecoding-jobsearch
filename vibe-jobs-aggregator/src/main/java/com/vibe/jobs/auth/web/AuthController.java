package com.vibe.jobs.auth.web;

import com.vibe.jobs.auth.application.EmailAuthService;
import com.vibe.jobs.auth.web.dto.SendCodeRequest;
import com.vibe.jobs.auth.web.dto.SendCodeResponse;
import com.vibe.jobs.auth.web.dto.SessionResponse;
import com.vibe.jobs.auth.web.dto.VerifyCodeRequest;
import com.vibe.jobs.auth.web.dto.VerifyCodeResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/auth")
@CrossOrigin(origins = "*")
public class AuthController {
    private final EmailAuthService authService;

    public AuthController(EmailAuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/challenges")
    @ResponseStatus(HttpStatus.CREATED)
    public SendCodeResponse createChallenge(@Valid @RequestBody SendCodeRequest request) {
        var result = authService.requestChallenge(request.email());
        return new SendCodeResponse(
                result.challengeId(),
                result.maskedEmail(),
                result.expiresAt(),
                result.resendAvailableAt(),
                result.debugCode()
        );
    }

    @PostMapping("/challenges/{challengeId}/verify")
    public VerifyCodeResponse verifyChallenge(@PathVariable UUID challengeId,
                                              @Valid @RequestBody VerifyCodeRequest request) {
        var result = authService.verifyChallenge(challengeId, request.code());
        return new VerifyCodeResponse(
                result.userId(),
                result.email(),
                result.sessionToken(),
                result.sessionExpiresAt()
        );
    }

    @GetMapping("/session")
    public ResponseEntity<SessionResponse> resolveSession(@RequestHeader(name = "X-Session-Token", required = false) String headerToken,
                                                          @RequestParam(name = "token", required = false) String tokenParam) {
        String token = headerToken;
        if (token == null || token.isBlank()) {
            token = tokenParam;
        }
        return authService.resolveSession(token)
                .map(user -> ResponseEntity.ok(new SessionResponse(user.userId(), user.email(), user.sessionExpiresAt())))
                .orElseGet(() -> ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());
    }
}
