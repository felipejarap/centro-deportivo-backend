package cl.duoc.MS_Usuarios.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Schema(description = "Datos de perfil para crear un alumno sin permitir que el cliente asigne su rol o identificador")
public class StudentRegistrationRequestDto {

    @NotBlank
    private String paternalSurname;

    @NotBlank
    private String maternalSurname;

    @NotBlank
    @Email
    private String email;

    private String phone;
    private String firstName;
    private String lastName;
    private String rut;
    private Long planId;
    private String emergencyContact;
    private String avatar;
}
