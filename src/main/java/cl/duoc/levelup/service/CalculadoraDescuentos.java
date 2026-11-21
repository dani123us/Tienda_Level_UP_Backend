package cl.duoc.levelup.service;

import cl.duoc.levelup.exception.ReglaNegocioException;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.regex.Pattern;

/**
 * Reglas de descuento de la tienda (las mismas que muestra el frontend):
 * <ul>
 *   <li>20% para correos @duoc.cl o @profesor.duoc.cl.</li>
 *   <li>Cupones: LEVELUP10 (10%) y GAMER15 (15%).</li>
 * </ul>
 * Los descuentos se suman. El cálculo final siempre lo hace el backend.
 */
@Component
public class CalculadoraDescuentos {

    public static final int DESCUENTO_DUOC = 20;
    public static final Map<String, Integer> CUPONES = Map.of("LEVELUP10", 10, "GAMER15", 15);
    private static final Pattern CORREO_DUOC = Pattern.compile(".*@(duoc\\.cl|profesor\\.duoc\\.cl)$",
            Pattern.CASE_INSENSITIVE);

    public record Resumen(int subtotal, int porcentaje, int descuento, int total, String cupon) {
    }

    public boolean esCorreoDuoc(String correo) {
        return correo != null && CORREO_DUOC.matcher(correo.trim()).matches();
    }

    /** Devuelve el porcentaje del cupón, 0 si viene vacío, o lanza error si no existe. */
    public int porcentajeCupon(String codigo) {
        if (codigo == null || codigo.isBlank()) {
            return 0;
        }
        Integer porcentaje = CUPONES.get(normalizarCupon(codigo));
        if (porcentaje == null) {
            throw new ReglaNegocioException("El cupón " + codigo.trim() + " no es válido");
        }
        return porcentaje;
    }

    public Resumen calcular(int subtotal, String correoUsuario, String cupon) {
        int porcentaje = porcentajeCupon(cupon) + (esCorreoDuoc(correoUsuario) ? DESCUENTO_DUOC : 0);
        porcentaje = Math.min(porcentaje, 100);
        int descuento = Math.round(subtotal * porcentaje / 100f);
        String cuponNormalizado = (cupon == null || cupon.isBlank()) ? null : normalizarCupon(cupon);
        return new Resumen(subtotal, porcentaje, descuento, subtotal - descuento, cuponNormalizado);
    }

    private String normalizarCupon(String codigo) {
        return codigo.trim().toUpperCase();
    }
}
