package cl.duoc.MS_Usuarios.dto;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class TypeUserResponseDtoTest {

    @Test
    void derivesSpringSecurityRoleNameAndLeavesItNullWithoutAName() {
        TypeUserResponseDto dto = new TypeUserResponseDto(3L, "cliente", "Student", List.of("read"));

        assertEquals("ROLE_CLIENTE", dto.getRoleName());
        assertEquals(3L, dto.getId());
        assertEquals("cliente", dto.getName());
        assertEquals("Student", dto.getDescription());
        assertEquals(List.of("read"), dto.getPermissions());

        dto.setName(null);
        assertNull(dto.getRoleName());
    }
}
