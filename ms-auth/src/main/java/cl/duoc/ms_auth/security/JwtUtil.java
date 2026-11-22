package cl.duoc.ms_auth.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;
import java.util.UUID;

@Component
public class JwtUtil {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration}")
    private long expiration;

    private Key getKey() {
        return Keys.hmacShaKeyFor(secret.getBytes());
    }

    public String generateToken(String username, Long idUsuario, String rol) {
        return generateToken(username, idUsuario, rol, "access", expiration);
    }

    public String generateRefreshToken(String username, Long idUsuario, String rol) {
        return generateToken(username, idUsuario, rol, "refresh", 7L * 24 * 60 * 60 * 1000);
    }

    private String generateToken(String username, Long idUsuario, String rol, String tokenType, long expiresIn) {
        return Jwts.builder()
                .setSubject(username)
                .claim("idUsuario", idUsuario)
                .claim("rol", rol)
                .claim("tokenType", tokenType)
                .setId(UUID.randomUUID().toString())
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + expiresIn))
                .signWith(getKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    public String extractUsername(String token) {
        return parseClaims(token).getSubject();
    }

    public Long extractIdUsuario(String token) {
        Number id = parseClaims(token).get("idUsuario", Number.class);
        return id != null ? id.longValue() : null;
    }

    public String extractRol(String token) {
        return parseClaims(token).get("rol", String.class);
    }

    public String extractTokenId(String token) {
        return parseClaims(token).getId();
    }

    public boolean validateAccessToken(String token) {
        try {
            String type = parseClaims(token).get("tokenType", String.class);
            return type == null || "access".equals(type);
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    public boolean validateRefreshToken(String token) {
        try {
            return "refresh".equals(parseClaims(token).get("tokenType", String.class));
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    public boolean validateToken(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (JwtException e) {
            return false;
        }
    }

    private Claims parseClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
}
