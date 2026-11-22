package cl.duoc.ms_reservas.controller;

import cl.duoc.ms_reservas.dto.ReservationRequestDto;
import cl.duoc.ms_reservas.dto.ReservationResponseDto;
import cl.duoc.ms_reservas.service.ReservationService;
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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReservationControllerTest {

    @Mock
    private ReservationService service;

    @InjectMocks
    private ReservationController controller;

    private ReservationRequestDto request(Long userId, String status) {
        ReservationRequestDto request = new ReservationRequestDto();
        request.setIdUser(userId);
        request.setIdClasse(5L);
        request.setIdCoach(3L);
        request.setReservationDate(LocalDateTime.of(2026, 6, 25, 14, 0));
        request.setReservationStatus(status);
        return request;
    }

    private ReservationResponseDto response(Long userId, String status) {
        ReservationResponseDto response = new ReservationResponseDto();
        response.setIdReservation(1L);
        response.setIdUser(userId);
        response.setIdClasse(5L);
        response.setIdCoach(3L);
        response.setReservationDate(LocalDateTime.of(2026, 6, 25, 14, 0));
        response.setReservationStatus(status);
        return response;
    }

    @Test
    void findAllReturnsServiceResults() {
        List<ReservationResponseDto> results = List.of(response(10L, "CONFIRMADA"));
        when(service.findAll()).thenReturn(results);

        assertEquals(results, controller.findAll().getBody());
    }

    @Test
    void findByIdReturnsFoundOrNotFound() {
        ReservationResponseDto result = response(10L, "CONFIRMADA");
        when(service.findById(1L)).thenReturn(result);
        assertEquals(HttpStatus.OK, controller.findById(1L).getStatusCode());

        when(service.findById(2L)).thenReturn(null);
        assertEquals(HttpStatus.NOT_FOUND, controller.findById(2L).getStatusCode());
    }

    @Test
    void createReturnsCreatedReservation() {
        ReservationRequestDto request = request(10L, "CONFIRMADA");
        ReservationResponseDto result = response(10L, "CONFIRMADA");
        when(service.create(request)).thenReturn(result);

        ResponseEntity<ReservationResponseDto> response = controller.create(request);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals(result, response.getBody());
    }

    @Test
    void createForUserRejectsEitherIdentityMismatchAndAllowsMatchingIdentity() {
        ReservationRequestDto matching = request(10L, "CONFIRMADA");
        ReservationResponseDto result = response(10L, "CONFIRMADA");
        when(service.create(matching)).thenReturn(result);

        assertEquals(HttpStatus.FORBIDDEN, controller.createForUser(10L, 11L, matching).getStatusCode());
        assertEquals(HttpStatus.FORBIDDEN,
                controller.createForUser(10L, 10L, request(11L, "CONFIRMADA")).getStatusCode());
        ResponseEntity<ReservationResponseDto> created = controller.createForUser(10L, 10L, matching);
        assertEquals(HttpStatus.CREATED, created.getStatusCode());
        verify(service, times(1)).create(matching);
    }

    @Test
    void updateReturnsOkOrNotFound() {
        ReservationRequestDto request = request(10L, "CANCELADA");
        when(service.update(1L, request)).thenReturn(response(10L, "CANCELADA"));
        assertEquals(HttpStatus.OK, controller.update(1L, request).getStatusCode());

        when(service.update(2L, request)).thenReturn(null);
        assertEquals(HttpStatus.NOT_FOUND, controller.update(2L, request).getStatusCode());
    }

    @Test
    void updateForUserChecksOwnershipAndOnlyPermitsCancellationWithoutChangingReservationDetails() {
        ReservationRequestDto cancellation = request(10L, "CANCELADA");
        ReservationResponseDto current = response(10L, "CONFIRMADA");
        when(service.findById(1L)).thenReturn(current);
        when(service.update(1L, cancellation)).thenReturn(response(10L, "CANCELADA"));

        assertEquals(HttpStatus.FORBIDDEN,
                controller.updateForUser(10L, 1L, 11L, cancellation).getStatusCode());
        assertEquals(HttpStatus.FORBIDDEN,
                controller.updateForUser(10L, 1L, 10L, request(11L, "CANCELADA")).getStatusCode());
        assertEquals(HttpStatus.FORBIDDEN,
                controller.updateForUser(10L, 1L, 10L, request(10L, "CONFIRMADA")).getStatusCode());
        ReservationRequestDto changedClass = request(10L, "CANCELADA");
        changedClass.setIdClasse(6L);
        assertEquals(HttpStatus.FORBIDDEN,
                controller.updateForUser(10L, 1L, 10L, changedClass).getStatusCode());
        ReservationRequestDto changedCoach = request(10L, "CANCELADA");
        changedCoach.setIdCoach(4L);
        assertEquals(HttpStatus.FORBIDDEN,
                controller.updateForUser(10L, 1L, 10L, changedCoach).getStatusCode());
        ReservationRequestDto changedDate = request(10L, "CANCELADA");
        changedDate.setReservationDate(changedDate.getReservationDate().plusDays(1));
        assertEquals(HttpStatus.FORBIDDEN,
                controller.updateForUser(10L, 1L, 10L, changedDate).getStatusCode());

        assertEquals(HttpStatus.OK, controller.updateForUser(10L, 1L, 10L, cancellation).getStatusCode());
        verify(service).update(1L, cancellation);
    }

    @Test
    void updateForUserDistinguishesMissingReservationAndServiceRace() {
        ReservationRequestDto cancellation = request(10L, "CANCELADA");
        when(service.findById(2L)).thenReturn(null);
        assertEquals(HttpStatus.NOT_FOUND, controller.updateForUser(10L, 2L, 10L, cancellation).getStatusCode());

        when(service.findById(3L)).thenReturn(response(10L, "CONFIRMADA"));
        when(service.update(3L, cancellation)).thenReturn(null);
        assertEquals(HttpStatus.NOT_FOUND, controller.updateForUser(10L, 3L, 10L, cancellation).getStatusCode());
    }

    @Test
    void deleteReturnsNoContentOrNotFound() {
        when(service.delete(1L)).thenReturn(true);
        when(service.delete(2L)).thenReturn(false);
        assertEquals(HttpStatus.NO_CONTENT, controller.delete(1L).getStatusCode());
        assertEquals(HttpStatus.NOT_FOUND, controller.delete(2L).getStatusCode());
    }

    @Test
    void deleteForUserChecksAuthenticationAndOwnership() {
        when(service.findById(1L)).thenReturn(response(10L, "CONFIRMADA"));
        when(service.delete(1L)).thenReturn(true);
        assertEquals(HttpStatus.FORBIDDEN, controller.deleteForUser(10L, 1L, 11L).getStatusCode());
        assertEquals(HttpStatus.NO_CONTENT, controller.deleteForUser(10L, 1L, 10L).getStatusCode());

        when(service.findById(2L)).thenReturn(response(11L, "CONFIRMADA"));
        assertEquals(HttpStatus.FORBIDDEN, controller.deleteForUser(10L, 2L, 10L).getStatusCode());
        when(service.findById(3L)).thenReturn(null);
        assertEquals(HttpStatus.NOT_FOUND, controller.deleteForUser(10L, 3L, 10L).getStatusCode());

        when(service.findById(4L)).thenReturn(response(10L, "CONFIRMADA"));
        when(service.delete(4L)).thenReturn(false);
        assertEquals(HttpStatus.NOT_FOUND, controller.deleteForUser(10L, 4L, 10L).getStatusCode());
    }

    @Test
    void filteredSearchEndpointsReturnResultsOrNotFoundOnException() throws Exception {
        List<ReservationResponseDto> results = List.of(response(10L, "CONFIRMADA"));
        when(service.findByUserId(10L)).thenReturn(results);
        when(service.findByUserId(11L)).thenThrow(new Exception("missing user"));
        when(service.findByCoachId(3L)).thenReturn(results);
        when(service.findByCoachId(4L)).thenThrow(new Exception("missing coach"));
        when(service.findByClasseId(5L)).thenReturn(results);
        when(service.findByClasseId(6L)).thenThrow(new Exception("missing class"));
        when(service.findByUserId(12L)).thenReturn(null);

        assertEquals(results, controller.findByUserId(10L).getBody());
        assertEquals(HttpStatus.NOT_FOUND, controller.findByUserId(11L).getStatusCode());
        assertEquals(results, controller.findByCoachId(3L).getBody());
        assertEquals(HttpStatus.NOT_FOUND, controller.findByCoachId(4L).getStatusCode());
        assertEquals(results, controller.findByClasseId(5L).getBody());
        assertEquals(HttpStatus.NOT_FOUND, controller.findByClasseId(6L).getStatusCode());
        assertNull(controller.findByUserId(12L).getBody());
    }
}
