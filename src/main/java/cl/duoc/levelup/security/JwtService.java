package cl.duoc.levelup.security;

import cl.duoc.levelup.model.Usuario;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;

/**
 * Genera y valida los tokens JWT.
 * <ul>
 *   <li>Token de acceso: dura 15 minutos y lleva el rol del usuario.</li>
 *   <li>Token de renovación (refresh): dura 7 días y solo sirve en /api/v1/auth/refresh.</li>
 * </ul>
 * Ambos se firman con HMAC-SHA256 usando una clave secreta del servidor, por lo que no se pueden
 * modificar sin invalidar la firma.
 */
@Service
public class JwtService {

    public static final String TIPO_ACCESO = "acceso";
    public static final String TIPO_REFRESH = "refresh";

    private final SecretKey clave;
    private final Duration duracionAcceso;
    private final Duration duracionRefresh;

    public JwtService(@Value("${app.jwt.secreto}") String secretoBase64,
                      @Value("${app.jwt.expiracion-acceso-minutos:15}") long minutosAcceso,
                      @Value("${app.jwt.expiracion-refresh-dias:7}") long diasRefresh) {
        this.clave = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secretoBase64));
        this.duracionAcceso = Duration.ofMinutes(minutosAcceso);
        this.duracionRefresh = Duration.ofDays(diasRefresh);
    }

    public String generarTokenAcceso(Usuario usuario) {
        return generar(usuario, TIPO_ACCESO, duracionAcceso);
    }

    public String generarTokenRefresh(Usuario usuario) {
        return generar(usuario, TIPO_REFRESH, duracionRefresh);
    }

    public long getSegundosAcceso() {
        return duracionAcceso.toSeconds();
    }

    /**
     * Valida firma, expiración y tipo del token.
     * @return el correo del usuario (subject) si el token es válido y del tipo esperado.
     */
    public Optional<String> validar(String token, String tipoEsperado) {
        try {
            Claims claims = Jwts.parser().verifyWith(clave).build().parseSignedClaims(token).getPayload();
            if (!tipoEsperado.equals(claims.get("tipo", String.class))) {
                return Optional.empty();
            }
            return Optional.ofNullable(claims.getSubject());
        } catch (JwtException | IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    private String generar(Usuario usuario, String tipo, Duration duracion) {
        Instant ahora = Instant.now();
        return Jwts.builder()
                .subject(usuario.getCorreo())
                .claim("uid", usuario.getId())
                .claim("rol", usuario.getRol().name())
                .claim("tipo", tipo)
                .issuedAt(Date.from(ahora))
                .expiration(Date.from(ahora.plus(duracion)))
                .signWith(clave)
                .compact();
    }
}
