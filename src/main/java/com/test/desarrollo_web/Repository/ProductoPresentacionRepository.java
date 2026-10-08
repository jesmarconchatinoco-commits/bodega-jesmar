package com.test.desarrollo_web.Repository;

import com.test.desarrollo_web.Models.ProductoPresentacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface ProductoPresentacionRepository extends JpaRepository<ProductoPresentacion, Integer> {

    List<ProductoPresentacion> findByProducto_IdOrderByOrdenAscIdAsc(Integer productoId);

    long countByProducto_Id(Integer productoId);

    @Query("""
            SELECT pp FROM ProductoPresentacion pp
            JOIN FETCH pp.producto p
            LEFT JOIN FETCH p.categoria
            WHERE pp.estado = com.test.desarrollo_web.Models.ProductoPresentacion.Estado.ACTIVO
              AND p.estado = com.test.desarrollo_web.Models.Producto.Estado.ACTIVO
            ORDER BY p.categoria.nombre ASC, p.nombre ASC, pp.orden ASC, pp.id ASC
            """)
    List<ProductoPresentacion> findActivasParaVenta();

    @Query("""
            SELECT pp FROM ProductoPresentacion pp
            JOIN FETCH pp.producto
            WHERE pp.producto.id = :productoId
              AND pp.estado = com.test.desarrollo_web.Models.ProductoPresentacion.Estado.ACTIVO
            ORDER BY pp.orden ASC, pp.id ASC
            """)
    List<ProductoPresentacion> findActivasByProductoId(Integer productoId);

    Optional<ProductoPresentacion> findFirstByProducto_IdOrderByOrdenAscIdAsc(Integer productoId);

    boolean existsByProducto_IdAndNombreIgnoreCaseAndIdNot(Integer productoId, String nombre, Integer id);

    boolean existsByProducto_IdAndNombreIgnoreCase(Integer productoId, String nombre);

    boolean existsByCodigoBarras(String codigoBarras);
}
