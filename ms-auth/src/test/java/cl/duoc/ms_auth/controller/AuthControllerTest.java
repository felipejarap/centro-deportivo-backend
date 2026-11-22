package cl.duoc.ms_auth.controller;

import cl.duoc.ms_auth.dto.*;
import cl.duoc.ms_auth.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private AuthService service;

    private AuthController controller;

    @BeforeEach
    void setUp() {
        controller = new AuthController(service);
    }

    @Test
    void loginReturnsOkForTokenAndUnauthorizedForInvalidCredentials() {
        AuthRequestDto request = new AuthRequestDto();
        AuthResponseDto success = authResponse("access");
        AuthResponseDto failure = authResponse(null);
        when(service.login(request)).thenReturn(success, failure);

        assertEquals(HttpStatus.OK, controller.login(request).getStatusCode());
        assertEquals(HttpStatus.UNAUTHORIZED, controller.login(request).getStatusCode());
    }

    @Test
    void refreshRequiresBearerHeaderAndReturnsUnauthorizedForUnknownToken() {
        assertEquals(HttpStatus.UNAUTHORIZED, controller.refresh("token").getStatusCode());
        when(service.refreshToken("refresh")).thenReturn(null);
        assertEquals(HttpStatus.UNAUTHORIZED, controller.refresh("Bearer refresh").getStatusCode());
        when(service.refreshToken("valid")).thenReturn(authResponse("access"));
        assertEquals(HttpStatus.OK, controller.refresh("Bearer valid").getStatusCode());
    }

    @Test
    void registrationEndpointsReturnCreated() {
        StudentRegistrationRequestDto student = new StudentRegistrationRequestDto();
        CredencialRequestDto admin = new CredencialRequestDto();
        CredencialResponseDto credential = credential();
        when(service.registerStudent(student)).thenReturn(credential);
        when(service.registerForAdmin(admin)).thenReturn(credential);

        assertEquals(HttpStatus.CREATED, controller.register(student).getStatusCode());
        assertEquals(HttpStatus.CREATED, controller.registerForAdmin(admin).getStatusCode());
        assertEquals(credential, controller.register(student).getBody());
    }

    @Test
    void credentialQueriesHandleFoundAndMissingRecords() {
        CredencialResponseDto credential = credential();
        when(service.findAll()).thenReturn(List.of(credential));
        when(service.findById(1L)).thenReturn(credential);
        when(service.findById(2L)).thenReturn(null);

        ResponseEntity<List<CredencialResponseDto>> all = controller.findAll();
        assertEquals(HttpStatus.OK, all.getStatusCode());
        assertEquals(List.of(credential), all.getBody());
        assertEquals(HttpStatus.OK, controller.findById(1L).getStatusCode());
        assertEquals(HttpStatus.NOT_FOUND, controller.findById(2L).getStatusCode());
        assertNull(controller.findById(2L).getBody());
    }

    @Test
    void deletionAndPasswordChangeReturnTheirExpectedStatuses() {
        when(service.delete(1L)).thenReturn(true);
        when(service.delete(2L)).thenReturn(false);
        when(service.changePassword(7L, "old", "new-pass")).thenReturn(true);
        when(service.changePassword(8L, "old", "new-pass")).thenReturn(false);
        PasswordChangeRequestDto request = new PasswordChangeRequestDto();
        request.setCurrentPassword("old");
        request.setNewPassword("new-pass");

        assertEquals(HttpStatus.NO_CONTENT, controller.delete(1L).getStatusCode());
        assertEquals(HttpStatus.NOT_FOUND, controller.delete(2L).getStatusCode());
        assertEquals(HttpStatus.NO_CONTENT, controller.changePassword(7L, request).getStatusCode());
        assertEquals(HttpStatus.UNAUTHORIZED, controller.changePassword(8L, request).getStatusCode());
    }

    private AuthResponseDto authResponse(String token) {
        return new AuthResponseDto(token, "refresh", "student", 7L, "Cliente", "message");
    }

    private CredencialResponseDto credential() {
        return new CredencialResponseDto(1L, "student@example.com", 7L, true);
    }
}
