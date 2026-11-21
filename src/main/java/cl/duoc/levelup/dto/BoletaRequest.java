package cl.duoc.levelup.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * Checkout: productos del carrito + datos del cliente + pago.
 * Por seguridad solo se envían los últimos 4 dígitos de la tarjeta (nunca el número completo).
 * Tarjeta de prueba: si termina en 0000 el pago es rechazado.
 */
public record BoletaRequest(
        @NotEmpty(message = "El carrito está vacío") List<@Valid ItemBoletaRequest> items,
        @NotBlank(message = "El nombre es obligatorio") @Size(max = 150) String nombreCliente,
        @NotBlank(message = "El correo es obligatorio") @Email(message = "Correo no válido") String correoCliente,
        @Size(max = 20) String telefono,
        @NotBlank(message = "La dirección es obligatoria") @Size(max = 300) String direccion,
        @NotBlank(message = "La región es obligatoria") String region,
        @NotBlank(message = "La comuna es obligatoria") String comuna,
        @Size(max = 300) String indicaciones,
        @Schema(example = "LEVELUP10") String cupon,
        @Schema(example = "Tarjeta de crédito") @NotBlank(message = "El método de pago es obligatorio") String metodoPago,
        @Schema(example = "4242") @NotBlank(message = "Faltan los dígitos de la tarjeta") @Pattern(regexp = "\\d{4}", message = "Se esperan 4 dígitos") String ultimosDigitosTarjeta) {
}
