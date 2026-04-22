package com.unict.dmi.omniprice.aspect;

import com.unict.dmi.omniprice.annotation.RequiresRole;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;

/**
 * Aspetto AOP che implementa il Reference Monitor pattern.
 *
 * Pattern: Reference Monitor (via Spring AOP)
 * Intercetta OGNI invocazione di metodo annotato con @RequiresRole e verifica:
 * 1. L'utente e' autenticato
 * 2. Il ruolo dell'utente e' tra quelli autorizzati
 *
 * Se il controllo fallisce, lancia 403 Forbidden prima che il metodo venga eseguito.
 * Il Reference Monitor e' un'entita' che non puo' essere aggirata (always invoked,
 * tamper proof, verifiable).
 */
@Aspect
@Component
public class SecurityAspect {

    private static final Logger log = LoggerFactory.getLogger(SecurityAspect.class);

    /**
     * Intercetta tutti i metodi annotati con @RequiresRole.
     */
    @Around("@annotation(com.unict.dmi.omniprice.annotation.RequiresRole)")
    public Object enforceRoleAccess(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        RequiresRole requiresRole = method.getAnnotation(RequiresRole.class);
        String[] requiredRoles = requiresRole.value();

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            log.warn("Accesso negato: utente non autenticato - metodo: {}", method.getName());
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Autenticazione richiesta");
        }

        String username = authentication.getName();
        Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();

        boolean hasRole = authorities.stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(authority -> Arrays.stream(requiredRoles)
                        .anyMatch(required -> authority.equals("ROLE_" + required)));

        if (!hasRole) {
            log.warn("Accesso negato: utente '{}' non ha i ruoli richiesti {} per {}",
                    username, Arrays.toString(requiredRoles), method.getName());
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Ruolo richiesto: " + Arrays.toString(requiredRoles));
        }

        log.debug("Accesso autorizzato: utente '{}' -> {}", username, method.getName());
        return joinPoint.proceed();
    }
}
