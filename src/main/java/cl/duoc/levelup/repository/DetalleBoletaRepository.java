package cl.duoc.levelup.repository;

import cl.duoc.levelup.model.DetalleBoleta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DetalleBoletaRepository extends JpaRepository<DetalleBoleta, Long> {

    /** Al eliminar un producto, sus detalles históricos quedan sin referencia (se conserva nombre y precio). */
    @Modifying
    @Query("update DetalleBoleta d set d.producto = null where d.producto.id = :productoId")
    void desvincularProducto(@Param("productoId") Long productoId);
}
