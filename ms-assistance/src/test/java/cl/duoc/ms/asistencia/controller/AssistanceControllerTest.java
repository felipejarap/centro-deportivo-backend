package cl.duoc.ms.asistencia.controller;

import cl.duoc.ms.asistencia.dto.AssistanceResponseDto;
import cl.duoc.ms.asistencia.dto.AssistanceRequestDto;
import cl.duoc.ms.asistencia.dto.ClasseResponseDto;
import cl.duoc.ms.asistencia.service.AssistanceService;
import cl.duoc.ms.asistencia.service.api.ClasseClient;
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
import static org.mockito.Mockito.never;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AssistanceControllerTest {

    @Mock
    private AssistanceService service;

    @Mock
    private ClasseClient classeClient;

    @InjectMocks
    private AssistanceController controller;

    @Test
    void coachCanReadAttendanceForOwnedClass() throws Exception {
        ClasseResponseDto gymClass = new ClasseResponseDto();
        gymClass.setCoachId(7L);
        when(classeClient.findById(3L)).thenReturn(gymClass);
        when(service.findByClasseId(3L)).thenReturn(List.of(new AssistanceResponseDto()));

        ResponseEntity<List<AssistanceResponseDto>> response = controller.findByClasseId(3L, 7L, "Profesor");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(service).findByClasseId(3L);
    }

    @Test
    void coachCannotReadAttendanceForAnotherCoachsClass() throws Exception {
        ClasseResponseDto gymClass = new ClasseResponseDto();
        gymClass.setCoachId(8L);
        when(classeClient.findById(3L)).thenReturn(gymClass);

        ResponseEntity<List<AssistanceResponseDto>> response = controller.findByClasseId(3L, 7L, "Profesor");

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        verify(service, never()).findByClasseId(3L);
    }

    private AssistanceRequestDto request(Long userId, Long classId) {
        AssistanceRequestDto request = new AssistanceRequestDto();
        request.setIdUser(userId);
        request.setIdClasse(classId);
        request.setArrivalTime(LocalDateTime.of(2026, 6, 21, 10, 0));
        request.setAssist(true);
        return request;
    }

    private AssistanceResponseDto response(Long userId, Long classId) {
        AssistanceResponseDto response = new AssistanceResponseDto();
        response.setIdAssistance(1L);
        response.setIdUser(userId);
        response.setIdClasse(classId);
        return response;
    }

    @Test
    void adminCanCreateAndFindAttendance() {
        AssistanceRequestDto request = request(10L, 3L);
        AssistanceResponseDto result = response(10L, 3L);
        when(service.create(request)).thenReturn(result);
        when(service.findAll()).thenReturn(List.of(result));
        when(service.findById(1L)).thenReturn(result);
        when(service.findById(2L)).thenReturn(null);

        assertEquals(HttpStatus.CREATED, controller.create(99L, "ADMIN", request).getStatusCode());
        assertEquals(List.of(result), controller.findAll().getBody());
        assertEquals(HttpStatus.OK, controller.findById(1L).getStatusCode());
        assertEquals(HttpStatus.NOT_FOUND, controller.findById(2L).getStatusCode());
        verify(classeClient, never()).findById(anyLong());
    }

    @Test
    void classManagerValidationHandlesRolesOwnershipAndClientFailures() {
        AssistanceRequestDto request = request(10L, 3L);
        AssistanceResponseDto result = response(10L, 3L);
        when(service.create(request)).thenReturn(result);
        ClasseResponseDto ownedClass = new ClasseResponseDto();
        ownedClass.setCoachId(7L);
        when(classeClient.findById(3L)).thenReturn(ownedClass);

        assertEquals(HttpStatus.CREATED, controller.create(7L, "Entrenador", request).getStatusCode());
        assertEquals(HttpStatus.FORBIDDEN, controller.create(null, "Profesor", request).getStatusCode());
        assertEquals(HttpStatus.FORBIDDEN, controller.create(7L, "Alumno", request).getStatusCode());

        ClasseResponseDto noClass = new ClasseResponseDto();
        when(classeClient.findById(4L)).thenReturn(noClass);
        assertEquals(HttpStatus.FORBIDDEN, controller.create(7L, "Profesor", request(10L, 4L)).getStatusCode());
        when(classeClient.findById(5L)).thenReturn(null);
        assertEquals(HttpStatus.FORBIDDEN, controller.create(7L, "Profesor", request(10L, 5L)).getStatusCode());
        when(classeClient.findById(6L)).thenThrow(new RuntimeException("unavailable"));
        assertEquals(HttpStatus.FORBIDDEN, controller.create(7L, "Profesor", request(10L, 6L)).getStatusCode());
    }

    @Test
    void updateChecksExistenceIdentityAndClassPermission() {
        AssistanceRequestDto request = request(10L, 3L);
        AssistanceResponseDto existing = response(10L, 3L);
        when(service.findById(1L)).thenReturn(existing);
        when(service.findById(2L)).thenReturn(null);
        when(service.update(1L, request)).thenReturn(response(10L, 3L));

        assertEquals(HttpStatus.NOT_FOUND, controller.update(2L, 99L, "ADMIN", request).getStatusCode());
        assertEquals(HttpStatus.FORBIDDEN, controller.update(1L, 99L, "ADMIN", request(11L, 3L)).getStatusCode());
        assertEquals(HttpStatus.FORBIDDEN, controller.update(1L, 99L, "ADMIN", request(10L, 4L)).getStatusCode());
        assertEquals(HttpStatus.OK, controller.update(1L, 99L, "ADMIN", request).getStatusCode());
        assertEquals(HttpStatus.FORBIDDEN, controller.update(1L, null, "Profesor", request).getStatusCode());
    }

    @Test
    void deleteChecksExistencePermissionAndServiceOutcome() {
        AssistanceResponseDto existing = response(10L, 3L);
        when(service.findById(1L)).thenReturn(existing);
        when(service.findById(2L)).thenReturn(null);
        when(service.findById(3L)).thenReturn(existing);
        when(service.delete(1L)).thenReturn(true);
        when(service.delete(3L)).thenReturn(false);

        assertEquals(HttpStatus.NOT_FOUND, controller.delete(2L, 99L, "ADMIN").getStatusCode());
        assertEquals(HttpStatus.NO_CONTENT, controller.delete(1L, 99L, "ADMIN").getStatusCode());
        assertEquals(HttpStatus.NOT_FOUND, controller.delete(3L, 99L, "ADMIN").getStatusCode());
        assertEquals(HttpStatus.FORBIDDEN, controller.delete(1L, null, "Profesor").getStatusCode());
        verify(service, never()).delete(2L);
    }

    @Test
    void userSearchReturnsResultsOrNotFound() throws Exception {
        List<AssistanceResponseDto> results = List.of(response(10L, 3L));
        when(service.findByUserId(10L)).thenReturn(results);
        when(service.findByUserId(11L)).thenThrow(new Exception("missing user"));

        assertEquals(results, controller.findByUserId(10L).getBody());
        assertEquals(HttpStatus.NOT_FOUND, controller.findByUserId(11L).getStatusCode());
    }

    @Test
    void classSearchChecksPermissionAndHandlesServiceErrors() throws Exception {
        List<AssistanceResponseDto> results = List.of(response(10L, 3L));
        when(service.findByClasseId(3L)).thenReturn(results);
        when(service.findByClasseId(4L)).thenThrow(new Exception("missing class"));

        assertEquals(HttpStatus.FORBIDDEN, controller.findByClasseId(3L, null, "Profesor").getStatusCode());
        assertEquals(results, controller.findByClasseId(3L, 99L, "ADMIN").getBody());
        assertEquals(HttpStatus.NOT_FOUND, controller.findByClasseId(4L, 99L, "ADMIN").getStatusCode());
    }
}
