package cl.duoc.ms_auth.security;

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
        ReflectionTestUtils.setField(jwtUtil, "expiration", 60_000L);
    }

    @Test
    void generatedAccessAndRefreshTokensCarryIdentityAndDistinctTypes() {
        String accessToken = jwtUtil.generateToken("student@example.com", 17L, "Cliente");
        String refreshToken = jwtUtil.generateRefreshToken("student@example.com", 17L, "Cliente");

        assertTrue(jwtUtil.validateToken(accessToken));
        assertTrue(jwtUtil.validateAccessToken(accessToken));
        assertFalse(jwtUtil.validateRefreshToken(accessToken));
        assertTrue(jwtUtil.validateRefreshToken(refreshToken));
        assertFalse(jwtUtil.validateAccessToken(refreshToken));
        assertEquals("student@example.com", jwtUtil.extractUsername(accessToken));
        assertEquals(17L, jwtUtil.extractIdUsuario(accessToken));
        assertEquals("Cliente", jwtUtil.extractRol(accessToken));
        assertNotNull(jwtUtil.extractTokenId(accessToken));
        assertNotEquals(jwtUtil.extractTokenId(accessToken), jwtUtil.extractTokenId(refreshToken));
    }

    @Test
    void acceptsLegacyAccessTokenWithoutTypeAndReturnsNullForAbsentIdentityClaim() {
        String token = token(SECRET, null, null, null, System.currentTimeMillis() + 60_000);

        assertTrue(jwtUtil.validateToken(token));
        assertTrue(jwtUtil.validateAccessToken(token));
        assertFalse(jwtUtil.validateRefreshToken(token));
        assertNull(jwtUtil.extractIdUsuario(token));
        assertNull(jwtUtil.extractRol(token));
    }

    @Test
    void rejectsExpiredMalformedAndWronglySignedTokens() {
        String expired = token(SECRET, "access", 17L, "Cliente", System.currentTimeMillis() - 1_000);
        String signedWithDifferentKey = token(
                "another-test-secret-key-that-is-also-32-bytes-long",
                "access", 17L, "Cliente", System.currentTimeMillis() + 60_000);

        assertFalse(jwtUtil.validateToken(expired));
        assertFalse(jwtUtil.validateAccessToken(expired));
        assertFalse(jwtUtil.validateRefreshToken(expired));
        assertFalse(jwtUtil.validateToken(signedWithDifferentKey));
        assertFalse(jwtUtil.validateToken("not-a-jwt"));
        assertFalse(jwtUtil.validateAccessToken("not-a-jwt"));
        assertFalse(jwtUtil.validateRefreshToken("not-a-jwt"));
    }

    private String token(String key, String type, Long userId, String role, long expiresAt) {
        var builder = Jwts.builder()
                .setSubject("student@example.com")
                .setIssuedAt(new Date(System.currentTimeMillis() - 1_000))
                .setExpiration(new Date(expiresAt));
        if (type != null) {
            builder.claim("tokenType", type);
        }
        if (userId != null) {
            builder.claim("idUsuario", userId);
        }
        if (role != null) {
            builder.claim("rol", role);
        }
        return builder.signWith(
                Keys.hmacShaKeyFor(key.getBytes(StandardCharsets.UTF_8)),
                SignatureAlgorithm.HS256).compact();
    }
}
