package cl.duoc.levelup.service;

import cl.duoc.levelup.dto.AuthResponse;
import cl.duoc.levelup.dto.LoginRequest;
import cl.duoc.levelup.dto.RegistroRequest;
import cl.duoc.levelup.dto.UsuarioResponse;
import cl.duoc.levelup.exception.TokenInvalidoException;
import cl.duoc.levelup.model.Usuario;
import cl.duoc.levelup.repository.UsuarioRepository;
import cl.duoc.levelup.security.JwtService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Login, registro y renovación de tokens.
 * Flujo: login → par de tokens (acceso + refresh) → el front usa el de acceso en cada petición →
 * cuando vence (401) llama a /auth/refresh con el refresh y recibe un par NUEVO (rotación).
 */
@Service
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final UsuarioRepository usuarioRepository;
    private final UsuarioService usuarioService;
    private final JwtService jwtService;

    public AuthService(AuthenticationManager authenticationManager, UsuarioRepository usuarioRepository,
                       UsuarioService usuarioService, JwtService jwtService) {
        this.authenticationManager = authenticationManager;
        this.usuarioRepository = usuarioRepository;
        this.usuarioService = usuarioService;
        this.jwtService = jwtService;
    }

    /** Si el correo o la contraseña no coinciden, Spring lanza BadCredentialsException (401). */
    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest req) {
        String correo = req.correo().trim().toLowerCase();
        authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(correo, req.password()));
        return generarTokens(usuarioService.buscarPorCorreo(correo));
    }

    @Transactional
    public AuthResponse registrar(RegistroRequest req) {
        return generarTokens(usuarioService.registrar(req));
    }

    @Transactional(readOnly = true)
    public AuthResponse refrescar(String refreshToken) {
        String correo = jwtService.validar(refreshToken, JwtService.TIPO_REFRESH)
                .orElseThrow(() -> new TokenInvalidoException("El refresh token no es válido o expiró. Inicia sesión nuevamente"));
        Usuario usuario = usuarioRepository.findByCorreoIgnoreCase(correo)
                .orElseThrow(() -> new TokenInvalidoException("El usuario del token ya no existe"));
        return generarTokens(usuario);
    }

    @Transactional(readOnly = true)
    public UsuarioResponse usuarioActual(String correo) {
        return UsuarioResponse.desde(usuarioService.buscarPorCorreo(correo));
    }

    private AuthResponse generarTokens(Usuario usuario) {
        return new AuthResponse(jwtService.generarTokenAcceso(usuario), jwtService.generarTokenRefresh(usuario),
                "Bearer", jwtService.getSegundosAcceso(), UsuarioResponse.desde(usuario));
    }
}
