package cl.duoc.levelup.controller;

import cl.duoc.levelup.dto.BoletaRequest;
import cl.duoc.levelup.dto.BoletaResponse;
import cl.duoc.levelup.dto.ErrorResponse;
import cl.duoc.levelup.service.BoletaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/boletas")
@Tag(name = "Boletas (órdenes)", description = "Checkout y consulta de compras")
@ApiResponse(responseCode = "401", description = "Sin token o token vencido", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
@ApiResponse(responseCode = "403", description = "El rol no tiene permiso", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
public class BoletaController {

    private final BoletaService boletaService;

    public BoletaController(BoletaService boletaService) {
        this.boletaService = boletaService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('CLIENTE','ADMINISTRADOR')")
    @Operation(summary = "Checkout (crear boleta y pagar)",
            description = "Valida stock, calcula con los precios de la BD, aplica descuentos y procesa el pago simulado. "
                    + "Si la tarjeta termina en 0000 el pago se rechaza: la boleta queda RECHAZADA y el stock no cambia.")
    @ApiResponse(responseCode = "201", description = "Boleta creada (revisar el campo estado: PAGADA o RECHAZADA)")
    @ApiResponse(responseCode = "400", description = "Datos inválidos", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "404", description = "Algún producto no existe", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "409", description = "Stock insuficiente o cupón inválido", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public BoletaResponse crear(@Valid @RequestBody BoletaRequest req, @AuthenticationPrincipal UserDetails usuario) {
        return boletaService.crear(req, usuario.getUsername());
    }

    @GetMapping("/mias")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Mis compras", description = "Boletas del usuario dueño del token.")
    @ApiResponse(responseCode = "200", description = "Lista de boletas del usuario")
    public List<BoletaResponse> mias(@AuthenticationPrincipal UserDetails usuario) {
        return boletaService.listarDelUsuario(usuario.getUsername());
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','VENDEDOR')")
    @Operation(summary = "Listar órdenes", description = "Administrador y Vendedor.")
    @ApiResponse(responseCode = "200", description = "Todas las boletas, de la más reciente a la más antigua")
    public List<BoletaResponse> listar() {
        return boletaService.listar();
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Detalle de una orden", description = "Administrador y Vendedor ven cualquiera; un Cliente solo las suyas.")
    @ApiResponse(responseCode = "200", description = "Boleta con su detalle")
    @ApiResponse(responseCode = "404", description = "La boleta no existe", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public BoletaResponse obtener(@PathVariable Long id, @AuthenticationPrincipal UserDetails usuario) {
        return boletaService.obtener(id, usuario.getUsername());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @Operation(summary = "Eliminar boleta", description = "Solo Administrador.")
    @ApiResponse(responseCode = "204", description = "Boleta eliminada")
    @ApiResponse(responseCode = "404", description = "La boleta no existe", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public void eliminar(@PathVariable Long id) {
        boletaService.eliminar(id);
    }
}
