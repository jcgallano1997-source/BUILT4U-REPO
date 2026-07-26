package com.built4u.pos.auth;

import com.built4u.pos.site.Site;
import com.built4u.pos.user.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.List;

@Service
public class JwtService {

    private final SecretKey signingKey;
    private final long accessTtlMs;

    public JwtService(
        @Value("${app.jwt.secret}") String secret,
        @Value("${app.jwt.access-ttl-ms:900000}") long accessTtlMs
    ) {
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessTtlMs = accessTtlMs;
    }

    /**
     * Access token. Single-business: no entity/plan claims. Modules are derived
     * from role grants only — a wildcard (ADMIN) role emits the {@code "*"}
     * sentinel (expanded to every {@code MOD_*} by the filter); non-wildcard
     * roles emit the union of their granted module codes.
     */
    public String generateAccessToken(User user, Site activeSite) {
        Instant now = Instant.now();
        List<String> roles = user.getRoles().stream().map(r -> r.getCode()).toList();

        boolean wildcard = user.getRoles().stream().anyMatch(r -> r.isWildcard());
        List<String> modules;
        if (wildcard) {
            modules = List.of(Modules.WILDCARD);
        } else {
            modules = user.getRoles().stream()
                .flatMap(r -> r.getModuleCodes().stream())
                .distinct()
                .sorted()
                .toList();
        }

        return Jwts.builder()
            .subject(user.getUsername())
            .claim("typ", "access")
            .claim("userId", user.getId())
            .claim("roles", roles)
            .claim("modules", modules)
            .claim("siteId", activeSite.getId())
            .claim("siteCode", activeSite.getCode())
            .claim("siteName", activeSite.getName())
            .claim("mustChangePassword", user.isMustChangePassword())
            .issuedAt(Date.from(now))
            .expiration(Date.from(now.plusMillis(accessTtlMs)))
            .signWith(signingKey, Jwts.SIG.HS512)
            .compact();
    }

    public Claims parse(String token) {
        return Jwts.parser()
            .verifyWith(signingKey)
            .build()
            .parseSignedClaims(token)
            .getPayload();
    }

    /** Generate a 256-bit random opaque refresh token (base64url, no padding). */
    public String generateRefreshTokenRaw() {
        byte[] bytes = new byte[32];
        new SecureRandom().nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /** SHA-256 of the raw refresh token. We store this hash, never the raw value. */
    public String hashRefreshToken(String rawToken) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(64);
            for (byte b : hash) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }
}
