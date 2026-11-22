package cl.duoc.MS_Usuarios.controller;

import cl.duoc.MS_Usuarios.dto.TypeUserRequestDto;
import cl.duoc.MS_Usuarios.dto.TypeUserResponseDto;
import cl.duoc.MS_Usuarios.service.TypeUserService;
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
class TypeUserControllerTest {

    @Mock
    private TypeUserService service;

    @InjectMocks
    private TypeUserController controller;

    @Test
    void listsTypesAndReturnsNotFoundForMissingType() {
        TypeUserResponseDto type = mock(TypeUserResponseDto.class);
        List<TypeUserResponseDto> types = List.of(type);
        when(service.findAll()).thenReturn(types);
        when(service.findById(1L)).thenReturn(type);
        when(service.findById(2L)).thenReturn(null);

        assertSame(types, controller.findAll().getBody());
        assertSame(type, controller.findById(1L).getBody());
        assertEquals(HttpStatus.NOT_FOUND, controller.findById(2L).getStatusCode());
    }

    @Test
    void createsUpdatesAndDeletesTypesWithExpectedStatuses() {
        TypeUserRequestDto request = new TypeUserRequestDto();
        TypeUserResponseDto type = mock(TypeUserResponseDto.class);
        when(service.create(request)).thenReturn(type);
        when(service.update(1L, request)).thenReturn(type);
        when(service.update(2L, request)).thenReturn(null);
        when(service.deleteById(1L)).thenReturn(true);
        when(service.deleteById(2L)).thenReturn(false);

        ResponseEntity<TypeUserResponseDto> created = controller.save(request);
        assertEquals(HttpStatus.CREATED, created.getStatusCode());
        assertSame(type, created.getBody());
        assertSame(type, controller.update(1L, request).getBody());
        assertEquals(HttpStatus.NOT_FOUND, controller.update(2L, request).getStatusCode());
        assertEquals(HttpStatus.NO_CONTENT, controller.delete(1L).getStatusCode());
        assertEquals(HttpStatus.NOT_FOUND, controller.delete(2L).getStatusCode());

        verify(service).create(request);
    }
}
