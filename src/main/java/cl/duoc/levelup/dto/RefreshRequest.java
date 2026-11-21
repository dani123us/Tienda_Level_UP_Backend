package cl.duoc.levelup.dto;

import jakarta.validation.constraints.NotBlank;

public record RefreshRequest(@NotBlank(message = "El refresh token es obligatorio") String refreshToken) {
}
