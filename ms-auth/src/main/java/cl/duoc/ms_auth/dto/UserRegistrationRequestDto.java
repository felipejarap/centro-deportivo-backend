package cl.duoc.ms_auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class UserRegistrationRequestDto {

    @NotBlank
    @Email
    private String email;

    @NotBlank
    private String paternalSurname;

    @NotBlank
    private String maternalSurname;

    private String phone;
    private String firstName;
    private String lastName;
    private String rut;
    private Long planId;
    private String emergencyContact;
    private String avatar;
}
