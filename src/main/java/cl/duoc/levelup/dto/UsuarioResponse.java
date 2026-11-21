package cl.duoc.levelup.dto;

import cl.duoc.levelup.model.Rol;
import cl.duoc.levelup.model.Usuario;

import java.time.LocalDate;

/** Usuario sin la contraseña: es lo único que sale de la API. */
public record UsuarioResponse(
        Long id,
        String run,
        String nombre,
        String apellidos,
        String correo,
        Rol rol,
        LocalDate fechaNacimiento,
        String region,
        String comuna,
        String direccion,
        String telefono) {

    public static UsuarioResponse desde(Usuario u) {
        return new UsuarioResponse(u.getId(), u.getRun(), u.getNombre(), u.getApellidos(), u.getCorreo(),
                u.getRol(), u.getFechaNacimiento(), u.getRegion(), u.getComuna(), u.getDireccion(), u.getTelefono());
    }
}
