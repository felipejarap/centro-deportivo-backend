package cl.duoc.ms_subscription.controller.handler;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ApiExceptionHandlerTest {

    private final ApiExceptionHandler handler = new ApiExceptionHandler();

    @Test
    void preservesExpectedResponseStatusExceptions() {
        var exception = new ResponseStatusException(HttpStatus.BAD_REQUEST, "invalid subscription reference");

        var response = handler.handlerResponseStatus(exception);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("invalid subscription reference", response.getBody().get("error"));
    }
}
