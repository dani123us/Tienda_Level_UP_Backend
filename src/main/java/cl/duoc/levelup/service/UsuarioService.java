package cl.duoc.levelup.service;

import cl.duoc.levelup.dto.RegistroRequest;
import cl.duoc.levelup.dto.UsuarioRequest;
import cl.duoc.levelup.dto.UsuarioResponse;
import cl.duoc.levelup.exception.DatosInvalidosException;
import cl.duoc.levelup.exception.RecursoNoEncontradoException;
import cl.duoc.levelup.exception.ReglaNegocioException;
import cl.duoc.levelup.model.Rol;
import cl.duoc.levelup.model.Usuario;
import cl.duoc.levelup.repository.BoletaRepository;
import cl.duoc.levelup.repository.UsuarioRepository;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * Reglas de negocio de los usuarios:
 * correo y RUN únicos, correo en minúsculas, contraseña de 4 a 10 caracteres cifrada con BCrypt,
 * un administrador no puede eliminarse ni quitarse el rol, y no se elimina un usuario con boletas.
 */
@Service
public class UsuarioService {

    private static final int PASSWORD_MIN = 4;
    private static final int PASSWORD_MAX = 10;

    private final UsuarioRepository usuarioRepository;
    private final BoletaRepository boletaRepository;
    private final PasswordEncoder passwordEncoder;

    public UsuarioService(UsuarioRepository usuarioRepository, BoletaRepository boletaRepository,
                          PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.boletaRepository = boletaRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public List<UsuarioResponse> listar() {
        return usuarioRepository.findAll(Sort.by("id")).stream().map(UsuarioResponse::desde).toList();
    }

    @Transactional(readOnly = true)
    public UsuarioResponse obtener(Long id) {
        return UsuarioResponse.desde(buscar(id));
    }

    @Transactional(readOnly = true)
    public Usuario buscarPorCorreo(String correo) {
        return usuarioRepository.findByCorreoIgnoreCase(correo)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe un usuario con el correo " + correo));
    }

    /** Creación desde el panel de administración (el admin elige el rol). */
    @Transactional
    public UsuarioResponse crear(UsuarioRequest req) {
        validarPassword(req.password(), true);
        Usuario usuario = new Usuario();
        copiarDatos(usuario, req.run(), req.nombre(), req.apellidos(), req.correo(), req.fechaNacimiento(),
                req.region(), req.comuna(), req.direccion(), req.telefono(), null);
        usuario.setRol(req.rol());
        usuario.setPassword(passwordEncoder.encode(req.password()));
        return UsuarioResponse.desde(usuarioRepository.save(usuario));
    }

    /** Registro público: siempre queda con rol CLIENTE. */
    @Transactional
    public Usuario registrar(RegistroRequest req) {
        validarPassword(req.password(), true);
        Usuario usuario = new Usuario();
        copiarDatos(usuario, req.run(), req.nombre(), req.apellidos(), req.correo(), req.fechaNacimiento(),
                req.region(), req.comuna(), req.direccion(), req.telefono(), null);
        usuario.setRol(Rol.CLIENTE);
        usuario.setPassword(passwordEncoder.encode(req.password()));
        return usuarioRepository.save(usuario);
    }

    /**
     * Edición desde el panel. Si la contraseña viene vacía se mantiene la anterior.
     * @param correoActual correo del administrador que hace el cambio
     */
    @Transactional
    public UsuarioResponse actualizar(Long id, UsuarioRequest req, String correoActual) {
        Usuario usuario = buscar(id);
        if (usuario.getCorreo().equalsIgnoreCase(correoActual) && req.rol() != Rol.ADMINISTRADOR) {
            throw new ReglaNegocioException("No puedes quitarte el rol de Administrador a ti mismo");
        }
        validarPassword(req.password(), false);
        copiarDatos(usuario, req.run(), req.nombre(), req.apellidos(), req.correo(), req.fechaNacimiento(),
                req.region(), req.comuna(), req.direccion(), req.telefono(), id);
        usuario.setRol(req.rol());
        if (req.password() != null && !req.password().isBlank()) {
            usuario.setPassword(passwordEncoder.encode(req.password()));
        }
        return UsuarioResponse.desde(usuario);
    }

    @Transactional
    public void eliminar(Long id, String correoActual) {
        Usuario usuario = buscar(id);
        if (usuario.getCorreo().equalsIgnoreCase(correoActual)) {
            throw new ReglaNegocioException("No puedes eliminar tu propia cuenta");
        }
        if (boletaRepository.existsByUsuarioId(id)) {
            throw new ReglaNegocioException("No se puede eliminar a " + usuario.getCorreo()
                    + " porque tiene compras registradas");
        }
        usuarioRepository.delete(usuario);
    }

    private void copiarDatos(Usuario usuario, String run, String nombre, String apellidos, String correo,
                             LocalDate fechaNacimiento, String region, String comuna, String direccion,
                             String telefono, Long idActual) {
        String runLimpio = normalizarRun(run);
        String correoLimpio = correo.trim().toLowerCase();

        usuarioRepository.findByCorreoIgnoreCase(correoLimpio)
                .filter(otro -> !otro.getId().equals(idActual))
                .ifPresent(otro -> { throw new ReglaNegocioException("El correo " + correoLimpio + " ya está registrado"); });
        boolean runCambio = idActual == null || !runLimpio.equalsIgnoreCase(usuario.getRun());
        if (runCambio && usuarioRepository.existsByRunIgnoreCase(runLimpio)) {
            throw new ReglaNegocioException("El RUN " + runLimpio + " ya está registrado");
        }

        usuario.setRun(runLimpio);
        usuario.setNombre(nombre.trim());
        usuario.setApellidos(apellidos.trim());
        usuario.setCorreo(correoLimpio);
        usuario.setFechaNacimiento(fechaNacimiento);
        usuario.setRegion(limpiar(region));
        usuario.setComuna(limpiar(comuna));
        usuario.setDireccion(limpiar(direccion));
        usuario.setTelefono(limpiar(telefono));
    }

    private void validarPassword(String password, boolean obligatoria) {
        boolean vacia = password == null || password.isBlank();
        if (vacia && !obligatoria) {
            return;
        }
        if (vacia || password.length() < PASSWORD_MIN || password.length() > PASSWORD_MAX) {
            throw new DatosInvalidosException("password", "La contraseña debe tener entre 4 y 10 caracteres");
        }
    }

    /** Quita puntos, guion y espacios del RUN y lo deja en mayúsculas (ej: 19.011.022-k → 19011022K). */
    static String normalizarRun(String run) {
        return run.replaceAll("[.\\-\\s]", "").toUpperCase();
    }

    private static String limpiar(String valor) {
        return valor == null ? null : valor.trim();
    }

    private Usuario buscar(Long id) {
        return usuarioRepository.findById(id).orElseThrow(() -> new RecursoNoEncontradoException("Usuario", id));
    }
}
