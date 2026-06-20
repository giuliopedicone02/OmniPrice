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
     * Se true, si fida dell'header X-Forwarded-For (solo dietro un reverse proxy
     * FIDATO). Se false (default), usa l'IP reale della connessione: questo evita
     * che un client possa aggirare il rate limit falsificando X-Forwarded-For.
     */
    @Value("${omniprice.ratelimit.login.trust-forwarded-header:false}")
    private boolean trustForwardedHeader;

    /*
     * Un RateLimiter per ogni IP sorgente, in una cache con eviction:
     * expireAfterAccess evita la crescita illimitata della mappa (memory leak)
     * quando arrivano richieste da molti IP diversi nel tempo.
     */
    private final Cache<String, RateLimiter> limitersByIp = Caffeine.newBuilder()
            .expireAfterAccess(Duration.ofMinutes(10))
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
        RateLimiter limiter = limitersByIp.get(clientIp, this::createLimiter);

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
        // X-Forwarded-For e' falsificabile dal client: lo si usa solo se esplicitamente
        // configurato (cioe' quando c'e' un reverse proxy fidato che lo imposta).
        if (trustForwardedHeader) {
            String forwarded = request.getHeader("X-Forwarded-For");
            if (forwarded != null && !forwarded.isBlank()) {
                return forwarded.split(",")[0].trim();
            }
        }
        return request.getRemoteAddr();
    }
}
