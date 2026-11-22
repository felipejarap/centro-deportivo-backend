package cl.duoc.ms_auth.service.impl;

import cl.duoc.ms_auth.dto.*;
import cl.duoc.ms_auth.exception.UsernameAlreadyExistsException;
import cl.duoc.ms_auth.model.Credencial;
import cl.duoc.ms_auth.reporsitory.CredencialRepository;
import cl.duoc.ms_auth.security.JwtUtil;
import cl.duoc.ms_auth.service.api.UsuarioClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

// Habilita el uso de Mockito en JUnit 5 para simular objetos sin levantar Spring
@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private CredencialRepository repository;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private UsuarioClient usuarioClient;

    @InjectMocks
    private AuthServiceImpl service;

    private Credencial credencialEntity;
    private AuthRequestDto loginRequest;
    private StudentRegistrationRequestDto registrationRequest;

    @BeforeEach
    void setUp() {
        credencialEntity = new Credencial();
        credencialEntity.setIdCredencial(1L);
        credencialEntity.setUsername("felipe.perez");
        credencialEntity.setPassword("hash123");
        credencialEntity.setIdUser(10L);
        credencialEntity.setActive(true);

        loginRequest = new AuthRequestDto();
        loginRequest.setUsername("felipe.perez");
        loginRequest.setPassword("password123");

        registrationRequest = new StudentRegistrationRequestDto();
        registrationRequest.setEmail("felipe.perez@example.com");
        registrationRequest.setPaternalSurname("Perez");
        registrationRequest.setMaternalSurname("Soto");
        registrationRequest.setPassword("password123");
    }

    // =========================================================================
    // PRUEBAS: METODO login() (Camino Feliz y Errores)
    // =========================================================================

    /**
     * LOGIN EXITOSO: Evalúa que al ingresar credenciales correctas, el sistema valide
     * la clave hash, genere un token JWT con el rol correspondiente y devuelva un mensaje de éxito.
     */
    @Test
    void givenValidCredentials_whenLogin_thenReturnSuccessDto() {
        // GIVEN
        when(repository.findByUsername("felipe.perez")).thenReturn(Optional.of(credencialEntity));
        when(passwordEncoder.matches("password123", "hash123")).thenReturn(true);
        when(usuarioClient.findById(10L)).thenReturn(activeClientUser());
        when(jwtUtil.generateToken("felipe.perez", 10L, "Cliente")).thenReturn("jwt-token");
        when(jwtUtil.generateRefreshToken("felipe.perez", 10L, "Cliente")).thenReturn("refresh-token");

        // WHEN
        AuthResponseDto response = service.login(loginRequest);

        // THEN
        assertNotNull(response);
        assertEquals("Login exitoso", response.getMensaje());
        assertEquals("jwt-token", response.getToken());
        assertEquals("refresh-token", response.getRefreshToken());
        assertEquals("Cliente", response.getRol());
    }

    @Test
    void refreshTokenRotatesTokensForActiveCredentialAndUser() {
        credencialEntity.setRefreshTokenId("old-token-id");
        when(jwtUtil.validateRefreshToken("refresh-token")).thenReturn(true);
        when(jwtUtil.extractTokenId("refresh-token")).thenReturn("old-token-id");
        when(jwtUtil.extractIdUsuario("refresh-token")).thenReturn(10L);
        when(jwtUtil.extractUsername("refresh-token")).thenReturn("felipe.perez");
        when(repository.findByIdUser(10L)).thenReturn(Optional.of(credencialEntity));
        when(usuarioClient.findById(10L)).thenReturn(activeClientUser());
        when(jwtUtil.generateToken("felipe.perez", 10L, "Cliente")).thenReturn("new-access");
        when(jwtUtil.generateRefreshToken("felipe.perez", 10L, "Cliente")).thenReturn("new-refresh");
        when(jwtUtil.extractTokenId("new-refresh")).thenReturn("new-token-id");

        AuthResponseDto response = service.refreshToken("refresh-token");

        assertNotNull(response);
        assertEquals("new-access", response.getToken());
        assertEquals("new-refresh", response.getRefreshToken());
        assertEquals(10L, response.getIdUsuario());
        assertEquals("new-token-id", credencialEntity.getRefreshTokenId());
        assertNull(service.refreshToken("refresh-token"));
    }

    @Test
    void refreshTokenRejectsInactiveCredential() {
        credencialEntity.setActive(false);
        when(jwtUtil.validateRefreshToken("refresh-token")).thenReturn(true);
        when(jwtUtil.extractIdUsuario("refresh-token")).thenReturn(10L);
        when(jwtUtil.extractUsername("refresh-token")).thenReturn("felipe.perez");
        when(repository.findByIdUser(10L)).thenReturn(Optional.of(credencialEntity));

        assertNull(service.refreshToken("refresh-token"));
        verifyNoInteractions(usuarioClient);
    }

    /**
     * LOGIN USUARIO INEXISTENTE: Si el nombre de usuario no existe en la base de datos de credenciales,
     * el servicio debe interceptarlo retornando un mensaje controlado de credenciales inválidas sin token.
     */
    @Test
    void givenNonExistingUser_whenLogin_thenReturnInvalidCredentials() {
        // GIVEN
        when(repository.findByUsername("felipe.perez")).thenReturn(Optional.empty());

        // WHEN
        AuthResponseDto response = service.login(loginRequest);

        // THEN
        assertNotNull(response);
        assertEquals("Credenciales inválidas", response.getMensaje());
        assertNull(response.getToken());
    }

    /**
     * LOGIN CON CLAVE ERRÓNEA: Si el usuario existe pero la contraseña no coincide con el hash guardado,
     * el sistema frena la sesión devolviendo credenciales inválidas por seguridad.
     */
    @Test
    void givenWrongPassword_whenLogin_thenReturnInvalidCredentials() {
        // GIVEN
        when(repository.findByUsername("felipe.perez")).thenReturn(Optional.of(credencialEntity));
        when(passwordEncoder.matches("password123", "hash123")).thenReturn(false);

        // WHEN
        AuthResponseDto response = service.login(loginRequest);

        // THEN
        assertNotNull(response);
        assertEquals("Credenciales inválidas", response.getMensaje());
        assertNull(response.getToken());
    }

    // =========================================================================
    // PRUEBAS: REGISTRO CONJUNTO DE ALUMNO Y CREDENCIALES
    // =========================================================================

    /**
     * REGISTRO EXITOSO: Comprueba que al enviar una solicitud de credenciales válida, se codifique la contraseña,
     * se guarde en el repositorio local y devuelva los datos del nuevo usuario.
     */
    @Test
    void givenValidRequest_whenRegisterStudent_thenCreateUserAndCredential() {
        when(repository.findByUsername("felipe.perez@example.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("password123")).thenReturn("hash123");
        UsuarioResponseDto user = activeClientUser();
        user.setIdUser(10L);
        when(usuarioClient.registerStudent(any(UserRegistrationRequestDto.class))).thenReturn(user);
        when(repository.save(any(Credencial.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CredencialResponseDto response = service.registerStudent(registrationRequest);

        assertNotNull(response);
        assertEquals("felipe.perez@example.com", response.getUsername());
        assertEquals(10L, response.getIdUsuario());
        verify(usuarioClient).registerStudent(any(UserRegistrationRequestDto.class));
        verify(repository, times(1)).save(any(Credencial.class));
    }

    @Test
    void rollsBackStudentProfileWhenCredentialCreationFails() {
        when(repository.findByUsername("felipe.perez@example.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("password123")).thenReturn("hash123");
        UsuarioResponseDto user = activeClientUser();
        user.setIdUser(10L);
        when(usuarioClient.registerStudent(any(UserRegistrationRequestDto.class))).thenReturn(user);
        when(repository.save(any(Credencial.class))).thenThrow(new RuntimeException("database unavailable"));

        assertThrows(RuntimeException.class, () -> service.registerStudent(registrationRequest));

        verify(usuarioClient).delete(10L);
    }

    private UsuarioResponseDto activeClientUser() {
        UsuarioResponseDto user = new UsuarioResponseDto();
        user.setIdUser(10L);
        user.setActive(true);
        user.setTypeUser(new TypeUserResponseDto(1L, "Cliente"));
        return user;
    }

    @Test
    void rejectsEmailAlreadyUsedForCredentialsBeforeCreatingUser() {
        when(repository.findByUsername("felipe.perez@example.com")).thenReturn(Optional.of(credencialEntity));

        assertThrows(UsernameAlreadyExistsException.class, () -> service.registerStudent(registrationRequest));
        verify(usuarioClient, never()).registerStudent(any(UserRegistrationRequestDto.class));
        verify(repository, never()).save(any(Credencial.class));
    }

    // =========================================================================
    // PRUEBAS: METODOS findAll() Y findById()
    // =========================================================================

    /**
     * BUSCAR TODOS: Verifica que el servicio acceda al repositorio, recupere la colección
     * completa de credenciales y la exponga en una lista mapeada a DTOs.
     */
    @Test
    void givenExistingRecords_whenFindAll_thenReturnList() {
        // GIVEN
        when(repository.findAll()).thenReturn(List.of(credencialEntity));

        // WHEN
        List<CredencialResponseDto> result = service.findAll();

        // THEN
        assertNotNull(result);
        assertEquals(1, result.size());
    }

    /**
     * BUSCAR POR ID EXISTENTE: Valida que al proveer una clave primaria que sí se encuentra en el sistema,
     * el servicio extraiga el DTO y verifique que la cuenta se encuentra con estado activo.
     */
    @Test
    void givenExistingId_whenFindById_thenReturnDto() {
        // GIVEN
        when(repository.findById(1L)).thenReturn(Optional.of(credencialEntity));

        // WHEN
        CredencialResponseDto result = service.findById(1L);

        // THEN
        assertNotNull(result);
        assertTrue(result.getActivo());
    }

    // =========================================================================
    // PRUEBAS: METODO delete()
    // =========================================================================

    /**
     * ELIMINAR EXITOSO: Comprueba que si la clave primaria existe localmente, se proceda a ejecutar
     * la remoción física del registro en el repositorio devolviendo true.
     */
    @Test
    void givenExistingId_whenDelete_thenReturnTrue() {
        // GIVEN
        when(repository.existsById(1L)).thenReturn(true);
        doNothing().when(repository).deleteById(1L);

        // WHEN
        boolean result = service.delete(1L);

        // THEN
        assertTrue(result);
        verify(repository, times(1)).deleteById(1L);
    }

    @Test
    void loginRejectsInactiveCredentialAndIncompleteUserProfiles() {
        credencialEntity.setActive(false);
        when(repository.findByUsername("felipe.perez")).thenReturn(Optional.of(credencialEntity));
        assertEquals("Credenciales inválidas", service.login(loginRequest).getMensaje());

        credencialEntity.setActive(true);
        when(usuarioClient.findById(10L)).thenReturn(
                null,
                inactiveClientUser(),
                userWithoutRole(),
                userWithUnnamedRole());
        when(repository.findByUsername("felipe.perez")).thenReturn(Optional.of(credencialEntity));
        when(passwordEncoder.matches("password123", "hash123")).thenReturn(true);

        for (int attempt = 0; attempt < 4; attempt++) {
            assertEquals("Credenciales inválidas", service.login(loginRequest).getMensaje());
        }
        verify(jwtUtil, never()).generateToken(any(), any(), any());
    }

    @Test
    void refreshRejectsInvalidTokensAndMissingIdentityClaims() {
        when(jwtUtil.validateRefreshToken("invalid")).thenReturn(false);
        assertNull(service.refreshToken("invalid"));

        when(jwtUtil.validateRefreshToken("missing-id")).thenReturn(true);
        when(jwtUtil.extractIdUsuario("missing-id")).thenReturn(null);
        assertNull(service.refreshToken("missing-id"));

        when(jwtUtil.validateRefreshToken("missing-username")).thenReturn(true);
        when(jwtUtil.extractIdUsuario("missing-username")).thenReturn(10L);
        when(jwtUtil.extractUsername("missing-username")).thenReturn(null);
        assertNull(service.refreshToken("missing-username"));
        verifyNoInteractions(usuarioClient);
    }

    @Test
    void refreshRejectsUnknownCredentialAndMismatchedUsernameOrTokenId() {
        when(jwtUtil.validateRefreshToken(any())).thenReturn(true);
        when(jwtUtil.extractIdUsuario("unknown")).thenReturn(10L);
        when(jwtUtil.extractUsername("unknown")).thenReturn("felipe.perez");
        when(repository.findByIdUser(10L)).thenReturn(Optional.empty());
        assertNull(service.refreshToken("unknown"));

        when(jwtUtil.extractIdUsuario("wrong-user")).thenReturn(10L);
        when(jwtUtil.extractUsername("wrong-user")).thenReturn("someone-else");
        when(repository.findByIdUser(10L)).thenReturn(Optional.of(credencialEntity));
        assertNull(service.refreshToken("wrong-user"));

        credencialEntity.setRefreshTokenId("stored-token-id");
        when(jwtUtil.extractIdUsuario("wrong-token")).thenReturn(10L);
        when(jwtUtil.extractUsername("wrong-token")).thenReturn("felipe.perez");
        when(jwtUtil.extractTokenId("wrong-token")).thenReturn("different-token-id");
        assertNull(service.refreshToken("wrong-token"));
        verifyNoInteractions(usuarioClient);
    }

    @Test
    void refreshRejectsInactiveOrIncompleteUserProfiles() {
        credencialEntity.setRefreshTokenId("current-token");
        when(jwtUtil.validateRefreshToken(any())).thenReturn(true);
        when(jwtUtil.extractIdUsuario(any())).thenReturn(10L);
        when(jwtUtil.extractUsername(any())).thenReturn("felipe.perez");
        when(jwtUtil.extractTokenId(any())).thenReturn("current-token");
        when(repository.findByIdUser(10L)).thenReturn(Optional.of(credencialEntity));
        when(usuarioClient.findById(10L)).thenReturn(
                inactiveClientUser(),
                userWithoutRole(),
                userWithUnnamedRole());

        for (int attempt = 0; attempt < 3; attempt++) {
            assertNull(service.refreshToken("refresh-token"));
        }
        verify(repository, never()).save(any(Credencial.class));
    }

    @Test
    void rejectsUnconfirmedAndInvalidStudentProfilesAndCompensatesInvalidProfile() {
        when(repository.findByUsername("felipe.perez@example.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("password123")).thenReturn("hash123");
        when(usuarioClient.registerStudent(any(UserRegistrationRequestDto.class)))
                .thenReturn(null, userWithoutId(), inactiveClientUser());
        assertThrows(ResponseStatusException.class, () -> service.registerStudent(registrationRequest));
        assertThrows(ResponseStatusException.class, () -> service.registerStudent(registrationRequest));
        assertThrows(ResponseStatusException.class, () -> service.registerStudent(registrationRequest));
        verify(usuarioClient).delete(10L);
        verify(repository, never()).save(any(Credencial.class));
    }

    @Test
    void reportsWhenCompensationOfInvalidStudentProfileFails() {
        when(repository.findByUsername("felipe.perez@example.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("password123")).thenReturn("hash123");
        when(usuarioClient.registerStudent(any(UserRegistrationRequestDto.class))).thenReturn(inactiveClientUser());
        doThrow(new IllegalStateException("users service unavailable")).when(usuarioClient).delete(10L);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class, () -> service.registerStudent(registrationRequest));

        assertEquals(500, exception.getStatusCode().value());
    }

    @Test
    void adminRegistrationValidatesUserAndMapsCredential() {
        CredencialRequestDto request = new CredencialRequestDto(
                "admin-user", "secret123", 10L);
        when(repository.findByUsername("admin-user")).thenReturn(Optional.empty());
        when(usuarioClient.findById(10L)).thenReturn(activeClientUser());
        when(passwordEncoder.encode("secret123")).thenReturn("encoded");
        when(repository.save(any(Credencial.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CredencialResponseDto response = service.registerForAdmin(request);

        assertEquals("admin-user", response.getUsername());
        assertEquals(10L, response.getIdUsuario());
        assertTrue(response.getActivo());
        assertEquals("encoded", captureSavedCredential().getPassword());
    }

    @Test
    void adminRegistrationRejectsDuplicateAndMissingUsers() {
        CredencialRequestDto request = new CredencialRequestDto(
                "admin-user", "secret123", 10L);
        when(repository.findByUsername("admin-user")).thenReturn(Optional.of(credencialEntity));
        assertThrows(UsernameAlreadyExistsException.class, () -> service.registerForAdmin(request));

        when(repository.findByUsername("new-admin")).thenReturn(Optional.empty());
        request.setUsername("new-admin");
        when(usuarioClient.findById(10L)).thenReturn(null, inactiveClientUser());
        assertThrows(org.springframework.security.core.userdetails.UsernameNotFoundException.class,
                () -> service.registerForAdmin(request));
        assertThrows(org.springframework.security.core.userdetails.UsernameNotFoundException.class,
                () -> service.registerForAdmin(request));
        verify(repository, never()).save(any(Credencial.class));
    }

    @Test
    void findByIdAndDeleteReturnEmptyResultsWhenCredentialDoesNotExist() {
        when(repository.findById(2L)).thenReturn(Optional.empty());
        when(repository.existsById(2L)).thenReturn(false);

        assertNull(service.findById(2L));
        assertFalse(service.delete(2L));
        verify(repository, never()).deleteById(2L);
    }

    @Test
    void passwordChangeRequiresValidCredentialAndRotatesStoredPassword() {
        when(repository.findByIdUser(11L)).thenReturn(Optional.empty());
        assertFalse(service.changePassword(11L, "old", "new-pass"));

        Credencial inactive = new Credencial();
        inactive.setActive(false);
        when(repository.findByIdUser(12L)).thenReturn(Optional.of(inactive));
        assertFalse(service.changePassword(12L, "old", "new-pass"));

        when(repository.findByIdUser(10L)).thenReturn(Optional.of(credencialEntity));
        when(passwordEncoder.matches("wrong", "hash123")).thenReturn(false);
        assertFalse(service.changePassword(10L, "wrong", "new-pass"));

        credencialEntity.setRefreshTokenId("previous-refresh");
        when(passwordEncoder.matches("password123", "hash123")).thenReturn(true);
        when(passwordEncoder.encode("new-pass")).thenReturn("new-hash");
        when(repository.save(credencialEntity)).thenReturn(credencialEntity);
        assertTrue(service.changePassword(10L, "password123", "new-pass"));
        assertEquals("new-hash", credencialEntity.getPassword());
        assertNull(credencialEntity.getRefreshTokenId());
    }

    private Credencial captureSavedCredential() {
        var captor = org.mockito.ArgumentCaptor.forClass(Credencial.class);
        verify(repository).save(captor.capture());
        return captor.getValue();
    }

    private UsuarioResponseDto inactiveClientUser() {
        UsuarioResponseDto user = activeClientUser();
        user.setActive(false);
        return user;
    }

    private UsuarioResponseDto userWithoutId() {
        UsuarioResponseDto user = activeClientUser();
        user.setIdUser(null);
        return user;
    }

    private UsuarioResponseDto userWithoutRole() {
        UsuarioResponseDto user = activeClientUser();
        user.setTypeUser(null);
        return user;
    }

    private UsuarioResponseDto userWithUnnamedRole() {
        UsuarioResponseDto user = activeClientUser();
        user.setTypeUser(new TypeUserResponseDto(1L, null));
        return user;
    }
}
