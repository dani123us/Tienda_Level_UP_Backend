package cl.duoc.levelup.exception;

/**
 * Se lanza cuando un dato no cumple una validación que depende del caso
 * (por ejemplo, la contraseña es obligatoria al crear pero opcional al editar). Responde 400.
 */
public class DatosInvalidosException extends RuntimeException {

    private final String campo;

    public DatosInvalidosException(String campo, String mensaje) {
        super(mensaje);
        this.campo = campo;
    }

    public String getCampo() {
        return campo;
    }
}
