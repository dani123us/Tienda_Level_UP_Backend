package cl.duoc.levelup.dto;

import java.time.LocalDateTime;
import java.util.Map;

/** Formato único para todos los errores de la API. */
public record ErrorResponse(
        LocalDateTime fecha,
        int estado,
        String error,
        String mensaje,
        String ruta,
        Map<String, String> campos) {
}
