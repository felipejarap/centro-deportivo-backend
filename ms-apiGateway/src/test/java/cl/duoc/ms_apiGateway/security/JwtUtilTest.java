package cl.duoc.ms_apiGateway.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

class JwtUtilTest {

    private static final String SECRET = "a-test-secret-key-that-is-at-least-32-bytes-long";
    private JwtUtil jwtUtil;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(jwtUtil, "secret", SECRET);
    }

    @Test
    void validatesAccessAndRefreshTokensAndExtractsIdentityClaims() {
        String access = token(SECRET, "access", System.currentTimeMillis() + 60_000);
        String refresh = token(SECRET, "refresh", System.currentTimeMillis() + 60_000);

        assertTrue(jwtUtil.validateToken(access));
        assertTrue(jwtUtil.validateAccessToken(access));
        assertFalse(jwtUtil.validateRefreshToken(access));
        assertTrue(jwtUtil.validateRefreshToken(refresh));
        assertFalse(jwtUtil.validateAccessToken(refresh));
        assertEquals("student@example.com", jwtUtil.extractUsername(access));
        assertEquals(17L, jwtUtil.extractIdUsuario(access));
        assertEquals("Cliente", jwtUtil.extractRol(access));
    }

    @Test
    void acceptsLegacyTokenWithoutTypeAndRejectsExpiredMalformedOrWronglySignedTokens() {
        String legacy = token(SECRET, null, System.currentTimeMillis() + 60_000);
        String expired = token(SECRET, "access", System.currentTimeMillis() - 1_000);
        String otherKey = token(
                "another-test-secret-key-that-is-also-32-bytes-long",
                "access", System.currentTimeMillis() + 60_000);

        assertTrue(jwtUtil.validateAccessToken(legacy));
        assertFalse(jwtUtil.validateRefreshToken(legacy));
        assertFalse(jwtUtil.validateToken(expired));
        assertFalse(jwtUtil.validateAccessToken(expired));
        assertFalse(jwtUtil.validateRefreshToken(expired));
        assertFalse(jwtUtil.validateToken(otherKey));
        assertFalse(jwtUtil.validateToken("not-a-jwt"));
        assertFalse(jwtUtil.validateAccessToken("not-a-jwt"));
        assertFalse(jwtUtil.validateRefreshToken("not-a-jwt"));
    }

    private String token(String key, String type, long expiresAt) {
        var builder = Jwts.builder()
                .setSubject("student@example.com")
                .claim("idUsuario", 17L)
                .claim("rol", "Cliente")
                .setIssuedAt(new Date(System.currentTimeMillis() - 1_000))
                .setExpiration(new Date(expiresAt));
        if (type != null) {
            builder.claim("tokenType", type);
        }
        return builder.signWith(
                Keys.hmacShaKeyFor(key.getBytes(StandardCharsets.UTF_8)),
                SignatureAlgorithm.HS256).compact();
    }
}
