package com.unict.dmi.omniprice.security;

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
import java.util.concurrent.ConcurrentHashMap;

/**
 * Filtro Rate-Limiting per l'endpoint /auth/login.
 *
 * Pattern: Authenticator (§5.3.2) — il Rate-Limiter e' il PRIMO componente
 * a processare la richiesta, prima dell'Authenticator, per bloccare attacchi
 * DoS prima che consumino risorse CPU (hash Scrypt/BCrypt e' intenzionalmente lento).
 *
 * Un RateLimiter per IP sorgente: ogni IP ha al massimo
 * {@code limitForPeriod} tentativi ogni {@code refreshPeriodSeconds} secondi.
 */
@Component
public class LoginRateLimitFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(LoginRateLimitFilter.class);
    private static final String LOGIN_PATH = "/auth/login";

    @Value("${omniprice.ratelimit.login.limit-for-period:5}")
    private int limitForPeriod;

    @Value("${omniprice.ratelimit.login.refresh-period-seconds:60}")
    private int refreshPeriodSeconds;

    /*
     * Un RateLimiter per ogni IP sorgente. ConcurrentHashMap garantisce
     * thread-safety in ambienti multi-thread.
     */
    private final ConcurrentHashMap<String, RateLimiter> limitersByIp = new ConcurrentHashMap<>();

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
        RateLimiter limiter = limitersByIp.computeIfAbsent(clientIp, this::createLimiter);

        boolean permitted = limiter.acquirePermission();

        if (!permitted) {
            log.warn("Rate limit superato per IP: {} su {}", clientIp, LOGIN_PATH);
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getWriter().write(
                    "{\"error\":\"Troppi tentativi di login. Riprova tra " + refreshPeriodSeconds + " secondi.\"}"
            );
            return;
        }

        filterChain.doFilter(request, response);
    }

    private RateLimiter createLimiter(String ip) {
        RateLimiterConfig config = RateLimiterConfig.custom()
                .limitForPeriod(limitForPeriod)
                .limitRefreshPeriod(Duration.ofSeconds(refreshPeriodSeconds))
                .timeoutDuration(Duration.ZERO)
                .build();

        return RateLimiterRegistry.of(config).rateLimiter("login-" + ip);
    }

    private String resolveClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
