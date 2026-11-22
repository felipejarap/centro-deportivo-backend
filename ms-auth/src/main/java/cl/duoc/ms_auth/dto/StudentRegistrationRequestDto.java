package cl.duoc.ms_auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class StudentRegistrationRequestDto extends UserRegistrationRequestDto {

    @NotBlank
    @Size(min = 6, message = "La contraseña debe tener al menos 6 caracteres.")
    private String password;
}
