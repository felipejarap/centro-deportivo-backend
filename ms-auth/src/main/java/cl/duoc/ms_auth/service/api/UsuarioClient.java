package cl.duoc.ms_auth.service.api;

import cl.duoc.ms_auth.dto.UsuarioResponseDto;
import cl.duoc.ms_auth.dto.UserRegistrationRequestDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "ms-users")
public interface UsuarioClient {
    @GetMapping("/api/v1/users/{id}")
    UsuarioResponseDto findById(@PathVariable Long id);

    @PostMapping("/api/v1/users/register")
    UsuarioResponseDto registerStudent(@RequestBody UserRegistrationRequestDto request);

    @DeleteMapping("/api/v1/users/{id}")
    void delete(@PathVariable Long id);
}
