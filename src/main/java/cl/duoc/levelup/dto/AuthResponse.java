package cl.duoc.levelup.dto;

/**
 * Respuesta de login, registro y renovación.
 * accessToken: dura poco (15 min) y se envía en cada petición como "Authorization: Bearer ...".
 * refreshToken: dura más (7 días) y solo sirve para pedir un accessToken nuevo.
 */
public record AuthResponse(
        String accessToken,
        String refreshToken,
        String tipo,
        long expiraEnSegundos,
        UsuarioResponse usuario) {
}
