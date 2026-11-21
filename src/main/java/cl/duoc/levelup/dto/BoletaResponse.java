package cl.duoc.levelup.dto;

import cl.duoc.levelup.model.Boleta;
import cl.duoc.levelup.model.EstadoBoleta;

import java.time.LocalDateTime;
import java.util.List;

public record BoletaResponse(
        Long id,
        LocalDateTime fecha,
        EstadoBoleta estado,
        Long usuarioId,
        String nombreCliente,
        String correoCliente,
        String telefono,
        String direccion,
        String region,
        String comuna,
        String indicaciones,
        String metodoPago,
        String ultimosDigitosTarjeta,
        String motivoRechazo,
        String cupon,
        Integer subtotal,
        Integer porcentajeDescuento,
        Integer descuento,
        Integer total,
        List<DetalleBoletaResponse> detalles) {

    public static BoletaResponse desde(Boleta b) {
        return new BoletaResponse(b.getId(), b.getFecha(), b.getEstado(), b.getUsuario().getId(),
                b.getNombreCliente(), b.getCorreoCliente(), b.getTelefono(), b.getDireccion(), b.getRegion(),
                b.getComuna(), b.getIndicaciones(), b.getMetodoPago(), b.getUltimosDigitosTarjeta(),
                b.getMotivoRechazo(), b.getCupon(), b.getSubtotal(), b.getPorcentajeDescuento(), b.getDescuento(),
                b.getTotal(), b.getDetalles().stream().map(DetalleBoletaResponse::desde).toList());
    }
}
