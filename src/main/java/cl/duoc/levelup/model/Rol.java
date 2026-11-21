package cl.duoc.levelup.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Roles del sistema.
 * <ul>
 *   <li>ADMINISTRADOR: acceso total.</li>
 *   <li>VENDEDOR: solo puede ver productos y órdenes (boletas).</li>
 *   <li>CLIENTE: solo puede usar la tienda.</li>
 * </ul>
 * En el JSON se envía con su etiqueta legible ("Administrador", "Vendedor", "Cliente").
 */
public enum Rol {
    ADMINISTRADOR("Administrador"),
    VENDEDOR("Vendedor"),
    CLIENTE("Cliente");

    private final String etiqueta;

    Rol(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    @JsonValue
    public String getEtiqueta() {
        return etiqueta;
    }

    /** Nombre de la autoridad que usa Spring Security (ej: ROLE_ADMINISTRADOR). */
    public String getAutoridad() {
        return "ROLE_" + name();
    }

    @JsonCreator
    public static Rol desdeTexto(String valor) {
        if (valor == null) {
            return null;
        }
        for (Rol rol : values()) {
            if (rol.name().equalsIgnoreCase(valor.trim()) || rol.etiqueta.equalsIgnoreCase(valor.trim())) {
                return rol;
            }
        }
        throw new IllegalArgumentException("Rol no válido: " + valor);
    }
}
