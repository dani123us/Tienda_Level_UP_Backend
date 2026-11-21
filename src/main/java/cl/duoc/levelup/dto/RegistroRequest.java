package cl.duoc.levelup.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/** Datos que envía un cliente al registrarse en la tienda (siempre queda con rol Cliente). */
public record RegistroRequest(
        @NotBlank(message = "El RUN es obligatorio") @Size(min = 7, max = 12) String run,
        @NotBlank(message = "El nombre es obligatorio") @Size(max = 50) String nombre,
        @NotBlank(message = "Los apellidos son obligatorios") @Size(max = 100) String apellidos,
        @NotBlank(message = "El correo es obligatorio") @Email(message = "Correo no válido") @Size(max = 100) String correo,
        @NotBlank(message = "La contraseña es obligatoria") @Size(min = 4, max = 10, message = "La contraseña debe tener entre 4 y 10 caracteres") String password,
        @NotNull(message = "La fecha de nacimiento es obligatoria") @Past LocalDate fechaNacimiento,
        @NotBlank(message = "La región es obligatoria") String region,
        @NotBlank(message = "La comuna es obligatoria") String comuna,
        @NotBlank(message = "La dirección es obligatoria") @Size(max = 300) String direccion,
        String telefono) {
}
