package cl.duoc.levelup.dto;

import cl.duoc.levelup.model.Producto;

public record ProductoResponse(
        Long id,
        String codigo,
        String nombre,
        String descripcion,
        Integer precio,
        Integer stock,
        Integer stockCritico,
        String imagen,
        String icono,
        boolean destacado,
        Long categoriaId,
        String categoria,
        boolean enStockCritico) {

    public static ProductoResponse desde(Producto p) {
        return new ProductoResponse(p.getId(), p.getCodigo(), p.getNombre(), p.getDescripcion(), p.getPrecio(),
                p.getStock(), p.getStockCritico(), p.getImagen(), p.getIcono(), p.isDestacado(),
                p.getCategoria().getId(), p.getCategoria().getNombre(), p.isEnStockCritico());
    }
}
