package cl.duoc.MS_Usuarios.controller;

import cl.duoc.MS_Usuarios.dto.StudentRegistrationRequestDto;
import cl.duoc.MS_Usuarios.dto.UserRequestDto;
import cl.duoc.MS_Usuarios.dto.UserResponseDto;
import cl.duoc.MS_Usuarios.service.UserService;
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
class UserControllerTest {

    @Mock
    private UserService service;

    @InjectMocks
    private UserController controller;

    @Test
    void listsUsersAndFiltersByRole() {
        UserResponseDto user = mock(UserResponseDto.class);
        List<UserResponseDto> users = List.of(user);
        when(service.findAll()).thenReturn(users);
        when(service.findByTypeUserId(7L)).thenReturn(users);

        assertSame(users, controller.findAll().getBody());
        assertSame(users, controller.findByTypeUserId(7L).getBody());

        verify(service).findAll();
        verify(service).findByTypeUserId(7L);
    }

    @Test
    void returnsNotFoundForMissingUserAndUpdate() {
        UserResponseDto user = mock(UserResponseDto.class);
        UserRequestDto request = new UserRequestDto();
        when(service.findById(1L)).thenReturn(user);
        when(service.findById(2L)).thenReturn(null);
        when(service.update(1L, request)).thenReturn(user);
        when(service.update(2L, request)).thenReturn(null);

        assertSame(user, controller.findById(1L).getBody());
        assertEquals(HttpStatus.NOT_FOUND, controller.findById(2L).getStatusCode());
        assertSame(user, controller.update(1L, request).getBody());
        assertEquals(HttpStatus.NOT_FOUND, controller.update(2L, request).getStatusCode());
    }

    @Test
    void createsUsersAndRegistersStudents() {
        UserResponseDto user = mock(UserResponseDto.class);
        UserRequestDto request = new UserRequestDto();
        StudentRegistrationRequestDto registration = new StudentRegistrationRequestDto();
        when(service.create(request)).thenReturn(user);
        when(service.registerStudent(registration)).thenReturn(user);

        ResponseEntity<UserResponseDto> created = controller.create(request);
        ResponseEntity<UserResponseDto> registered = controller.registerStudent(registration);

        assertEquals(HttpStatus.CREATED, created.getStatusCode());
        assertSame(user, created.getBody());
        assertEquals(HttpStatus.CREATED, registered.getStatusCode());
        assertSame(user, registered.getBody());
        verify(service).create(request);
        verify(service).registerStudent(registration);
    }

    @Test
    void updatesProfileAndReportsDeleteOutcome() {
        UserRequestDto request = new UserRequestDto();
        UserResponseDto user = mock(UserResponseDto.class);
        when(service.updateProfile(1L, request)).thenReturn(user);
        when(service.updateProfile(2L, request)).thenReturn(null);
        when(service.delete(1L)).thenReturn(true);
        when(service.delete(2L)).thenReturn(false);

        assertSame(user, controller.updateProfile(1L, request).getBody());
        assertEquals(HttpStatus.NOT_FOUND, controller.updateProfile(2L, request).getStatusCode());
        assertEquals(HttpStatus.NO_CONTENT, controller.delete(1L).getStatusCode());
        assertEquals(HttpStatus.NOT_FOUND, controller.delete(2L).getStatusCode());
    }
}
