package duoc.cl.ms_plans.controller;

import duoc.cl.ms_plans.dto.PlansRequestDto;
import duoc.cl.ms_plans.dto.PlansResponseDto;
import duoc.cl.ms_plans.service.PlansService;
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
class PlansControllerTest {

    @Mock
    private PlansService service;

    @InjectMocks
    private PlansController controller;

    @Test
    void managesPlanResponsesAcrossCrudOperations() {
        PlansRequestDto request = new PlansRequestDto();
        PlansResponseDto plan = mock(PlansResponseDto.class);
        List<PlansResponseDto> plans = List.of(plan);
        when(service.findAll()).thenReturn(plans);
        when(service.findById(1L)).thenReturn(plan);
        when(service.findById(2L)).thenReturn(null);
        when(service.create(request)).thenReturn(plan);
        when(service.update(1L, request)).thenReturn(plan);
        when(service.update(2L, request)).thenReturn(null);
        when(service.deleteById(1L)).thenReturn(true);
        when(service.deleteById(2L)).thenReturn(false);

        assertSame(plans, controller.findAll().getBody());
        assertSame(plan, controller.findById(1L).getBody());
        assertEquals(HttpStatus.NOT_FOUND, controller.findById(2L).getStatusCode());
        ResponseEntity<PlansResponseDto> created = controller.create(request);
        assertEquals(HttpStatus.CREATED, created.getStatusCode());
        assertSame(plan, created.getBody());
        assertSame(plan, controller.update(1L, request).getBody());
        assertEquals(HttpStatus.NOT_FOUND, controller.update(2L, request).getStatusCode());
        assertEquals(HttpStatus.NO_CONTENT, controller.delete(1L).getStatusCode());
        assertEquals(HttpStatus.NOT_FOUND, controller.delete(2L).getStatusCode());

        verify(service).create(request);
    }
}
