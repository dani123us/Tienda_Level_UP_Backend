package cl.duoc.levelup.dto;

import cl.duoc.levelup.model.Rol;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * Datos para crear o actualizar un usuario desde el panel de administración.
 * Al actualizar, si la contraseña viene vacía se mantiene la anterior.
 */
public record UsuarioRequest(
        @NotBlank(message = "El RUN es obligatorio") @Size(min = 7, max = 12) String run,
        @NotBlank(message = "El nombre es obligatorio") @Size(max = 50) String nombre,
        @NotBlank(message = "Los apellidos son obligatorios") @Size(max = 100) String apellidos,
        @NotBlank(message = "El correo es obligatorio") @Email(message = "Correo no válido") @Size(max = 100) String correo,
        @Size(max = 10, message = "La contraseña debe tener como máximo 10 caracteres") String password,
        @NotNull(message = "El rol es obligatorio") Rol rol,
        LocalDate fechaNacimiento,
        String region,
        String comuna,
        @Size(max = 300) String direccion,
        String telefono) {
}
