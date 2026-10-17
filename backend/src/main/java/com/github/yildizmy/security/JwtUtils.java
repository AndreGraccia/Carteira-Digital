package com.github.yildizmy.security;

import com.github.yildizmy.config.MessageSourceConfig;
import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.util.Date;
import javax.crypto.SecretKey;

import static com.github.yildizmy.common.MessageKeys.*;

/**
 * Utility class for Jwt related tasks.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtUtils {

    private final MessageSourceConfig messageConfig;

    @Value("${app.security.jwtSecret}")
    private String jwtSecret;

    @Value("${app.security.jwtExpirationMs}")
    private long jwtExpirationMs;

    /** Fails at startup instead of on the first login when jwt_secret is missing or malformed. */
    @jakarta.annotation.PostConstruct
    void validateSecret() {
        signingKey();
    }

    private SecretKey signingKey() {
        try {
            return Keys.hmacShaKeyFor(Decoders.BASE64.decode(jwtSecret));
        } catch (RuntimeException exception) {
            throw new IllegalStateException("jwt_secret must be a Base64 value with at least 256 bits", exception);
        }
    }

    /** Only tokens this API issued, for this web client, are accepted. */
    static final String ISSUER = "wallia-api";
    static final String AUDIENCE = "wallia-web";

    public String generateJwtToken(Authentication authentication) {
        final UserDetailsImpl userPrincipal = (UserDetailsImpl) authentication.getPrincipal();
        return Jwts.builder()
                .subject(userPrincipal.getUsername())
                .issuer(ISSUER)
                .audience().add(AUDIENCE).and()
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + jwtExpirationMs))
                .signWith(signingKey())
                .compact();
    }

    private io.jsonwebtoken.JwtParser parser() {
        return Jwts.parser().verifyWith(signingKey()).requireIssuer(ISSUER).requireAudience(AUDIENCE).build();
    }

    public String getUsernameFromJwtToken(String token) {
        return parser().parseSignedClaims(token).getPayload().getSubject();
    }

    public java.time.Instant getIssuedAt(String token) {
        return parser().parseSignedClaims(token).getPayload().getIssuedAt().toInstant();
    }

    public boolean validateJwtToken(String authToken) {
        try {
            parser().parseSignedClaims(authToken);
            return true;
        } catch (io.jsonwebtoken.IncorrectClaimException | io.jsonwebtoken.MissingClaimException e) {
            log.warn("Token from another issuer or audience rejected");
        } catch (io.jsonwebtoken.security.SecurityException e) {
            log.warn(messageConfig.getMessage(ERROR_JWT_INVALID_SIGNATURE, "invalid signature"));
        } catch (MalformedJwtException e) {
            log.warn(messageConfig.getMessage(ERROR_JWT_INVALID_TOKEN, "malformed token"));
        } catch (ExpiredJwtException e) {
            log.info(messageConfig.getMessage(ERROR_JWT_EXPIRED, "expired token"));
        } catch (UnsupportedJwtException e) {
            log.warn(messageConfig.getMessage(ERROR_JWT_UNSUPPORTED, "unsupported token"));
        } catch (IllegalArgumentException e) {
            log.warn(messageConfig.getMessage(ERROR_JWT_EMPTY_CLAIMS, "empty token"));
        }
        return false;
    }
}
