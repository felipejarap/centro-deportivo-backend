package cl.duoc.ms_subscription.controller;

import cl.duoc.ms_subscription.dto.SubscriptionRequestDto;
import cl.duoc.ms_subscription.dto.SubscriptionResponseDto;
import cl.duoc.ms_subscription.service.SubscriptionService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SubscriptionControllerTest {

    @Mock
    private SubscriptionService service;

    @InjectMocks
    private SubscriptionController controller;

    private SubscriptionRequestDto request(Long userId) {
        return new SubscriptionRequestDto(1L, userId, 5L, LocalDateTime.of(2026, 7, 25, 10, 0),
                true, LocalDateTime.of(2026, 6, 25, 10, 0), false, "Tarjeta");
    }

    private SubscriptionResponseDto response(Long userId) {
        SubscriptionResponseDto response = new SubscriptionResponseDto();
        response.setId(1L);
        response.setUserId(userId);
        return response;
    }

    @Test
    void findAllAndFindByIdReturnExpectedStatuses() {
        List<SubscriptionResponseDto> results = List.of(response(10L));
        when(service.findAll()).thenReturn(results);
        when(service.findById(1L)).thenReturn(results.getFirst());
        when(service.findById(2L)).thenReturn(null);

        assertEquals(results, controller.findAll().getBody());
        assertEquals(HttpStatus.OK, controller.findById(1L).getStatusCode());
        assertEquals(HttpStatus.NOT_FOUND, controller.findById(2L).getStatusCode());
    }

    @Test
    void filteredQueriesReturnServiceResultsAndTranslateExceptionsToNotFound() throws Exception {
        List<SubscriptionResponseDto> results = List.of(response(10L));
        when(service.findByUserId(10L)).thenReturn(results);
        when(service.findByUserId(11L)).thenThrow(new Exception("missing user"));
        when(service.findByPlansId(5L)).thenReturn(results);
        when(service.findByPlansId(6L)).thenThrow(new Exception("missing plan"));

        assertEquals(results, controller.findByUserId(10L).getBody());
        assertEquals(HttpStatus.NOT_FOUND, controller.findByUserId(11L).getStatusCode());
        assertEquals(results, controller.findByPlansId(5L).getBody());
        assertEquals(HttpStatus.NOT_FOUND, controller.findByPlansId(6L).getStatusCode());
    }

    @Test
    void createReturnsCreatedAndScopedCreateChecksBothIdentities() {
        SubscriptionRequestDto request = request(10L);
        SubscriptionResponseDto created = response(10L);
        when(service.create(request)).thenReturn(created);
        when(service.createForUser(10L, request)).thenReturn(created);

        assertEquals(HttpStatus.CREATED, controller.create(request).getStatusCode());
        assertEquals(HttpStatus.FORBIDDEN, controller.createForUser(10L, 11L, request).getStatusCode());
        assertEquals(HttpStatus.FORBIDDEN, controller.createForUser(10L, 10L, request(11L)).getStatusCode());
        ResponseEntity<SubscriptionResponseDto> scoped = controller.createForUser(10L, 10L, request);
        assertEquals(HttpStatus.CREATED, scoped.getStatusCode());
        assertEquals(created, scoped.getBody());
    }

    @Test
    void updateReturnsOkOrNotFound() {
        SubscriptionRequestDto request = request(10L);
        when(service.update(1L, request)).thenReturn(response(10L));
        when(service.update(2L, request)).thenReturn(null);
        assertEquals(HttpStatus.OK, controller.update(1L, request).getStatusCode());
        assertEquals(HttpStatus.NOT_FOUND, controller.update(2L, request).getStatusCode());
    }

    @Test
    void renewAndCancelRequireAuthenticatedOwnerAndReturnNotFoundWhenMissing() {
        SubscriptionResponseDto result = response(10L);
        when(service.renewForUser(10L, 1L)).thenReturn(result);
        when(service.renewForUser(10L, 2L)).thenReturn(null);
        when(service.cancelForUser(10L, 1L)).thenReturn(result);
        when(service.cancelForUser(10L, 2L)).thenReturn(null);

        assertEquals(HttpStatus.FORBIDDEN, controller.renewForUser(10L, 1L, 11L).getStatusCode());
        assertEquals(HttpStatus.OK, controller.renewForUser(10L, 1L, 10L).getStatusCode());
        assertEquals(HttpStatus.NOT_FOUND, controller.renewForUser(10L, 2L, 10L).getStatusCode());
        assertEquals(HttpStatus.FORBIDDEN, controller.cancelForUser(10L, 1L, 11L).getStatusCode());
        assertEquals(HttpStatus.OK, controller.cancelForUser(10L, 1L, 10L).getStatusCode());
        assertEquals(HttpStatus.NOT_FOUND, controller.cancelForUser(10L, 2L, 10L).getStatusCode());
    }

    @Test
    void deleteReturnsNoContentOrNotFound() {
        when(service.deleteById(1L)).thenReturn(true);
        when(service.deleteById(2L)).thenReturn(false);
        assertEquals(HttpStatus.NO_CONTENT, controller.delete(1L).getStatusCode());
        assertEquals(HttpStatus.NOT_FOUND, controller.delete(2L).getStatusCode());
    }
}
