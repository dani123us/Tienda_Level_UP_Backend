package cl.duoc.levelup.dto;

import cl.duoc.levelup.model.DetalleBoleta;

public record DetalleBoletaResponse(
        Long id,
        Long productoId,
        String codigoProducto,
        String nombreProducto,
        Integer cantidad,
        Integer precioUnitario,
        Integer subtotal) {

    public static DetalleBoletaResponse desde(DetalleBoleta d) {
        return new DetalleBoletaResponse(d.getId(), d.getProducto() != null ? d.getProducto().getId() : null,
                d.getCodigoProducto(), d.getNombreProducto(), d.getCantidad(), d.getPrecioUnitario(), d.getSubtotal());
    }
}
