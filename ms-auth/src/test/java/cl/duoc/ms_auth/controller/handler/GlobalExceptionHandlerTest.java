package cl.duoc.ms_auth.controller.handler;

import cl.duoc.ms_auth.controller.AuthController;
import cl.duoc.ms_auth.dto.AuthRequestDto;
import cl.duoc.ms_auth.exception.UserNotFoundException;
import cl.duoc.ms_auth.exception.UsernameAlreadyExistsException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.core.MethodParameter;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void mapsDuplicateAndMissingUsersToConflictAndNotFound() {
        var duplicate = handler.handleUsernameExists(new UsernameAlreadyExistsException("duplicate"));
        var missing = handler.handleUserNotFound(new UserNotFoundException("missing"));

        assertEquals(HttpStatus.CONFLICT, duplicate.getStatusCode());
        assertEquals(Map.of("error", "duplicate"), duplicate.getBody());
        assertEquals(HttpStatus.NOT_FOUND, missing.getStatusCode());
        assertEquals(Map.of("error", "missing"), missing.getBody());
    }

    @Test
    void preservesResponseStatusAndProvidesDefaultReasonWhenAbsent() {
        var withReason = handler.handleResponseStatus(
                new ResponseStatusException(HttpStatus.BAD_GATEWAY, "users unavailable"));
        var withoutReason = handler.handleResponseStatus(
                new ResponseStatusException(HttpStatus.BAD_REQUEST));

        assertEquals(HttpStatus.BAD_GATEWAY, withReason.getStatusCode());
        assertEquals(Map.of("error", "users unavailable"), withReason.getBody());
        assertEquals(HttpStatus.BAD_REQUEST, withoutReason.getStatusCode());
        assertEquals(Map.of("error", "Solicitud no válida."), withoutReason.getBody());
    }

    @Test
    void returnsFieldSpecificValidationErrors() throws Exception {
        var binding = new BeanPropertyBindingResult(new AuthRequestDto(), "request");
        binding.addError(new FieldError("request", "username", "must not be blank"));
        MethodParameter parameter = new MethodParameter(
                AuthController.class.getMethod("login", AuthRequestDto.class), 0);

        var response = handler.handleValidationErrors(
                new MethodArgumentNotValidException(parameter, binding));

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals(Map.of("username", "must not be blank"), response.getBody());
    }

    @Test
    void mapsUnexpectedExceptionsToInternalErrorWithDetail() {
        var response = handler.handleGenericError(new IllegalStateException("unexpected"));

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertEquals(Map.of(
                "error", "Error interno del servidor",
                "detalle", "unexpected"), response.getBody());
    }
}
