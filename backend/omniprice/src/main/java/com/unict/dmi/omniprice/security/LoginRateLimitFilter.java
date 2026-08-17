package com.unict.dmi.omniprice.security;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;

/**
 * Filtro Rate-Limiting per l'endpoint /auth/login.
 *
 * Pattern: Authenticator (§5.3.2) — il Rate-Limiter e' il PRIMO componente
 * a processare la richiesta, prima dell'Authenticator, per bloccare attacchi
 * DoS prima che consumino risorse CPU (hash Scrypt/BCrypt e' intenzionalmente lento).
 *
 * Supporta Exponential Backoff:
 * - Base iniziale: {@code baseRefreshPeriodSeconds} (30s)
 * - Ad ogni blocco consecutivo, la durata raddoppia (30s -> 60s -> 120s -> 240s -> 300s max)
 * - Fino a un tetto massimo di {@code maxPenaltySeconds} (300s = 5 minuti).
 */
@Component
public class LoginRateLimitFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(LoginRateLimitFilter.class);
    private static final String LOGIN_PATH = "/auth/login";

    @Value("${omniprice.ratelimit.login.limit-for-period:5}")
    private int limitForPeriod;

    @Value("${omniprice.ratelimit.login.refresh-period-seconds:30}")
    private int baseRefreshPeriodSeconds;

    @Value("${omniprice.ratelimit.login.max-penalty-seconds:300}")
    private int maxPenaltySeconds;

    @Value("${omniprice.ratelimit.login.trust-forwarded-header:false}")
    private boolean trustForwardedHeader;

    /**
     * Stato del rate limiter per un dato IP.
     */
    private static class IpRateLimitState {
        RateLimiter rateLimiter;
        int consecutiveBlocks = 0;
        Instant blockExpiresAt = Instant.MIN;
        Instant lastAttempt = Instant.now();
    }

    private final Cache<String, IpRateLimitState> stateByIp = Caffeine.newBuilder()
            .expireAfterAccess(Duration.ofMinutes(15))
            .maximumSize(100_000)
            .build();

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getServletPath().equals(LOGIN_PATH)
                || !request.getMethod().equalsIgnoreCase("POST");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String clientIp = resolveClientIp(request);
        IpRateLimitState state = stateByIp.get(clientIp, k -> new IpRateLimitState());
        Instant now = Instant.now();

        synchronized (state) {
            // Se l'utente e' attualmente in cooldown/blocco attivo:
            if (now.isBefore(state.blockExpiresAt)) {
                long remainingSeconds = Duration.between(now, state.blockExpiresAt).toSeconds() + 1;
                log.warn("Tentativo di login durante cooldown per IP: {} (Mancano {}s)", clientIp, remainingSeconds);
                sendTooManyRequestsResponse(response, remainingSeconds, state.consecutiveBlocks);
                return;
            }

            // Se e' passato molto tempo dall'ultimo blocco (> 10 minuti di inattivita'), azzera la penalita' progressiva
            if (state.blockExpiresAt != Instant.MIN && now.isAfter(state.blockExpiresAt.plus(Duration.ofMinutes(10)))) {
                state.consecutiveBlocks = 0;
            }

            // Inizializza o aggiorna il RateLimiter di Resilience4J
            if (state.rateLimiter == null) {
                state.rateLimiter = createLimiter(clientIp, baseRefreshPeriodSeconds);
            }

            boolean permitted = state.rateLimiter.acquirePermission();

            if (!permitted) {
                state.consecutiveBlocks++;
                // Calcolo backoff esponenziale: 30 * 2^(blocks-1) -> 30, 60, 120, 240, 300 (max)
                long backoff = (long) baseRefreshPeriodSeconds * (1L << Math.min(state.consecutiveBlocks - 1, 10));
                int penaltySeconds = (int) Math.min((long) maxPenaltySeconds, backoff);

                state.blockExpiresAt = now.plusSeconds(penaltySeconds);
                // Crea un nuovo limiter tarato sulla nuova finestra di penalita'
                state.rateLimiter = createLimiter(clientIp, penaltySeconds);

                log.warn("Rate limit superato per IP: {} su {} (Blocco #{} per {}s)",
                        clientIp, LOGIN_PATH, state.consecutiveBlocks, penaltySeconds);

                sendTooManyRequestsResponse(response, penaltySeconds, state.consecutiveBlocks);
                return;
            }
        }

        filterChain.doFilter(request, response);

        // Se il login ha avuto successo (status 200 OK), resetta i livelli di penalità e il limiter per questo IP
        if (response.getStatus() == HttpStatus.OK.value()) {
            synchronized (state) {
                state.consecutiveBlocks = 0;
                state.blockExpiresAt = Instant.MIN;
                state.rateLimiter = createLimiter(clientIp, baseRefreshPeriodSeconds);
                log.info("Login riuscito per IP: {}. Reset dei livelli di rate limit e timer a {}s.", clientIp, baseRefreshPeriodSeconds);
            }
        }
    }

    private void sendTooManyRequestsResponse(HttpServletResponse response,
                                             long waitSeconds,
                                             int penaltyLevel) throws IOException {
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setHeader("Retry-After", String.valueOf(waitSeconds));
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write(String.format(
                "{\"error\":\"Troppi tentativi di login. Riprova tra %d secondi.\",\"retryAfterSeconds\":%d,\"penaltyLevel\":%d}",
                waitSeconds, waitSeconds, penaltyLevel
        ));
    }

    private RateLimiter createLimiter(String ip, int periodSeconds) {
        RateLimiterConfig config = RateLimiterConfig.custom()
                .limitForPeriod(limitForPeriod)
                .limitRefreshPeriod(Duration.ofSeconds(periodSeconds))
                .timeoutDuration(Duration.ZERO)
                .build();

        return RateLimiterRegistry.of(config).rateLimiter("login-" + ip + "-" + periodSeconds);
    }

    private String resolveClientIp(HttpServletRequest request) {
        if (trustForwardedHeader) {
            String forwarded = request.getHeader("X-Forwarded-For");
            if (forwarded != null && !forwarded.isBlank()) {
                return forwarded.split(",")[0].trim();
            }
        }
        return request.getRemoteAddr();
    }
}
