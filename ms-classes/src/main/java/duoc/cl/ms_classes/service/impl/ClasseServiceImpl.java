package duoc.cl.ms_classes.service.impl;

import duoc.cl.ms_classes.dto.ClasseRequestDto;
import duoc.cl.ms_classes.dto.ClasseResponseDto;
import duoc.cl.ms_classes.dto.TypeClassesRequestDto;
import duoc.cl.ms_classes.dto.TypeClassesResponseDto;
import duoc.cl.ms_classes.dto.UserResponseDto;
import duoc.cl.ms_classes.model.Classe;
import duoc.cl.ms_classes.model.TypeClasses;
import duoc.cl.ms_classes.repository.ClasseRepository;
import duoc.cl.ms_classes.service.ClasseService;
import duoc.cl.ms_classes.service.api.UserClient;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ClasseServiceImpl implements ClasseService {

    private final ClasseRepository repository;
    private final UserClient userClient;

    private Classe toEntity(ClasseResponseDto dto) {
        Classe entity = new Classe();
        entity.setId(dto.getId());
        entity.setStartDate(dto.getStartDate());
        entity.setEndDate(dto.getEndDate());
        entity.setMaximumCapacity(dto.getMaximumCapacity());
        entity.setSpotsAvailable(dto.getSpotsAvailable());
        entity.setName(dto.getName());
        entity.setDescription(dto.getDescription());
        entity.setCoachId(dto.getCoachId());
        entity.setCoachName(dto.getCoachName());
        entity.setIntensity(dto.getIntensity());
        entity.setRoom(dto.getRoom());
        entity.setImage(dto.getImage());
        entity.setActive(dto.isActive());
        entity.setTypeClasse(dto.getTypeClasse());
        return entity;
    }

    private Classe toEntity(ClasseRequestDto dto) {
        Classe entity = new Classe();
        entity.setId(dto.getId());
        entity.setStartDate(dto.getStartDate());
        entity.setEndDate(dto.getEndDate());
        entity.setMaximumCapacity(dto.getMaximumCapacity());
        entity.setSpotsAvailable(dto.getSpotsAvailable());
        entity.setName(dto.getName());
        entity.setDescription(dto.getDescription());
        entity.setCoachId(dto.getCoachId());
        entity.setCoachName(dto.getCoachName());
        entity.setIntensity(dto.getIntensity());
        entity.setRoom(dto.getRoom());
        entity.setImage(dto.getImage());
        entity.setActive(dto.getActive() == null || dto.getActive());
        entity.setTypeClasse(dto.getTypeClasse());
        return entity;
    }

    private ClasseResponseDto toDto(Classe entity) {
        ClasseResponseDto dto = new ClasseResponseDto();
        dto.setId(entity.getId());
        dto.setStartDate(entity.getStartDate());
        dto.setEndDate(entity.getEndDate());
        dto.setMaximumCapacity(entity.getMaximumCapacity());
        dto.setSpotsAvailable(entity.getSpotsAvailable());
        dto.setName(entity.getName());
        dto.setDescription(entity.getDescription());
        dto.setCoachId(entity.getCoachId());
        dto.setCoachName(entity.getCoachName());
        dto.setIntensity(entity.getIntensity());
        dto.setRoom(entity.getRoom());
        dto.setImage(entity.getImage());
        dto.setActive(entity.isActive());
        dto.setTypeClasse(entity.getTypeClasse());
        return dto;
    }


    private TypeClasses toEntity(TypeClassesResponseDto dto) {
        return new TypeClasses( dto.getId(),dto.getName());
    }
    private TypeClasses toEntity(TypeClassesRequestDto dto) {
        return new TypeClasses( dto.getId(),dto.getName());
    }

    private TypeClassesResponseDto toDto(TypeClasses entity) {
        return new TypeClassesResponseDto(entity.getId(),entity.getName());
    }

    @Override
    public List<ClasseResponseDto> findAll() {
        return repository.findAll().stream().map(this::toDto).toList();
    }

    @Override
    public List<ClasseResponseDto> findByCoachId(Long coachId) {
        return repository.findByCoachId(coachId).stream().map(this::toDto).toList();
    }

    @Override
    public ClasseResponseDto findById(Long id) {
        return repository.findById(id).map(this::toDto).orElse(null);
    }

    @Override
    public ClasseResponseDto create(ClasseRequestDto classe) {
        validateCoach(classe.getCoachId());
        return toDto(repository.save(toEntity(classe)));
    }

    @Override
    public ClasseResponseDto update(Long id, ClasseRequestDto classe) {
        Classe current = repository.findById(id).orElse(null);
        if (current != null) {
            validateCoach(classe.getCoachId());
            int currentCapacity = current.getMaximumCapacity() == null ? 0 : current.getMaximumCapacity();
            int currentSpots = current.getSpotsAvailable() == null ? 0 : current.getSpotsAvailable();
            int reservedSpots = Math.max(0, currentCapacity - currentSpots);
            int newCapacity = classe.getMaximumCapacity() == null
                    ? currentCapacity
                    : classe.getMaximumCapacity();
            Classe entity = toEntity(classe);
            entity.setId(id);
            entity.setMaximumCapacity(newCapacity);
            entity.setSpotsAvailable(Math.max(0, newCapacity - reservedSpots));
            return toDto(repository.save(entity));
        }
        return null;
    }

    private void validateCoach(Long coachId) {
        if (coachId == null) {
            return;
        }

        UserResponseDto coach;
        try {
            coach = userClient.findById(coachId);
        } catch (FeignException.NotFound exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "El profesor indicado no existe.", exception);
        }

        if (coach == null || !coachId.equals(coach.getId())
                || coach.getTypeUser() == null
                || coach.getTypeUser().getName() == null
                || !"Entrenador".equalsIgnoreCase(coach.getTypeUser().getName().trim())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "La clase debe asignarse a un usuario con rol Entrenador.");
        }
    }

    @Override
    public boolean deleteById(Long id) {
        if (repository.existsById(id)) {
            repository.deleteById(id);
            return true;
        }
        return false;
    }

    @Override
    public boolean reserveSpot(Long id) {
        return repository.reserveSpot(id) == 1;
    }

    @Override
    public boolean releaseSpot(Long id) {
        return repository.releaseSpot(id) == 1;
    }
}
