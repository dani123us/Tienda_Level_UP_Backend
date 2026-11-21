package cl.duoc.levelup.dto;

import cl.duoc.levelup.model.Categoria;

public record CategoriaResponse(Long id, String nombre) {

    public static CategoriaResponse desde(Categoria c) {
        return new CategoriaResponse(c.getId(), c.getNombre());
    }
}
