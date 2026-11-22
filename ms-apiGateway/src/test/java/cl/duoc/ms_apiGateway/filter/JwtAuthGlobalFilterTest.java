package cl.duoc.ms_apiGateway.filter;

import cl.duoc.ms_apiGateway.security.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.mock.web.server.MockServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.net.URI;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JwtAuthGlobalFilterTest {

    @Mock
    private JwtUtil jwtUtil;

    private JwtAuthGlobalFilter filter;

    @BeforeEach
    void setUp() {
        filter = new JwtAuthGlobalFilter(jwtUtil);
    }

    @Test
    void rejectsProtectedRequestWithoutToken() {
        ServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/v1/users").build());

        StepVerifier.create(filter.filter(exchange, ignored -> Mono.empty())).verifyComplete();

        assertEquals(HttpStatus.UNAUTHORIZED, exchange.getResponse().getStatusCode());
    }

    @Test
    void onlyAllowsRegistrationEndpointWithPost() {
        ServerWebExchange post = MockServerWebExchange.from(
                MockServerHttpRequest.post("/api/v1/auth/register").build());
        ServerWebExchange get = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/v1/auth/register").build());
        ServerWebExchange directUserRegistration = MockServerWebExchange.from(
                MockServerHttpRequest.post("/api/v1/users/register").build());

        StepVerifier.create(filter.filter(post, ignored -> Mono.empty())).verifyComplete();
        StepVerifier.create(filter.filter(get, ignored -> Mono.empty())).verifyComplete();
        StepVerifier.create(filter.filter(directUserRegistration, ignored -> Mono.empty())).verifyComplete();

        assertNull(post.getResponse().getStatusCode());
        assertEquals(HttpStatus.UNAUTHORIZED, get.getResponse().getStatusCode());
        assertEquals(HttpStatus.UNAUTHORIZED, directUserRegistration.getResponse().getStatusCode());
    }

    @Test
    void deniesStudentAccessToAdministrativeUserMutation() {
        ServerWebExchange exchange = authenticatedRequest(HttpMethod.DELETE, "/api/v1/users/4", 4L, "Cliente");

        StepVerifier.create(filter.filter(exchange, ignored -> Mono.empty())).verifyComplete();

        assertEquals(HttpStatus.FORBIDDEN, exchange.getResponse().getStatusCode());
    }

    @Test
    void permitsStudentOwnerRouteAndOverwritesForwardedIdentityHeaders() {
        ServerWebExchange exchange = authenticatedRequest(HttpMethod.POST, "/api/v1/reservations/by-user/4", 4L, "Cliente");
        AtomicReference<ServerWebExchange> forwarded = new AtomicReference<>();

        StepVerifier.create(filter.filter(exchange, request -> {
            forwarded.set(request);
            return Mono.empty();
        })).verifyComplete();

        assertNull(exchange.getResponse().getStatusCode());
        assertEquals("4", forwarded.get().getRequest().getHeaders().getFirst("X-User-Id"));
        assertEquals("Cliente", forwarded.get().getRequest().getHeaders().getFirst("X-User-Role"));
        assertFalse(JwtAuthGlobalFilter.isAuthorized(HttpMethod.GET, "/api/v1/reservations/by-user/5", "Cliente", 4L));
        assertTrue(JwtAuthGlobalFilter.isAuthorized(HttpMethod.GET, "/api/v1/reservations/by-user/4", "Cliente", 4L));
    }

    @Test
    void acceptsRefreshTokenOnlyForRefreshEndpoint() {
        ServerWebExchange refresh = authenticatedRequest(HttpMethod.POST, "/api/v1/auth/refresh", 4L, "Cliente");
        ServerWebExchange api = authenticatedRequest(HttpMethod.GET, "/api/v1/users/4", 4L, "Cliente");
        when(jwtUtil.validateRefreshToken("token")).thenReturn(true);
        when(jwtUtil.validateAccessToken("token")).thenReturn(false);

        StepVerifier.create(filter.filter(refresh, ignored -> Mono.empty())).verifyComplete();
        StepVerifier.create(filter.filter(api, ignored -> Mono.empty())).verifyComplete();

        assertNull(refresh.getResponse().getStatusCode());
        assertEquals(HttpStatus.UNAUTHORIZED, api.getResponse().getStatusCode());
    }

    @Test
    void allowsSwaggerReadOnlyWithoutAuthentication() {
        ServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/docs/users/v3/api-docs").build());
        ServerWebExchange assets = MockServerWebExchange.from(
                MockServerHttpRequest.get("/swagger-ui/swagger-ui.css").build());

        StepVerifier.create(filter.filter(exchange, ignored -> Mono.empty())).verifyComplete();
        StepVerifier.create(filter.filter(assets, ignored -> Mono.empty())).verifyComplete();

        assertNull(exchange.getResponse().getStatusCode());
        assertNull(assets.getResponse().getStatusCode());
    }

    @Test
    void keepsSwaggerUiRedirectScopedToItsService() {
        ServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/docs/classes/api-docs").build());

        StepVerifier.create(filter.filter(exchange, ignored -> {
            exchange.getResponse().setStatusCode(HttpStatus.FOUND);
            exchange.getResponse().getHeaders().setLocation(URI.create("/swagger-ui/index.html"));
            return exchange.getResponse().setComplete();
        })).verifyComplete();

        assertEquals(URI.create("/docs/classes/swagger-ui/index.html"),
                exchange.getResponse().getHeaders().getLocation());
    }

    @Test
    void restrictsStudentSubscriptionAndAssistanceActions() {
        assertFalse(JwtAuthGlobalFilter.isAuthorized(
                HttpMethod.PUT, "/api/v1/subscriptions/by-user/4/2", "Cliente", 4L));
        assertTrue(JwtAuthGlobalFilter.isAuthorized(
                HttpMethod.POST, "/api/v1/subscriptions/by-user/4/2/cancel", "Cliente", 4L));
        assertFalse(JwtAuthGlobalFilter.isAuthorized(
                HttpMethod.GET, "/api/v1/assistances/by-user/4", "Profesor", 9L));
        assertTrue(JwtAuthGlobalFilter.isAuthorized(
                HttpMethod.GET, "/api/v1/assistances/by-classe/3", "Profesor", 9L));
    }

    private ServerWebExchange authenticatedRequest(HttpMethod method, String path, Long userId, String role) {
        MockServerHttpRequest request = MockServerHttpRequest.method(method, path)
                .header(HttpHeaders.AUTHORIZATION, "Bearer token")
                .header("X-User-Id", "999")
                .header("X-User-Role", "Admin")
                .build();
        when(jwtUtil.validateAccessToken("token")).thenReturn(true);
        when(jwtUtil.extractUsername("token")).thenReturn("user");
        when(jwtUtil.extractIdUsuario("token")).thenReturn(userId);
        when(jwtUtil.extractRol("token")).thenReturn(role);
        return MockServerWebExchange.from(request);
    }
}
