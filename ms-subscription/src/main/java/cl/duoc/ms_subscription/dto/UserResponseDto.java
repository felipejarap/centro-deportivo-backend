package cl.duoc.ms_subscription.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter @Getter @NoArgsConstructor @AllArgsConstructor
public class UserResponseDto {

    Long id;
    String username;
    String paternalSurname;
    String maternalSurname;
    String email;
    String phone;
    //private TypeUserResponseDto typeUser;
}
