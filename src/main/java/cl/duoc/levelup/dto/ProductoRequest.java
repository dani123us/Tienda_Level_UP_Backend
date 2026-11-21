package cl.duoc.levelup.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ProductoRequest(
        @Schema(example = "CO002") @NotBlank(message = "El código es obligatorio") @Size(min = 3, max = 20) String codigo,
        @Schema(example = "Nintendo Switch 2") @NotBlank(message = "El nombre es obligatorio") @Size(max = 150) String nombre,
        @Size(max = 1000) String descripcion,
        @Schema(example = "499990") @NotNull(message = "El precio es obligatorio") @Min(value = 0, message = "El precio no puede ser negativo") Integer precio,
        @Schema(example = "10") @NotNull(message = "El stock es obligatorio") @Min(value = 0, message = "El stock no puede ser negativo") Integer stock,
        @Schema(example = "3") @NotNull(message = "El stock crítico es obligatorio") @Min(value = 0, message = "El stock crítico no puede ser negativo") Integer stockCritico,
        @Size(max = 1000) String imagen,
        @Size(max = 10) String icono,
        boolean destacado,
        @Schema(example = "Consolas") @NotBlank(message = "La categoría es obligatoria") String categoria) {
}
