package duoc.cl.ms_classes.controller;

import duoc.cl.ms_classes.dto.ClasseRequestDto;
import duoc.cl.ms_classes.dto.ClasseResponseDto;
import duoc.cl.ms_classes.service.ClasseService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ClasseControllerTest {

    @Mock
    private ClasseService service;

    @InjectMocks
    private ClasseController controller;

    @Test
    void exposesClassReadCreateUpdateAndDeleteResults() {
        ClasseResponseDto classe = mock(ClasseResponseDto.class);
        ClasseRequestDto request = new ClasseRequestDto();
        List<ClasseResponseDto> classes = List.of(classe);
        when(service.findAll()).thenReturn(classes);
        when(service.findById(1L)).thenReturn(classe);
        when(service.findById(2L)).thenReturn(null);
        when(service.create(request)).thenReturn(classe);
        when(service.update(1L, request)).thenReturn(classe);
        when(service.update(2L, request)).thenReturn(null);
        when(service.deleteById(1L)).thenReturn(true);
        when(service.deleteById(2L)).thenReturn(false);

        assertSame(classes, controller.findAll().getBody());
        assertSame(classe, controller.findById(1L).getBody());
        assertEquals(HttpStatus.NOT_FOUND, controller.findById(2L).getStatusCode());
        ResponseEntity<ClasseResponseDto> created = controller.create(request);
        assertEquals(HttpStatus.CREATED, created.getStatusCode());
        assertSame(classe, created.getBody());
        assertSame(classe, controller.update(1L, request).getBody());
        assertEquals(HttpStatus.NOT_FOUND, controller.update(2L, request).getStatusCode());
        assertEquals(HttpStatus.NO_CONTENT, controller.delete(1L).getStatusCode());
        assertEquals(HttpStatus.NOT_FOUND, controller.delete(2L).getStatusCode());
    }

    @Test
    void reservesAndReleasesSpotsOnlyForExistingClasses() {
        ClasseResponseDto classe = mock(ClasseResponseDto.class);
        when(service.findById(1L)).thenReturn(null);
        when(service.findById(2L)).thenReturn(classe);
        when(service.findById(3L)).thenReturn(classe);
        when(service.reserveSpot(2L)).thenReturn(true);
        when(service.reserveSpot(3L)).thenReturn(false);
        when(service.releaseSpot(2L)).thenReturn(true);
        when(service.releaseSpot(3L)).thenReturn(false);

        assertEquals(HttpStatus.NOT_FOUND, controller.reserveSpot(1L).getStatusCode());
        assertEquals(HttpStatus.NO_CONTENT, controller.reserveSpot(2L).getStatusCode());
        assertEquals(HttpStatus.CONFLICT, controller.reserveSpot(3L).getStatusCode());
        assertEquals(HttpStatus.NOT_FOUND, controller.releaseSpot(1L).getStatusCode());
        assertEquals(HttpStatus.NO_CONTENT, controller.releaseSpot(2L).getStatusCode());
        assertEquals(HttpStatus.CONFLICT, controller.releaseSpot(3L).getStatusCode());

        verify(service, never()).reserveSpot(1L);
        verify(service, never()).releaseSpot(1L);
    }
}
