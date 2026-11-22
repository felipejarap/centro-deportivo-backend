package duoc.cl.ms_plans.controller.handler;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ApiExceptionHandlerTest {

    private final ApiExceptionHandler handler = new ApiExceptionHandler();

    @Test
    void convertsValidationErrorsToBadRequestBody() {
        BindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "plansRequest");
        bindingResult.addError(new ObjectError("plansRequest", "plan details are required"));
        MethodArgumentNotValidException exception = mock(MethodArgumentNotValidException.class);
        when(exception.getBindingResult()).thenReturn(bindingResult);

        var response = handler.handlerValidation(exception);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("plan details are required", response.getBody().get("plansRequest"));
    }

    @Test
    void convertsUnexpectedExceptionsToInternalServerErrorBody() {
        var response = handler.handlerGeneral(new IllegalStateException("plan store unavailable"));

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertEquals("plan store unavailable", response.getBody().get("error"));
    }
}
