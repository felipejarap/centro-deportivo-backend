package cl.duoc.MS_Usuarios.service.impl;

import cl.duoc.MS_Usuarios.dto.StudentRegistrationRequestDto;
import cl.duoc.MS_Usuarios.dto.TypeUserResponseDto;
import cl.duoc.MS_Usuarios.dto.UserRequestDto;
import cl.duoc.MS_Usuarios.dto.UserResponseDto;
import cl.duoc.MS_Usuarios.model.TypeUser;
import cl.duoc.MS_Usuarios.model.User;
import cl.duoc.MS_Usuarios.repository.TypeUserRepository;
import cl.duoc.MS_Usuarios.repository.UserRepository;
import cl.duoc.MS_Usuarios.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository repository;
    private final TypeUserRepository typeUserRepository;

    private TypeUser resolveTypeUser(Long typeUserId) {
        return typeUserRepository.findById(typeUserId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Tipo de usuario no encontrado: " + typeUserId
                ));
    }

    private User toEntity(UserRequestDto dto) {
        User user = new User();
        user.setUsername(dto.getUsername());
        user.setPaternalSurname(dto.getPaternalSurname());
        user.setMaternalSurname(dto.getMaternalSurname());
        user.setEmail(dto.getEmail());
        user.setPhone(dto.getPhone());
        user.setFirstName(dto.getFirstName());
        user.setLastName(dto.getLastName());
        user.setRut(dto.getRut());
        user.setActive(dto.getActive() == null || dto.getActive());
        user.setPlanId(dto.getPlanId());
        user.setSpecialty(dto.getSpecialty());
        user.setBio(dto.getBio());
        user.setEmergencyContact(dto.getEmergencyContact());
        user.setAvatar(dto.getAvatar());
        user.setRegistrationDate(dto.getRegistrationDate() == null ? java.time.LocalDate.now() : dto.getRegistrationDate());
        user.setTypeUser(resolveTypeUser(dto.getTypeUserId()));
        return user;
    }

    private UserResponseDto toDto(User entity) {
        TypeUserResponseDto typeDto = null;
        if (entity.getTypeUser() != null) {
            TypeUser role = entity.getTypeUser();
            typeDto = new TypeUserResponseDto(role.getId(), role.getName(), role.getDescription(), List.of());
        }
        return new UserResponseDto(
                entity.getIdUser(),
                entity.getUsername(),
                entity.getPaternalSurname(),
                entity.getMaternalSurname(),
                entity.getEmail(),
                entity.getPhone(),
                typeDto,
                entity.getFirstName(),
                entity.getLastName(),
                entity.getRut(),
                entity.isActive(),
                entity.getPlanId(),
                entity.getSpecialty(),
                entity.getBio(),
                entity.getEmergencyContact(),
                entity.getAvatar(),
                entity.getRegistrationDate() == null ? null : entity.getRegistrationDate().toString()
        );
    }

    @Override
    public List<UserResponseDto> findAll() {
        return repository.findAllWithTypeUser().stream().map(this::toDto).toList();
    }

    @Override
    public UserResponseDto findById(Long id) {
        return repository.findByIdWithTypeUser(id).map(this::toDto).orElse(null);
    }

    @Override
    public List<UserResponseDto> findByTypeUserId(Long typeUserId) {
        if (!typeUserRepository.existsById(typeUserId)) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Tipo de usuario no encontrado: " + typeUserId
            );
        }
        return repository.findByTypeUser_Id(typeUserId).stream().map(this::toDto).toList();
    }

    @Override
    public UserResponseDto create(UserRequestDto user) {
        User entity = toEntity(user);
        return toDto(repository.save(entity));
    }

    @Override
    public UserResponseDto registerStudent(StudentRegistrationRequestDto registration) {
        String email = registration.getEmail().trim().toLowerCase(java.util.Locale.ROOT);
        if (repository.existsByEmailIgnoreCase(email)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Este correo electrónico ya está registrado.");
        }
        TypeUser studentRole = typeUserRepository.findByNameIgnoreCase("Cliente")
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.INTERNAL_SERVER_ERROR,
                        "No está configurado el tipo de usuario Cliente."
                ));
        UserRequestDto user = new UserRequestDto();
        user.setUsername(email);
        user.setPaternalSurname(registration.getPaternalSurname().trim());
        user.setMaternalSurname(registration.getMaternalSurname().trim());
        user.setEmail(email);
        user.setPhone(registration.getPhone());
        user.setTypeUserId(studentRole.getId());
        user.setFirstName(registration.getFirstName());
        user.setLastName(registration.getLastName());
        user.setRut(registration.getRut());
        user.setActive(true);
        user.setPlanId(registration.getPlanId());
        user.setEmergencyContact(registration.getEmergencyContact());
        user.setAvatar(registration.getAvatar());
        user.setRegistrationDate(java.time.LocalDate.now());
        return create(user);
    }

    @Override
    public UserResponseDto update(Long id, UserRequestDto user) {
        if (repository.existsById(id)) {
            User entity = toEntity(user);
            entity.setIdUser(id);
            return toDto(repository.save(entity));
        }
        return null;
    }

    @Override
    public UserResponseDto updateProfile(Long id, UserRequestDto user) {
        return repository.findByIdWithTypeUser(id).map(entity -> {
            entity.setUsername(user.getUsername());
            entity.setPaternalSurname(user.getPaternalSurname());
            entity.setMaternalSurname(user.getMaternalSurname());
            entity.setEmail(user.getEmail());
            entity.setPhone(user.getPhone());
            entity.setFirstName(user.getFirstName());
            entity.setLastName(user.getLastName());
            entity.setRut(user.getRut());
            entity.setEmergencyContact(user.getEmergencyContact());
            entity.setAvatar(user.getAvatar());
            return toDto(repository.save(entity));
        }).orElse(null);
    }

    @Override
    public boolean delete(Long id) {
        if (repository.existsById(id)) {
            repository.deleteById(id);
            return true;
        }
        return false;
    }
}
