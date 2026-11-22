package cl.duoc.ms_auth.security;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class JwtFilterTest {

    private final JwtUtil jwtUtil = mock(JwtUtil.class);
    private final JwtFilter filter = new JwtFilter(jwtUtil);
    private final FilterChain chain = mock(FilterChain.class);

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void acceptsRefreshTokenOnlyOnRefreshEndpoint() throws Exception {
        MockHttpServletRequest refreshRequest = request("POST", "/api/v1/auth/refresh", "refresh-token");
        MockHttpServletResponse refreshResponse = new MockHttpServletResponse();
        when(jwtUtil.validateRefreshToken("refresh-token")).thenReturn(true);
        when(jwtUtil.extractUsername("refresh-token")).thenReturn("student");
        when(jwtUtil.extractRol("refresh-token")).thenReturn("Cliente");

        filter.doFilter(refreshRequest, refreshResponse, chain);

        assertEquals(200, refreshResponse.getStatus());
        verify(chain).doFilter(refreshRequest, refreshResponse);

        MockHttpServletRequest apiRequest = request("GET", "/api/v1/auth/credenciales", "refresh-token");
        MockHttpServletResponse apiResponse = new MockHttpServletResponse();

        filter.doFilter(apiRequest, apiResponse, chain);

        assertEquals(401, apiResponse.getStatus());
        verify(chain, never()).doFilter(apiRequest, apiResponse);
    }

    @Test
    void rejectsAccessTokenOnRefreshEndpoint() throws Exception {
        MockHttpServletRequest request = request("POST", "/api/v1/auth/refresh", "access-token");
        MockHttpServletResponse response = new MockHttpServletResponse();
        when(jwtUtil.validateRefreshToken("access-token")).thenReturn(false);

        filter.doFilter(request, response, chain);

        assertEquals(401, response.getStatus());
        verify(chain, never()).doFilter(request, response);
    }

    @Test
    void acceptsAccessTokenOnProtectedEndpoint() throws Exception {
        MockHttpServletRequest request = request("GET", "/api/v1/auth/credenciales", "access-token");
        MockHttpServletResponse response = new MockHttpServletResponse();
        when(jwtUtil.validateAccessToken("access-token")).thenReturn(true);
        when(jwtUtil.extractUsername("access-token")).thenReturn("student");
        when(jwtUtil.extractRol("access-token")).thenReturn("Cliente");

        filter.doFilter(request, response, chain);

        assertEquals(200, response.getStatus());
        verify(chain).doFilter(request, response);
    }

    private MockHttpServletRequest request(String method, String path, String token) {
        MockHttpServletRequest request = new MockHttpServletRequest(method, path);
        request.addHeader("Authorization", "Bearer " + token);
        return request;
    }
}
