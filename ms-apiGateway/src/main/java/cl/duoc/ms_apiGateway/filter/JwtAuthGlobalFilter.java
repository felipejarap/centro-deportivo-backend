package cl.duoc.ms_apiGateway.filter;

import cl.duoc.ms_apiGateway.security.JwtUtil;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
public class JwtAuthGlobalFilter implements GlobalFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthGlobalFilter.class);
    private final JwtUtil jwtUtil;

    public JwtAuthGlobalFilter(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();
        HttpMethod method = exchange.getRequest().getMethod();

        if (method == HttpMethod.GET) {
            preserveSwaggerServiceInRedirect(exchange, path);
        }

        if (method == HttpMethod.OPTIONS || isPublicRequest(method, path)) {
            log.debug("Ruta publica, sin validacion de token: {}", path);
            return chain.filter(exchange);
        }

        String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            log.warn("Peticion sin token a ruta protegida: {}", path);
            return unauthorized(exchange, "Token no proporcionado");
        }

        String token = authHeader.substring(7);
        boolean refreshRequest = method == HttpMethod.POST && path.equals("/api/v1/auth/refresh");

        if (refreshRequest ? !jwtUtil.validateRefreshToken(token) : !jwtUtil.validateAccessToken(token)) {
            log.warn("Token invalido o expirado para: {}", path);
            return unauthorized(exchange, "Token invalido o expirado");
        }

        String username = jwtUtil.extractUsername(token);
        Long idUsuario = jwtUtil.extractIdUsuario(token);
        String rol = jwtUtil.extractRol(token);

        if (!isAuthorized(method, path, rol, idUsuario)) {
            log.warn("Acceso denegado para rol {} a {} {}", rol, method, path);
            return forbidden(exchange, "No tienes permisos para realizar esta acción");
        }

        log.debug("Token valido para usuario: {} (rol: {}) -> {}", username, rol, path);

        ServerHttpRequest mutatedRequest = exchange.getRequest().mutate()
                .headers(headers -> {
                    headers.remove("X-User-Id");
                    headers.remove("X-User-Username");
                    headers.remove("X-User-Role");
                })
                .header("X-User-Id", idUsuario != null ? idUsuario.toString() : "")
                .header("X-User-Username", username != null ? username : "")
                .header("X-User-Role", rol != null ? rol : "")
                .build();

        return chain.filter(exchange.mutate().request(mutatedRequest).build());
    }

    private boolean isPublicRequest(HttpMethod method, String path) {
        if (method == HttpMethod.POST && List.of(
                "/api/v1/auth/login",
                "/api/v1/auth/register"
        ).contains(path.toLowerCase(Locale.ROOT))) {
            return true;
        }

        String normalizedPath = path.toLowerCase(Locale.ROOT);
        if (method == HttpMethod.GET
                && (normalizedPath.startsWith("/docs/") || normalizedPath.startsWith("/swagger-ui/"))) {
            return true;
        }

        return method == HttpMethod.GET && List.of(
                "/api/v1/classes",
                "/api/v1/type-classes",
                "/api/v1/plans",
                "/api/v1/type-users"
        ).stream().anyMatch(publicPath -> path.equalsIgnoreCase(publicPath)
                || path.toLowerCase(Locale.ROOT).startsWith(publicPath.toLowerCase(Locale.ROOT) + "/"));
    }

    private void preserveSwaggerServiceInRedirect(ServerWebExchange exchange, String path) {
        if (!path.matches("^/docs/(auth|users|classes|plans|subscriptions|reservations|assistances)/api-docs/?$")) {
            return;
        }

        String servicePath = path.substring(0, path.lastIndexOf("/api-docs"));
        exchange.getResponse().beforeCommit(() -> {
            var response = exchange.getResponse();
            var status = response.getStatusCode();
            String location = response.getHeaders().getFirst(HttpHeaders.LOCATION);
            if (status != null
                    && status.value() >= 300
                    && status.value() < 400
                    && "/swagger-ui/index.html".equals(location)) {
                response.getHeaders().setLocation(URI.create(servicePath + "/swagger-ui/index.html"));
            }
            return Mono.empty();
        });
    }

    static boolean isAuthorized(HttpMethod method, String path, String role, Long userId) {
        String normalizedRole = role == null ? "" : role.trim().toLowerCase(Locale.ROOT);
        boolean admin = Set.of("admin", "administrador").contains(normalizedRole);
        boolean coach = Set.of("entrenador", "profesor").contains(normalizedRole);
        boolean student = Set.of("cliente", "alumno").contains(normalizedRole);
        String[] parts = path.split("/");

        if (userId == null) {
            return false;
        }
        if (admin) {
            return true;
        }

        if (path.equals("/api/v1/auth/refresh") && method == HttpMethod.POST) {
            return student || coach;
        }
        if (path.matches("/api/v1/users/\\d+") && method == HttpMethod.GET) {
            return parts[4].equals(userId.toString());
        }
        if (path.matches("/api/v1/users/\\d+/profile") && method == HttpMethod.PUT) {
            return parts[4].equals(userId.toString());
        }
        if (path.matches("/api/v1/auth/users/\\d+/password") && method == HttpMethod.PUT) {
            return parts[5].equals(userId.toString());
        }
        if (path.equals("/api/v1/reservations") && method == HttpMethod.GET) {
            return false;
        }
        if (path.matches("/api/v1/reservations/by-user/\\d+") && method == HttpMethod.GET) {
            return parts[5].equals(userId.toString());
        }
        if (path.matches("/api/v1/reservations/by-user/\\d+") && method == HttpMethod.POST) {
            return student && parts[5].equals(userId.toString());
        }
        if (path.matches("/api/v1/reservations/by-user/\\d+/\\d+")
                && Set.of(HttpMethod.PUT, HttpMethod.DELETE).contains(method)) {
            return student && parts[5].equals(userId.toString());
        }
        if (path.matches("/api/v1/reservations/by-coach/\\d+") && method == HttpMethod.GET) {
            return coach && parts[5].equals(userId.toString());
        }
        if (path.matches("/api/v1/subscriptions/by-user/\\d+") && method == HttpMethod.GET) {
            return parts[5].equals(userId.toString());
        }
        if (path.matches("/api/v1/subscriptions/by-user/\\d+") && method == HttpMethod.POST) {
            return student && parts[5].equals(userId.toString());
        }
        if (path.matches("/api/v1/subscriptions/by-user/\\d+/\\d+/(renew|cancel)")
                && method == HttpMethod.POST) {
            return student && parts[5].equals(userId.toString());
        }
        if (path.matches("/api/v1/assistances/by-classe/\\d+") && method == HttpMethod.GET) {
            return coach;
        }
        if (path.equals("/api/v1/assistances") && method == HttpMethod.POST) {
            return coach;
        }
        if (path.matches("/api/v1/assistances/\\d+")
                && Set.of(HttpMethod.PUT, HttpMethod.DELETE).contains(method)) {
            return coach;
        }
        return false;
    }

    private Mono<Void> unauthorized(ServerWebExchange exchange, String message) {
        return writeError(exchange, HttpStatus.UNAUTHORIZED, message);
    }

    private Mono<Void> forbidden(ServerWebExchange exchange, String message) {
        return writeError(exchange, HttpStatus.FORBIDDEN, message);
    }

    private Mono<Void> writeError(ServerWebExchange exchange, HttpStatus status, String message) {
        exchange.getResponse().setStatusCode(status);
        exchange.getResponse().getHeaders().add(HttpHeaders.CONTENT_TYPE, "application/json");
        byte[] bytes = ("{\"error\":\"" + message + "\"}").getBytes(StandardCharsets.UTF_8);
        DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(bytes);
        return exchange.getResponse().writeWith(Mono.just(buffer));
    }

    @Override
    public int getOrder() {
        return -1;
    }
}
