package cl.duoc.ms_auth.service;

import cl.duoc.ms_auth.dto.AuthRequestDto;
import cl.duoc.ms_auth.dto.AuthResponseDto;
import cl.duoc.ms_auth.dto.CredencialRequestDto;
import cl.duoc.ms_auth.dto.CredencialResponseDto;
import cl.duoc.ms_auth.dto.StudentRegistrationRequestDto;

import java.util.List;

public interface AuthService {
    AuthResponseDto login(AuthRequestDto request);
    AuthResponseDto refreshToken(String refreshToken);
    CredencialResponseDto registerStudent(StudentRegistrationRequestDto request);
    CredencialResponseDto registerForAdmin(CredencialRequestDto request);
    List<CredencialResponseDto> findAll();
    CredencialResponseDto findById(Long id);
    boolean delete(Long id);
    boolean changePassword(Long userId, String currentPassword, String newPassword);
}
