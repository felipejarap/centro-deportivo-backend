package cl.duoc.ms_auth.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter @Getter @NoArgsConstructor
public class AuthResponseDto {

    private String token;
    private String refreshToken;
    private String username;
    private Long idUsuario;
    private String rol;
    private String mensaje;

    public AuthResponseDto(String token, String username, Long idUsuario, String rol, String mensaje) {
        this(token, null, username, idUsuario, rol, mensaje);
    }

    public AuthResponseDto(String token, String refreshToken, String username, Long idUsuario, String rol, String mensaje) {
        this.token = token;
        this.refreshToken = refreshToken;
        this.username = username;
        this.idUsuario = idUsuario;
        this.rol = rol;
        this.mensaje = mensaje;
    }
}
