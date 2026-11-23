package duoc.cl.ms_classes.controller.handler;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ApiExceptionHandlerTest {

    private final ApiExceptionHandler handler = new ApiExceptionHandler();

    @Test
    void convertsValidationErrorsToBadRequestBody() {
        BindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "classeRequest");
        bindingResult.addError(new ObjectError("classeRequest", "class details are required"));
        MethodArgumentNotValidException exception = mock(MethodArgumentNotValidException.class);
        when(exception.getBindingResult()).thenReturn(bindingResult);

        var response = handler.handlerValidation(exception);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("class details are required", response.getBody().get("classeRequest"));
    }

    @Test
    void convertsUnexpectedExceptionsToInternalServerErrorBody() {
        var response = handler.handlerGeneral(new IllegalStateException("capacity unavailable"));

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertEquals("capacity unavailable", response.getBody().get("error"));
    }

    @Test
    void preservesExpectedResponseStatusExceptions() {
        var exception = new ResponseStatusException(HttpStatus.BAD_REQUEST, "invalid coach");

        var response = handler.handlerResponseStatus(exception);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("invalid coach", response.getBody().get("error"));
    }
}
