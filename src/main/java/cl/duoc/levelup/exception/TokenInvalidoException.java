package cl.duoc.levelup.exception;

/** Se lanza cuando el refresh token no es válido, está vencido o no es de tipo refresh (responde 401). */
public class TokenInvalidoException extends RuntimeException {

    public TokenInvalidoException(String mensaje) {
        super(mensaje);
    }
}
