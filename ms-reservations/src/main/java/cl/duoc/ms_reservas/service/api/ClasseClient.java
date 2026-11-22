package cl.duoc.ms_reservas.service.api;

import cl.duoc.ms_reservas.dto.ClasseResponseDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

@FeignClient(name = "ms-classes")
public interface ClasseClient {
    @GetMapping("/api/v1/classes/{id}")
    ClasseResponseDto findById(@PathVariable Long id);

    @PostMapping("/api/v1/classes/{id}/spots/reserve")
    void reserveSpot(@PathVariable Long id);

    @PostMapping("/api/v1/classes/{id}/spots/release")
    void releaseSpot(@PathVariable Long id);
}
