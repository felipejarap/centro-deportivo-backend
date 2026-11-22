package cl.duoc.ms_auth.service.impl;

import cl.duoc.ms_auth.dto.AuthRequestDto;
import cl.duoc.ms_auth.dto.AuthResponseDto;
import cl.duoc.ms_auth.dto.CredencialRequestDto;
import cl.duoc.ms_auth.dto.CredencialResponseDto;
import cl.duoc.ms_auth.dto.StudentRegistrationRequestDto;
import cl.duoc.ms_auth.dto.UsuarioResponseDto;
import cl.duoc.ms_auth.dto.UserRegistrationRequestDto;
import cl.duoc.ms_auth.exception.UserNotFoundException;
import cl.duoc.ms_auth.exception.UsernameAlreadyExistsException;
import cl.duoc.ms_auth.model.Credencial;
import cl.duoc.ms_auth.reporsitory.CredencialRepository;
import cl.duoc.ms_auth.security.JwtUtil;
import cl.duoc.ms_auth.service.AuthService;
import cl.duoc.ms_auth.service.api.UsuarioClient;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Locale;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {
    private final CredencialRepository repository;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;
    private final UsuarioClient usuarioClient;

    private CredencialResponseDto toDto(Credencial entity) {
        return new CredencialResponseDto(
                entity.getIdCredencial(),
                entity.getUsername(),
                entity.getIdUser(),
                entity.getActive()
        );
    }

    @Override
    public AuthResponseDto login(AuthRequestDto request) {
        log.info("Intento de login para username: {}", request.getUsername());

        Credencial credencial = repository.findByUsername(request.getUsername())
                .orElse(null);

        if (credencial == null || !credencial.getActive()) {
            log.warn("Username no encontrado o inactivo: {}", request.getUsername());
            return new AuthResponseDto(null, null, null, null, "Credenciales inválidas");
        }

        if (!passwordEncoder.matches(request.getPassword(), credencial.getPassword())) {
            log.warn("Password incorrecto para: {}", request.getUsername());
            return new AuthResponseDto(null, null, null, null, "Credenciales inválidas");
        }

        var usuario = usuarioClient.findById(credencial.getIdUser());
        if (usuario == null || !usuario.isActive()
                || usuario.getTypeUser() == null || usuario.getTypeUser().getName() == null) {
            log.warn("Usuario asociado a la credencial {} no está activo o no tiene rol", credencial.getIdUser());
            return new AuthResponseDto(null, null, null, null, "Credenciales inválidas");
        }
        String rol = usuario.getTypeUser().getName();

        String token = jwtUtil.generateToken(
                credencial.getUsername(),
                credencial.getIdUser(),
                rol
        );
        String refreshToken = jwtUtil.generateRefreshToken(
                credencial.getUsername(),
                credencial.getIdUser(),
                rol
        );
        credencial.setRefreshTokenId(jwtUtil.extractTokenId(refreshToken));
        repository.save(credencial);

        log.info("Login exitoso para: {} con rol: {}", request.getUsername(), rol);
        return new AuthResponseDto(token, refreshToken, credencial.getUsername(),
                credencial.getIdUser(), rol, "Login exitoso");
    }

    @Override
    public AuthResponseDto refreshToken(String refreshToken) {
        if (!jwtUtil.validateRefreshToken(refreshToken)) {
            return null;
        }

        Long userId = jwtUtil.extractIdUsuario(refreshToken);
        String username = jwtUtil.extractUsername(refreshToken);
        if (userId == null || username == null) {
            return null;
        }

        Credencial credential = repository.findByIdUser(userId).orElse(null);
        if (credential == null || !credential.getActive()
                || !username.equals(credential.getUsername())
                || !java.util.Objects.equals(
                        jwtUtil.extractTokenId(refreshToken),
                        credential.getRefreshTokenId())) {
            return null;
        }

        var user = usuarioClient.findById(userId);
        if (user == null || !user.isActive() || user.getTypeUser() == null
                || user.getTypeUser().getName() == null) {
            return null;
        }

        String currentRole = user.getTypeUser().getName();
        String renewedRefreshToken = jwtUtil.generateRefreshToken(username, userId, currentRole);
        credential.setRefreshTokenId(jwtUtil.extractTokenId(renewedRefreshToken));
        repository.save(credential);
        return new AuthResponseDto(
                jwtUtil.generateToken(username, userId, currentRole),
                renewedRefreshToken,
                username,
                userId,
                currentRole,
                "Token renovado"
        );
    }

    @Override
    public CredencialResponseDto registerStudent(StudentRegistrationRequestDto request) {
        String email = request.getEmail().trim().toLowerCase(Locale.ROOT);
        if (repository.findByUsername(email).isPresent()) {
            throw new UsernameAlreadyExistsException("Este correo electrónico ya tiene una cuenta.");
        }

        UserRegistrationRequestDto userRequest = new UserRegistrationRequestDto();
        userRequest.setEmail(email);
        userRequest.setPaternalSurname(request.getPaternalSurname().trim());
        userRequest.setMaternalSurname(request.getMaternalSurname().trim());
        userRequest.setPhone(request.getPhone());
        userRequest.setFirstName(request.getFirstName());
        userRequest.setLastName(request.getLastName());
        userRequest.setRut(request.getRut());
        userRequest.setPlanId(request.getPlanId());
        userRequest.setEmergencyContact(request.getEmergencyContact());
        userRequest.setAvatar(request.getAvatar());

        String encodedPassword = passwordEncoder.encode(request.getPassword());
        UsuarioResponseDto user;
        try {
            user = usuarioClient.registerStudent(userRequest);
        } catch (FeignException.Conflict exception) {
            throw new UsernameAlreadyExistsException("Este correo electrónico ya está registrado.");
        }
        if (user == null || user.getIdUser() == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY, "El servicio de usuarios no confirmó la creación del perfil.");
        }
        if (!user.isActive() || user.getTypeUser() == null
                || !"Cliente".equalsIgnoreCase(user.getTypeUser().getName())) {
            compensateStudentRegistration(user.getIdUser());
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY, "El servicio de usuarios no creó un perfil de alumno válido.");
        }

        try {
            Credencial credential = new Credencial();
            credential.setUsername(email);
            credential.setPassword(encodedPassword);
            credential.setIdUser(user.getIdUser());
            credential.setActive(true);
            return toDto(repository.save(credential));
        } catch (RuntimeException exception) {
            compensateStudentRegistration(user.getIdUser());
            throw exception;
        }
    }

    @Override
    public CredencialResponseDto registerForAdmin(CredencialRequestDto request) {
        return register(request);
    }

    private CredencialResponseDto register(CredencialRequestDto request) {
        log.info("Registrando credencial para idUsuario: {}", request.getIdUser());

        if (repository.findByUsername(request.getUsername()).isPresent()) {
            log.warn("Username ya registrado:{}",request.getUsername());
            throw new UsernameAlreadyExistsException(
                    "El username '" + request.getUsername() + "' ya está en uso");
        }

        // Verificar que el usuario existe en ms-usuarios
        try {
            var user = usuarioClient.findById(request.getIdUser());
            if (user == null || !user.isActive()) {
                throw new UsernameNotFoundException("El usuario no existe.");
            }
        } catch (Exception e) {
            if (e instanceof UsernameNotFoundException notFound) throw notFound;
            log.warn("Usuario {} no encontrado en ms-usuarios", request.getIdUser());
            throw new UsernameNotFoundException(
                    "El usuario con id" + request.getIdUser() + " no existe");
        }
        Credencial credencial = new Credencial();
        credencial.setUsername(request.getUsername());
        credencial.setPassword(passwordEncoder.encode(request.getPassword())); // ← hashea el password
        credencial.setIdUser(request.getIdUser());
        credencial.setActive(true);

        return toDto(repository.save(credencial));
    }

    private void compensateStudentRegistration(Long userId) {
        try {
            usuarioClient.delete(userId);
        } catch (RuntimeException exception) {
            log.error("No se pudo revertir el perfil {} tras fallar el registro de credenciales", userId, exception);
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "El registro quedó incompleto y requiere revisión administrativa.");
        }
    }

    @Override
    public List<CredencialResponseDto> findAll() {
        return repository.findAll().stream().map(this::toDto).toList();
    }

    @Override
    public CredencialResponseDto findById(Long id) {
        return repository.findById(id).map(this::toDto).orElse(null);
    }

    @Override
    public boolean delete(Long id) {
        if (repository.existsById(id)) {
            repository.deleteById(id);
            return true;
        }
        return false;
    }

    @Override
    public boolean changePassword(Long userId, String currentPassword, String newPassword) {
        Credencial credencial = repository.findByIdUser(userId).orElse(null);
        if (credencial == null || !credencial.getActive()
                || !passwordEncoder.matches(currentPassword, credencial.getPassword())) {
            return false;
        }
        credencial.setPassword(passwordEncoder.encode(newPassword));
        credencial.setRefreshTokenId(null);
        repository.save(credencial);
        return true;
    }
}
