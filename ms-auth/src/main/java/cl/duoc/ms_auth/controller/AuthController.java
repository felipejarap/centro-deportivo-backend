package cl.duoc.ms_auth.controller;

import cl.duoc.ms_auth.dto.AuthRequestDto;
import cl.duoc.ms_auth.dto.AuthResponseDto;
import cl.duoc.ms_auth.dto.CredencialRequestDto;
import cl.duoc.ms_auth.dto.CredencialResponseDto;
import cl.duoc.ms_auth.dto.PasswordChangeRequestDto;
import cl.duoc.ms_auth.dto.StudentRegistrationRequestDto;
import cl.duoc.ms_auth.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Autenticación", description = "Endpoints para login, registro y administración de credenciales de acceso")
public class AuthController {

    private final AuthService service;
    // Login — obtener token
    @Operation(summary = "Iniciar sesión", description = "Valida las credenciales de un usuario y, si son correctas, retorna un token JWT de acceso.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Autenticación exitosa, token generado",
                content = @Content(schema = @Schema(implementation = AuthResponseDto.class))),
                 @ApiResponse(responseCode = "401", description = "Credenciales inválidas", content = @Content),
                 @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos o formato incompatible", content = @Content)
   })
    @PostMapping("/login")
    public ResponseEntity<AuthResponseDto> login(
            @Valid @RequestBody AuthRequestDto request) {
        AuthResponseDto response = service.login(request);
        if (response.getToken() == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        }
        return ResponseEntity.ok(response);
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponseDto> refresh(
            @RequestHeader("Authorization") String authorization) {
        if (!authorization.startsWith("Bearer ")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        AuthResponseDto response = service.refreshToken(authorization.substring(7));
        return response == null
                ? ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()
                : ResponseEntity.ok(response);
    }

    @Operation(summary = "Registrar alumno", description = "Crea un perfil nuevo de alumno y sus credenciales en una sola operación. No acepta identificadores de perfiles existentes.")
    @ApiResponses(value = {
                         @ApiResponse(responseCode = "201", description = "Alumno y credenciales registrados exitosamente",
                                 content = @Content(schema = @Schema(implementation = CredencialResponseDto.class))),
                         @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos", content = @Content),
                         @ApiResponse(responseCode = "409", description = "El correo ya está registrado", content = @Content)
   })
    @PostMapping("/register")
    public ResponseEntity<CredencialResponseDto> register(
            @Valid @RequestBody StudentRegistrationRequestDto request) {
        CredencialResponseDto response = service.registerStudent(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/credenciales")
    public ResponseEntity<CredencialResponseDto> registerForAdmin(
            @Valid @RequestBody CredencialRequestDto request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.registerForAdmin(request));
    }

    @Operation(summary = "Listar credenciales", description = "Recupera el listado completo de credenciales registradas en el sistema.")
    @ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Listado recuperado exitosamente") })

    @GetMapping("/credenciales")
    public ResponseEntity<List<CredencialResponseDto>> findAll() {

        return ResponseEntity.ok(service.findAll());
    }

    @Operation(summary = "Buscar credencial por ID", description = "Recupera el detalle de una credencial específica mediante su identificador único.")
    @ApiResponses(value = {
                         @ApiResponse(responseCode = "200", description = "Credencial encontrada",
                                 content = @Content(schema = @Schema(implementation = CredencialResponseDto.class))),
                         @ApiResponse(responseCode = "404", description = "No se encontró ninguna credencial con el ID proporcionado", content = @Content)
    })

    @GetMapping("/credenciales/{id}")
    public ResponseEntity<CredencialResponseDto> findById(@PathVariable Long id) {
        CredencialResponseDto credencial = service.findById(id);
        if (credencial == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(credencial);
    }

    @Operation(summary = "Eliminar credencial", description = "Elimina permanentemente las credenciales de acceso de un usuario.")
    @ApiResponses(value = {
                         @ApiResponse(responseCode = "204", description = "Credencial eliminada exitosamente (Sin contenido)", content = @Content),
                         @ApiResponse(responseCode = "404", description = "No se encontró la credencial solicitada", content = @Content)
    })

    @DeleteMapping("/credenciales/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        if (service.delete(id)) return ResponseEntity.noContent().build();
        return ResponseEntity.notFound().build();
    }

    @PutMapping("/users/{userId}/password")
    public ResponseEntity<Void> changePassword(
            @PathVariable Long userId,
            @Valid @RequestBody PasswordChangeRequestDto request) {
        if (service.changePassword(userId, request.getCurrentPassword(), request.getNewPassword())) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }
}
