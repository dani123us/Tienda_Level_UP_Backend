package cl.duoc.levelup.exception;

/** Se lanza cuando una operación no cumple una regla del negocio (responde 409). */
public class ReglaNegocioException extends RuntimeException {

    public ReglaNegocioException(String mensaje) {
        super(mensaje);
    }
}
