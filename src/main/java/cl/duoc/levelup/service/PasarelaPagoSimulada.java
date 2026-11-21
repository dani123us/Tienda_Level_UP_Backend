package cl.duoc.levelup.service;

import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Simula la respuesta de un medio de pago (no se cobra dinero real).
 * Regla de prueba: las tarjetas que terminan en 0000 son rechazadas por el banco.
 * Así se puede demostrar el flujo de "pago fallido" en la presentación.
 */
@Component
public class PasarelaPagoSimulada {

    public static final String DIGITOS_RECHAZO = "0000";

    /** @return vacío si el pago fue aprobado, o el motivo del rechazo. */
    public Optional<String> procesar(String metodoPago, String ultimosDigitos, int monto) {
        if (monto <= 0) {
            return Optional.of("El monto a pagar no es válido");
        }
        if (DIGITOS_RECHAZO.equals(ultimosDigitos)) {
            return Optional.of("El banco emisor rechazó el pago. Revisa los datos de tu tarjeta o usa otro medio de pago.");
        }
        return Optional.empty();
    }
}
