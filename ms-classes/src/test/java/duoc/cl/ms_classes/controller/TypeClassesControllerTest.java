package duoc.cl.ms_classes.controller;

import duoc.cl.ms_classes.dto.TypeClassesRequestDto;
import duoc.cl.ms_classes.dto.TypeClassesResponseDto;
import duoc.cl.ms_classes.service.TypeClassesService;
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
class TypeClassesControllerTest {

    @Mock
    private TypeClassesService service;

    @InjectMocks
    private TypeClassesController controller;

    @Test
    void managesTypeClassResponsesAcrossCrudOperations() {
        TypeClassesRequestDto request = new TypeClassesRequestDto();
        TypeClassesResponseDto type = mock(TypeClassesResponseDto.class);
        List<TypeClassesResponseDto> types = List.of(type);
        when(service.findAll()).thenReturn(types);
        when(service.findById(1L)).thenReturn(type);
        when(service.create(request)).thenReturn(type);
        when(service.update(1L, request)).thenReturn(type);
        when(service.update(2L, request)).thenReturn(null);
        when(service.deleteById(1L)).thenReturn(true);
        when(service.deleteById(2L)).thenReturn(false);

        assertSame(types, controller.findAll().getBody());
        assertSame(type, controller.findById(1L).getBody());
        ResponseEntity<TypeClassesResponseDto> created = controller.save(request);
        assertEquals(HttpStatus.CREATED, created.getStatusCode());
        assertSame(type, created.getBody());
        assertSame(type, controller.update(1L, request).getBody());
        assertEquals(HttpStatus.NOT_FOUND, controller.update(2L, request).getStatusCode());
        assertEquals(HttpStatus.NO_CONTENT, controller.delete(1L).getStatusCode());
        assertEquals(HttpStatus.NOT_FOUND, controller.delete(2L).getStatusCode());
    }
}
