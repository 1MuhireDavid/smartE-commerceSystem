package org.ecommerce.api.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.ecommerce.api.entity.UserEntity;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.Map;
import java.util.function.Function;

@Service
public class JwtService {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration}")
    private long expiration;

    // ── Token generation ──────────────────────────────────────────────────────

    /**
     * Generates a token with enriched claims when the principal is a full UserEntity.
     * Adds userId, email, and role so consumers can read them without a DB lookup.
     */
    public String generateToken(UserEntity user) {
        return buildToken(
            Map.of(
                "userId", user.getUserId(),
                "email",  user.getEmail(),
                "role",   user.getRole()
            ),
            user.getUsername()
        );
    }

    /** Minimal token for cases where only a UserDetails handle is available. */
    public String generateToken(UserDetails user) {
        return buildToken(Map.of(), user.getUsername());
    }

    private String buildToken(Map<String, Object> extraClaims, String subject) {
        Date now = new Date();
        return Jwts.builder()
                .claims(extraClaims)
                .subject(subject)
                .issuedAt(now)
                .expiration(new Date(now.getTime() + expiration))
                .signWith(signingKey(), Jwts.SIG.HS256)   // explicit HS256 (US 2.2)
                .compact();
    }

    // ── Token validation ──────────────────────────────────────────────────────

    /**
     * Returns true only when the subject matches and the token is not expired.
     * Throws JwtException subtypes (ExpiredJwtException, SignatureException, etc.)
     * on any structural problem so callers can distinguish the failure type.
     */
    public boolean isTokenValid(String token, UserDetails user) {
        return extractUsername(token).equals(user.getUsername()) && !isExpired(token);
    }

    // ── Claim extraction ──────────────────────────────────────────────────────

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    /** Exposes all claims so controllers can build a decoded-token view (US 2.2). */
    public Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(signingKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public long getExpiration() {
        return expiration;
    }

    // ── Internal helpers ──────────────────────────────────────────────────────

    private boolean isExpired(String token) {
        return extractClaim(token, Claims::getExpiration).before(new Date());
    }

    private <T> T extractClaim(String token, Function<Claims, T> resolver) {
        return resolver.apply(extractAllClaims(token));
    }

    private SecretKey signingKey() {
        return Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
    }
}
