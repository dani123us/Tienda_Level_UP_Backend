package cl.duoc.levelup.repository;

import cl.duoc.levelup.model.Producto;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProductoRepository extends JpaRepository<Producto, Long> {

    boolean existsByCodigoIgnoreCase(String codigo);

    boolean existsByCategoriaId(Long categoriaId);

    List<Producto> findByCategoriaNombreIgnoreCase(String categoria);

    List<Producto> findAllByOrderByIdAsc();
}
