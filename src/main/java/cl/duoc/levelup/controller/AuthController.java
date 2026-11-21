package cl.duoc.levelup.controller;

import cl.duoc.levelup.dto.AuthResponse;
import cl.duoc.levelup.dto.ErrorResponse;
import cl.duoc.levelup.dto.LoginRequest;
import cl.duoc.levelup.dto.RefreshRequest;
import cl.duoc.levelup.dto.RegistroRequest;
import cl.duoc.levelup.dto.UsuarioResponse;
import cl.duoc.levelup.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Autenticación", description = "Login, registro y renovación de tokens JWT")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    @SecurityRequirements
    @Operation(summary = "Iniciar sesión", description = "Devuelve un token de acceso (15 min) y un refresh token (7 días).")
    @ApiResponse(responseCode = "200", description = "Login correcto")
    @ApiResponse(responseCode = "400", description = "Datos inválidos", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "401", description = "Correo o contraseña incorrectos", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public AuthResponse login(@Valid @RequestBody LoginRequest req) {
        return authService.login(req);
    }

    @PostMapping("/registro")
    @ResponseStatus(HttpStatus.CREATED)
    @SecurityRequirements
    @Operation(summary = "Registrar cliente", description = "Crea una cuenta con rol Cliente y devuelve sus tokens.")
    @ApiResponse(responseCode = "201", description = "Cliente registrado")
    @ApiResponse(responseCode = "400", description = "Datos inválidos", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "409", description = "Correo o RUN ya registrado", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public AuthResponse registrar(@Valid @RequestBody RegistroRequest req) {
        return authService.registrar(req);
    }

    @PostMapping("/refresh")
    @SecurityRequirements
    @Operation(summary = "Renovar tokens", description = "Recibe el refresh token y entrega un par nuevo (rotación). Un token de acceso no sirve aquí.")
    @ApiResponse(responseCode = "200", description = "Tokens renovados")
    @ApiResponse(responseCode = "401", description = "Refresh token inválido o vencido", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public AuthResponse refrescar(@Valid @RequestBody RefreshRequest req) {
        return authService.refrescar(req.refreshToken());
    }

    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Usuario actual", description = "Datos del usuario dueño del token (sin contraseña).")
    @ApiResponse(responseCode = "200", description = "Usuario autenticado")
    @ApiResponse(responseCode = "401", description = "Sin token o token vencido", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public UsuarioResponse me(@AuthenticationPrincipal UserDetails usuario) {
        return authService.usuarioActual(usuario.getUsername());
    }
}
