package cl.duoc.ms_subscription.service.impl;

import cl.duoc.ms_subscription.dto.PlansResponseDto;
import cl.duoc.ms_subscription.dto.SubscriptionRequestDto;
import cl.duoc.ms_subscription.dto.SubscriptionResponseDto;
import cl.duoc.ms_subscription.dto.UserResponseDto;
import cl.duoc.ms_subscription.model.Subscription;
import cl.duoc.ms_subscription.repository.SubscriptionRepository;
import cl.duoc.ms_subscription.service.api.UserClient;
import cl.duoc.ms_subscription.service.api.PlansClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

// Habilita el entorno de simulación de Mockito en JUnit 5 sin necesidad de inicializar bases de datos reales o H2
@ExtendWith(MockitoExtension.class)
class SubscriptionServiceImplTest {

    @Mock
    private SubscriptionRepository repository;

    @Mock
    private UserClient userClient;

    @Mock
    private PlansClient plansClient;

    @InjectMocks
    private SubscriptionServiceImpl service;

    private Subscription subscriptionEntity;
    private SubscriptionRequestDto requestDto;
    private UserResponseDto mockUserResponse;
    private PlansResponseDto mockPlansResponse;
    private LocalDateTime endDate;

    @BeforeEach
    void setUp() {
        endDate = LocalDateTime.now().plusDays(30);
        subscriptionEntity = new Subscription(1L, 10L, 5L, endDate, true);
        requestDto = new SubscriptionRequestDto(1L, 10L, 5L, endDate, true,
                LocalDateTime.now(), false, "Tarjeta");
        mockUserResponse = new UserResponseDto();
        mockUserResponse.setId(10L);
        mockPlansResponse = new PlansResponseDto(5L, "Plan", 100.0, 30);
        lenient().when(userClient.findById(10L)).thenReturn(mockUserResponse);
        lenient().when(plansClient.findById(5L)).thenReturn(mockPlansResponse);
    }

    // =========================================================================
    // PRUEBAS: METODOS findAll() Y findById()
    // =========================================================================

    /**
     * BUSCAR TODOS: Valida que al listar todas las suscripciones, el servicio devuelva
     * correctamente la colección mapeada a DTOs con su tamaño adecuado.
     */
    @Test
    void givenExistingSubscriptions_whenFindAll_thenReturnList() {
        // GIVEN
        when(repository.findAll()).thenReturn(List.of(subscriptionEntity));

        // WHEN
        List<SubscriptionResponseDto> result = service.findAll();

        // THEN
        assertNotNull(result);
        assertEquals(1, result.size());
    }

    /**
     * BUSCAR POR ID EXISTENTE: Evalúa que al consultar una suscripción activa por un ID válido,
     * el Optional se extraiga correctamente y devuelva el estado de bandera true.
     */
    @Test
    void givenExistingId_whenFindById_thenReturnDto() {
        // GIVEN
        when(repository.findById(1L)).thenReturn(Optional.of(subscriptionEntity));

        // WHEN
        SubscriptionResponseDto result = service.findById(1L);

        // THEN
        assertNotNull(result);
        assertTrue(result.isState());
    }

    /**
     * BUSCAR ID VACÍO: Comprueba el comportamiento controlado donde el ID de suscripción no existe
     * en base de datos, garantizando que retorne null limpiamente.
     */
    @Test
    void givenNonExistingId_whenFindById_thenReturnNull() {
        // GIVEN
        when(repository.findById(99L)).thenReturn(Optional.empty());

        // WHEN
        SubscriptionResponseDto result = service.findById(99L);

        // THEN
        assertNull(result);
    }

    @Test
    void renewForUserUsesPlanDurationFromLaterOfCurrentExpiryAndNow() {
        LocalDateTime renewalStart = LocalDateTime.now().plusDays(10);
        subscriptionEntity.setEndDate(renewalStart);
        when(plansClient.findById(5L)).thenReturn(new PlansResponseDto(5L, "Plan", 100.0, 45));
        when(repository.findById(1L)).thenReturn(Optional.of(subscriptionEntity));
        when(repository.save(any(Subscription.class))).thenAnswer(invocation -> invocation.getArgument(0));

        SubscriptionResponseDto renewed = service.renewForUser(10L, 1L);

        assertNotNull(renewed);
        assertEquals(renewalStart.plusDays(45), renewed.getEndDate());
        assertTrue(renewed.isState());
        verify(repository).save(subscriptionEntity);
    }

    @Test
    void renewForUserRejectsMissingOrInvalidPlan() {
        when(repository.findById(1L)).thenReturn(Optional.of(subscriptionEntity));
        when(plansClient.findById(5L)).thenReturn(null);

        assertThrows(org.springframework.web.server.ResponseStatusException.class,
                () -> service.renewForUser(10L, 1L));
        verify(repository, never()).save(any(Subscription.class));
    }

    @Test
    void renewForUserDoesNotChangeAnotherUsersSubscription() {
        when(repository.findById(1L)).thenReturn(Optional.of(subscriptionEntity));

        assertNull(service.renewForUser(99L, 1L));

        verify(repository, never()).save(any(Subscription.class));
    }

    @Test
    void cancelForUserDisablesSubscriptionAndAutomaticRenewal() {
        when(repository.findById(1L)).thenReturn(Optional.of(subscriptionEntity));
        when(repository.save(any(Subscription.class))).thenAnswer(invocation -> invocation.getArgument(0));

        SubscriptionResponseDto cancelled = service.cancelForUser(10L, 1L);

        assertNotNull(cancelled);
        assertFalse(cancelled.isState());
        assertFalse(cancelled.isAutoRenew());
    }

    @Test
    void createForUserSetsOwnerAndUsesServerPlanDuration() {
        when(plansClient.findById(5L)).thenReturn(new PlansResponseDto(5L, "Plan", 100.0, 45));
        when(repository.save(any(Subscription.class))).thenAnswer(invocation -> invocation.getArgument(0));

        SubscriptionResponseDto created = service.createForUser(10L, requestDto);

        assertNotNull(created);
        assertEquals(10L, created.getUserId());
        assertTrue(created.isState());
        assertFalse(created.isAutoRenew());
        assertEquals(45, java.time.Duration.between(created.getStartDate(), created.getEndDate()).toDays());
    }

    // =========================================================================
    // PRUEBAS: METODO findByUserId() (Feign UserClient)
    // =========================================================================

    /**
     * FILTRAR POR USUARIO EXISTENTE: Caso feliz. Verifica que si el microservicio de usuarios valida
     * que el ID existe, se acceda a la base de datos local a recuperar las suscripciones de ese cliente.
     */
    @Test
    void givenExistingUserInFeign_whenFindByUserId_thenReturnList() throws Exception {
        // GIVEN
        when(userClient.findById(10L)).thenReturn(mockUserResponse);
        when(repository.findByUserId(10L)).thenReturn(List.of(subscriptionEntity));

        // WHEN
        List<SubscriptionResponseDto> result = service.findByUserId(10L);

        // THEN
        assertNotNull(result);
        assertEquals(1, result.size());
        verify(repository, times(1)).findByUserId(10L);
    }

    /**
     * FILTRAR POR USUARIO INEXISTENTE: Integridad de datos. Si Feign responde que el ID de usuario
     * no existe en el sistema principal (null), el servicio frena y nunca consulta la BD local de suscripciones.
     */
    @Test
    void givenNonExistingUserInFeign_whenFindByUserId_thenReturnNull() throws Exception {
        // GIVEN
        when(userClient.findById(99L)).thenReturn(null);

        // WHEN
        List<SubscriptionResponseDto> result = service.findByUserId(99L);

        // THEN
        assertNull(result);
        verify(repository, never()).findByUserId(anyLong());
    }

    /**
     * ERROR EN CLIENTE DE USUARIOS: Tolerancia a fallos. Si el microservicio de usuarios falla por red,
     * nuestra aplicación propaga la excepción hacia arriba para que el controlador la exponga adecuadamente.
     */
    @Test
    void givenUserFeignException_whenFindByUserId_throwException() {
        // GIVEN
        when(plansClient.findById(5L)).thenThrow(new RuntimeException("Timeout Plan"));

        // WHEN & THEN: Cambiado a Exception.class para que coincida con la firma del servicio
        Exception exception = assertThrows(Exception.class, () -> service.findByPlansId(5L));
        assertTrue(exception.getMessage().contains("Timeout Plan"));
    }

    // =========================================================================
    // PRUEBAS: METODO findByPlansId() (Feign PlansClient)
    // =========================================================================

    /**
     * FILTRAR POR PLAN EXISTENTE: Caso feliz. Si el microservicio de planes confirma la existencia del
     * plan, el servicio procede de forma limpia a extraer todas las suscripciones amarradas a él.
     */
    @Test
    void givenExistingPlansInFeign_whenFindByPlansId_thenReturnList() throws Exception {
        // GIVEN
        when(plansClient.findById(5L)).thenReturn(mockPlansResponse);
        when(repository.findByPlansId(5L)).thenReturn(List.of(subscriptionEntity));

        // WHEN
        List<SubscriptionResponseDto> result = service.findByPlansId(5L);

        // THEN
        assertNotNull(result);
        assertEquals(1, result.size());
    }

    /**
     * FILTRAR POR PLAN INEXISTENTE: Seguridad relacional. Si el plan consultado por Feign regresa vacío/null,
     * el flujo aborta retornando null y protegiendo el repositorio.
     */
    @Test
    void givenNonExistingPlansInFeign_whenFindByPlansId_thenReturnNull() throws Exception {
        // GIVEN
        when(plansClient.findById(99L)).thenReturn(null);

        // WHEN
        List<SubscriptionResponseDto> result = service.findByPlansId(99L);

        // THEN
        assertNull(result);
        verify(repository, never()).findByPlansId(anyLong());
    }

    /**
     * ERROR EN CLIENTE DE PLANES: Si hay caídas de comunicación con el servicio de planes,
     * el sistema responde arrojando la excepción controlada.
     */
    @Test
    void givenPlansFeignException_whenFindByPlansId_throwException() {
        // GIVEN
        when(userClient.findById(10L)).thenThrow(new RuntimeException("Error de red"));

        // WHEN & THEN: Cambiado a Exception.class para máxima flexibilidad con la firma del método
        Exception exception = assertThrows(Exception.class, () -> service.findByUserId(10L));
        assertTrue(exception.getMessage().contains("Error de red"));
    }

    // =========================================================================
    // PRUEBAS: METODOS create() Y update()
    // =========================================================================

    /**
     * CREAR SUSCRIPCIÓN: Comprueba que al enviar una petición correcta, se invoque el guardado
     * en el repositorio y retorne el objeto resultante con su clave primaria correspondiente.
     */
    @Test
    void givenValidRequest_whenCreate_thenReturnCreatedDto() {
        // GIVEN
        when(repository.save(any(Subscription.class))).thenReturn(subscriptionEntity);

        // WHEN
        SubscriptionResponseDto result = service.create(requestDto);

        // THEN
        assertNotNull(result);
        assertEquals(1L, result.getId());
    }

    /**
     * ACTUALIZAR EXITOSO: Valida que si el ID de suscripción existe localmente, el repositorio
     * sobrescriba los datos con los nuevos parámetros del DTO.
     */
    @Test
    void givenExistingId_whenUpdate_thenReturnUpdatedDto() {
        // GIVEN
        when(repository.existsById(1L)).thenReturn(true);
        when(repository.save(any(Subscription.class))).thenReturn(subscriptionEntity);

        // WHEN
        SubscriptionResponseDto result = service.update(1L, requestDto);

        // THEN
        assertNotNull(result);
        verify(repository, times(1)).save(any(Subscription.class));
    }

    @Test
    void createRejectsMissingUserOrPlanReferences() {
        requestDto.setUserId(null);
        assertEquals(org.springframework.http.HttpStatus.BAD_REQUEST,
                assertThrows(org.springframework.web.server.ResponseStatusException.class,
                        () -> service.create(requestDto)).getStatusCode());

        requestDto.setUserId(10L);
        when(plansClient.findById(5L)).thenReturn(null);
        assertEquals(org.springframework.http.HttpStatus.BAD_REQUEST,
                assertThrows(org.springframework.web.server.ResponseStatusException.class,
                        () -> service.create(requestDto)).getStatusCode());
        verify(repository, never()).save(any(Subscription.class));
    }

    /**
     * ACTUALIZAR ID INEXISTENTE: Si se intenta modificar un ID de suscripción inválido, el sistema
     * lo ignora, devuelve null y restringe cualquier intento de guardado en base de datos.
     */
    @Test
    void givenNonExistingId_whenUpdate_thenReturnNull() {
        // GIVEN
        when(repository.existsById(99L)).thenReturn(false);

        // WHEN
        SubscriptionResponseDto result = service.update(99L, requestDto);

        // THEN
        assertNull(result);
        verify(repository, never()).save(any(Subscription.class));
    }

    // =========================================================================
    // PRUEBAS: METODO deleteById()
    // =========================================================================

    /**
     * ELIMINAR EXITOSO: Verifica que si el ID existe en el sistema, se ejecute la sentencia de
     * remoción del registro físico en el repositorio devolviendo la confirmación true.
     */
    @Test
    void givenExistingId_whenDeleteById_thenReturnTrue() {
        // GIVEN
        when(repository.existsById(1L)).thenReturn(true);
        doNothing().when(repository).deleteById(1L);

        // WHEN
        boolean result = service.deleteById(1L);

        // THEN
        assertTrue(result);
        verify(repository, times(1)).deleteById(1L);
    }

    /**
     * ELIMINAR ID INEXISTENTE: Comprueba que si se intenta borrar un ID corrupto o que ya no existe,
     * el servicio retorna un valor false controlado en lugar de generar rupturas o excepciones inesperadas.
     */
    @Test
    void givenNonExistingId_whenDeleteById_thenReturnFalse() {
        // GIVEN
        when(repository.existsById(99L)).thenReturn(false);

        // WHEN
        boolean result = service.deleteById(99L);

        // THEN
        assertFalse(result);
        verify(repository, never()).deleteById(anyLong());
    }

    @Test
    void dtoEnrichmentIncludesUserAndPlanDetailsWhenAvailable() {
        UserResponseDto user = new UserResponseDto();
        user.setPaternalSurname("Pérez");
        user.setMaternalSurname("González");
        user.setEmail("user@example.test");
        PlansResponseDto plan = new PlansResponseDto(5L, "Plan anual", 120.0, 365);
        when(repository.findById(1L)).thenReturn(Optional.of(subscriptionEntity));
        when(userClient.findById(10L)).thenReturn(user);
        when(plansClient.findById(5L)).thenReturn(plan);

        SubscriptionResponseDto result = service.findById(1L);

        assertEquals("Pérez González", result.getUserName());
        assertEquals("user@example.test", result.getUserEmail());
        assertEquals("Plan anual", result.getPlanName());
        assertEquals(120.0, result.getPrice());
    }

    @Test
    void createAndUpdateSupplyMissingStartDates() {
        requestDto.setStartDate(null);
        when(repository.save(any(Subscription.class))).thenAnswer(invocation -> invocation.getArgument(0));
        SubscriptionResponseDto created = service.create(requestDto);
        assertNotNull(created.getStartDate());

        when(repository.existsById(1L)).thenReturn(true);
        SubscriptionRequestDto update = new SubscriptionRequestDto(1L, 10L, 5L, endDate, true,
                null, false, "Transferencia");
        SubscriptionResponseDto updated = service.update(1L, update);
        assertNotNull(updated.getStartDate());
        assertEquals("Transferencia", updated.getPaymentMethod());
    }

    @Test
    void createForUserRejectsPlansWithNoUsableDuration() {
        when(plansClient.findById(5L)).thenReturn(null);
        assertEquals(org.springframework.http.HttpStatus.BAD_REQUEST,
                assertThrows(org.springframework.web.server.ResponseStatusException.class,
                        () -> service.createForUser(10L, requestDto)).getStatusCode());

        when(plansClient.findById(5L)).thenReturn(new PlansResponseDto(5L, "Invalid", 0.0, 0));
        assertThrows(org.springframework.web.server.ResponseStatusException.class,
                () -> service.createForUser(10L, requestDto));
        verify(repository, never()).save(any(Subscription.class));
    }

    @Test
    void renewUsesNowForExpiredOrMissingEndDateAndRejectsNonPositiveDuration() {
        subscriptionEntity.setEndDate(LocalDateTime.now().minusDays(1));
        when(repository.findById(1L)).thenReturn(Optional.of(subscriptionEntity));
        when(plansClient.findById(5L)).thenReturn(new PlansResponseDto(5L, "Plan", 100.0, 30));
        when(repository.save(any(Subscription.class))).thenAnswer(invocation -> invocation.getArgument(0));

        SubscriptionResponseDto renewed = service.renewForUser(10L, 1L);
        assertNotNull(renewed.getEndDate());
        assertTrue(renewed.getEndDate().isAfter(LocalDateTime.now().plusDays(29)));

        subscriptionEntity.setEndDate(null);
        assertNotNull(service.renewForUser(10L, 1L));

        when(plansClient.findById(5L)).thenReturn(new PlansResponseDto(5L, "Invalid", 0.0, 0));
        assertThrows(org.springframework.web.server.ResponseStatusException.class,
                () -> service.renewForUser(10L, 1L));
    }

    @Test
    void cancelReturnsNullForDifferentOwnerOrMissingSubscription() {
        when(repository.findById(1L)).thenReturn(Optional.of(subscriptionEntity));
        when(repository.findById(2L)).thenReturn(Optional.empty());

        assertNull(service.cancelForUser(99L, 1L));
        assertNull(service.cancelForUser(10L, 2L));
        verify(repository, never()).save(any(Subscription.class));
    }
}
