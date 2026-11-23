package cl.duoc.ms_reservas.service.impl;

import cl.duoc.ms_reservas.dto.*;
import cl.duoc.ms_reservas.model.Reservation;
import cl.duoc.ms_reservas.repository.ReservationRespository;
import cl.duoc.ms_reservas.service.ReservationService;
import cl.duoc.ms_reservas.service.api.ClasseClient;
import cl.duoc.ms_reservas.service.api.UserClient;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ReservationServiceImpl implements ReservationService {

    private static final Logger log = LoggerFactory.getLogger(ReservationServiceImpl.class);

    private final ReservationRespository repository;
    private final UserClient userClient;
    private final ClasseClient claseClient;

    private Reservation toEntity(ReservationRequestDto dto) {
        Reservation r = new Reservation();
        r.setIdUser(dto.getIdUser());
        r.setIdClasse(dto.getIdClasse());
        r.setIdCoach(dto.getIdCoach());
        r.setReservationDate(dto.getReservationDate());
        r.setReservationStatus(dto.getReservationStatus());
        return r;
    }

    private ReservationResponseDto toDto(Reservation entity) {
        ReservationResponseDto dto = new ReservationResponseDto();
        dto.setIdReservation(entity.getIdReservation());
        dto.setIdUser(entity.getIdUser());
        dto.setIdClasse(entity.getIdClasse());
        dto.setIdCoach(entity.getIdCoach());
        dto.setReservationDate(entity.getReservationDate());
        dto.setReservationStatus(entity.getReservationStatus());

        try {
            UserResponseDto user = userClient.findById(entity.getIdUser());
            dto.setUser(user);
            log.debug("Usuario enriquecido en respuesta: idUser={}", entity.getIdUser());
        } catch (Exception e) {
            log.warn("No se pudo obtener datos del usuario para enriquecer respuesta: idUser={}, motivo={}",
                    entity.getIdUser(), e.getMessage());
            dto.setUser(null);
        }

        if (entity.getIdCoach() != null) {
            try {
                UserResponseDto coach = userClient.findById(entity.getIdCoach());
                dto.setCoach(coach);
                log.debug("Usuario entrenador enriquecido en respuesta: idCoach={}", entity.getIdCoach());
            } catch (Exception e) {
                log.warn("No se pudo obtener datos del usuario entrenador para enriquecer respuesta: idCoach={}, motivo={}",
                        entity.getIdCoach(), e.getMessage());
                dto.setCoach(null);
            }
        }

        try {
            ClasseResponseDto classe = claseClient.findById(entity.getIdClasse());
            dto.setClasse(classe);
            log.debug("Clase enriquecida en respuesta: idClasse={}", entity.getIdClasse());
        } catch (Exception e) {
            log.warn("No se pudo obtener datos de la clase para enriquecer respuesta: idClasse={}, motivo={}",
                    entity.getIdClasse(), e.getMessage());
            dto.setClasse(null);
        }

        return dto;
    }

    @Override
    public List<ReservationResponseDto> findAll() {
        log.info("Consultando todas las reservas");
        List<ReservationResponseDto> result = repository.findAll().stream().map(this::toDto).toList();
        log.info("Total de reservas encontradas: {}", result.size());
        return result;
    }

    @Override
    public ReservationResponseDto findById(Long id) {
        log.info("Buscando reserva por id: {}", id);
        return repository.findById(id)
                .map(entity -> {
                    log.info("Reserva encontrada: idReserva={}", id);
                    return toDto(entity);
                })
                .orElseGet(() -> {
                    log.warn("Reserva no encontrada: idReserva={}", id);
                    return null;
                });
    }

    @Override
    public ReservationResponseDto create(ReservationRequestDto reservation) {
        log.info("Creando reserva: idUsuario={}, idClase={}, idEntrenador={}, fechaReserva={}, estadoReserva={}",
                reservation.getIdUser(), reservation.getIdClasse(),
                reservation.getIdCoach(), reservation.getReservationDate(), reservation.getReservationStatus());
        validateReferences(reservation, true);
        boolean reservedSpot = isConfirmed(reservation.getReservationStatus());
        if (reservedSpot) {
            rejectDuplicateConfirmedReservation(reservation.getIdUser(), reservation.getIdClasse(), null);
            reserveSpot(reservation.getIdClasse());
        }
        Reservation saved;
        try {
            saved = repository.save(toEntity(reservation));
        } catch (RuntimeException exception) {
            if (reservedSpot) {
                compensateRelease(reservation.getIdClasse(), exception);
            }
            throw exception;
        }
        ReservationResponseDto response = toDto(saved);
        log.info("Reserva creada exitosamente: idReservation={}, idUser={}",
                response.getIdReservation(), response.getIdUser());
        return response;
    }

    @Override
    public ReservationResponseDto update(Long id, ReservationRequestDto reservation) {
        log.info("Actualizando reserva: idReservation={}, idUsuario={}, idClase={}, idEntrenador={}",
                id, reservation.getIdUser(), reservation.getIdClasse(), reservation.getIdCoach());
        Reservation existing = repository.findById(id).orElse(null);
        if (existing == null) {
            log.warn("No se pudo actualizar: reserva no encontrada: idReservation={}", id);
            return null;
        }

        validateReferences(reservation, false);
        boolean previouslyReserved = isConfirmed(existing.getReservationStatus());
        boolean willBeReserved = isConfirmed(reservation.getReservationStatus());
        boolean sameActiveClass = previouslyReserved && willBeReserved
                && existing.getIdClasse().equals(reservation.getIdClasse());
        boolean allocateNewSpot = willBeReserved && !sameActiveClass;
        boolean releasePreviousSpot = previouslyReserved && !sameActiveClass;

        if (allocateNewSpot) {
            rejectDuplicateConfirmedReservation(reservation.getIdUser(), reservation.getIdClasse(), id);
            reserveSpot(reservation.getIdClasse());
        }

        Reservation entity = toEntity(reservation);
        entity.setIdReservation(id);
        Reservation saved;
        try {
            saved = repository.save(entity);
        } catch (RuntimeException exception) {
            if (allocateNewSpot) {
                compensateRelease(reservation.getIdClasse(), exception);
            }
            throw exception;
        }

        if (releasePreviousSpot) {
            claseClient.releaseSpot(existing.getIdClasse());
        }
        log.info("Reserva actualizada exitosamente: idReservation={}", id);
        return toDto(saved);
    }

    private void validateReferences(ReservationRequestDto reservation, boolean enforceRequestedCoach) {
        requireUser(reservation.getIdUser(), "Cliente", "El alumno indicado no existe o no tiene rol Cliente.");
        if (reservation.getIdClasse() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La clase indicada es obligatoria.");
        }

        ClasseResponseDto classe;
        try {
            classe = claseClient.findById(reservation.getIdClasse());
        } catch (FeignException.NotFound exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "La clase indicada no existe.", exception);
        }
        if (classe == null || !reservation.getIdClasse().equals(classe.getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La clase indicada no existe.");
        }

        Long assignedCoachId = classe.getCoachId();
        Long requestedCoachId = reservation.getIdCoach();
        if (enforceRequestedCoach && requestedCoachId != null && !requestedCoachId.equals(assignedCoachId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "El profesor indicado no corresponde a la clase.");
        }
        if (assignedCoachId != null) {
            requireUser(assignedCoachId, "Entrenador",
                    "El profesor asignado a la clase no existe o no tiene rol Entrenador.");
            reservation.setIdCoach(assignedCoachId);
        }
    }

    private UserResponseDto requireUser(Long userId, String expectedRole, String errorMessage) {
        if (userId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, errorMessage);
        }

        UserResponseDto user;
        try {
            user = userClient.findById(userId);
        } catch (FeignException.NotFound exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, errorMessage, exception);
        }

        if (user == null || !userId.equals(user.getId())
                || user.getTypeUser() == null
                || user.getTypeUser().getName() == null
                || !expectedRole.equalsIgnoreCase(user.getTypeUser().getName().trim())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, errorMessage);
        }
        return user;
    }

    @Override
    public boolean delete(Long id) {
        log.info("Eliminando reserva: idReservation={}", id);
        Reservation existing = repository.findById(id).orElse(null);
        if (existing == null) {
            log.warn("No se pudo eliminar: reserva no encontrada: idReservation={}", id);
            return false;
        }

        boolean reserved = isConfirmed(existing.getReservationStatus());
        if (reserved) {
            claseClient.releaseSpot(existing.getIdClasse());
        }
        try {
            repository.deleteById(id);
        } catch (RuntimeException exception) {
            if (reserved) {
                compensateReserve(existing.getIdClasse(), exception);
            }
            throw exception;
        }
        log.info("Reserva eliminada exitosamente: idReservation={}", id);
        return true;
    }

    private boolean isConfirmed(String status) {
        return "CONFIRMADA".equalsIgnoreCase(status);
    }

    private void rejectDuplicateConfirmedReservation(Long userId, Long classId, Long reservationId) {
        boolean alreadyConfirmed = reservationId == null
                ? repository.existsByIdUserAndIdClasseAndReservationStatusIgnoreCase(
                        userId, classId, "CONFIRMADA")
                : repository.existsByIdUserAndIdClasseAndReservationStatusIgnoreCaseAndIdReservationNot(
                        userId, classId, "CONFIRMADA", reservationId);
        if (alreadyConfirmed) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Ya existe una reserva confirmada para esta clase.");
        }
    }

    private void reserveSpot(Long classId) {
        try {
            claseClient.reserveSpot(classId);
        } catch (FeignException exception) {
            if (exception.status() == HttpStatus.CONFLICT.value()) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "No quedan cupos disponibles para esta clase", exception);
            }
            if (exception.status() == HttpStatus.NOT_FOUND.value()) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "La clase solicitada no existe", exception);
            }
            throw exception;
        }
    }

    private void compensateRelease(Long classId, RuntimeException originalException) {
        try {
            claseClient.releaseSpot(classId);
        } catch (RuntimeException compensationException) {
            originalException.addSuppressed(compensationException);
            log.error("No se pudo compensar el cupo reservado para la clase {}", classId,
                    compensationException);
        }
    }

    private void compensateReserve(Long classId, RuntimeException originalException) {
        try {
            claseClient.reserveSpot(classId);
        } catch (RuntimeException compensationException) {
            originalException.addSuppressed(compensationException);
            log.error("No se pudo restaurar el cupo liberado para la clase {}", classId,
                    compensationException);
        }
    }

    @Override
    public List<ReservationResponseDto> findByUserId(Long userId) throws Exception {
        log.info("Buscando reservas por usuario: idUser={}", userId);
        try {
            UserResponseDto userFind = userClient.findById(userId);
            if (userFind == null) {
                log.warn("Usuario no encontrado en ms-usuarios: idUsuario={}", userId);
                return null;
            }
            List<ReservationResponseDto> result = repository.findByIdUser(userId)
                    .stream().map(this::toDto).toList();
            log.info("Reservas encontradas para usuario: idUsuario={}, total={}", userId, result.size());
            return result;
        } catch (Exception e) {
            log.error("Error al buscar reservas por usuario: idUsuario={}, motivo={}", userId, e.getMessage());
            throw new Exception(e.getMessage());
        }
    }

    @Override
    public List<ReservationResponseDto> findByCoachId(Long coachId) throws Exception {
        log.info("Buscando reservas por usuario entrenador: idUsuario={}", coachId);
        try {
            UserResponseDto coachFind = userClient.findById(coachId);
            if (coachFind == null) {
                log.warn("Usuario entrenador no encontrado en ms-users: idUsuario={}", coachId);
                return null;
            }
            List<ClasseResponseDto> assignedClasses = claseClient.findByCoachId(coachId);
            Map<Long, Reservation> reservationsById = new LinkedHashMap<>();
            for (ClasseResponseDto assignedClass : assignedClasses) {
                repository.findByIdClasse(assignedClass.getId())
                        .forEach(reservation -> reservationsById.put(reservation.getIdReservation(), reservation));
            }
            List<ReservationResponseDto> result = reservationsById.values()
                    .stream().map(this::toDto).toList();
            log.info("Reservas encontradas para usuario entrenador: idUsuario={}, total={}",
                    coachId, result.size());
            return result;
        } catch (Exception e) {
            log.error("Error al buscar reservas por usuario entrenador: idUsuario={}, motivo={}",
                    coachId, e.getMessage());
            throw new Exception(e.getMessage());
        }
    }

    @Override
    public List<ReservationResponseDto> findByClasseId(Long classeId) throws Exception {
        log.info("Buscando reservas por clase: idClase={}", classeId);
        try {
            ClasseResponseDto classeFind = claseClient.findById(classeId);
            if (classeFind == null) {
                log.warn("Clase no encontrada en ms-classes: idClasse={}", classeId);
                return null;
            }
            List<ReservationResponseDto> result = repository.findByIdClasse(classeId)
                    .stream().map(this::toDto).toList();
            log.info("Reservas encontradas para clase: idClasse={}, total={}", classeId, result.size());
            return result;
        } catch (Exception e) {
            log.error("Error al buscar reservas por clase: idClasse={}, motivo={}", classeId, e.getMessage());
            throw new Exception(e.getMessage());
        }
    }
}