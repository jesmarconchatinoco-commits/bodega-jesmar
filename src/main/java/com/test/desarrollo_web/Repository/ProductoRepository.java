package com.test.desarrollo_web.Repository;

import com.test.desarrollo_web.Models.Producto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ProductoRepository extends JpaRepository<Producto, Integer> {
    List<Producto> findByEstado(Producto.Estado estado);

    long countByEstado(Producto.Estado estado);

    long countByCategoria_Id(Integer categoriaId);

    @Query("SELECT COUNT(p) FROM Producto p WHERE p.estado = com.test.desarrollo_web.Models.Producto.Estado.ACTIVO AND p.stock <= p.stockMinimo")
    long countStockBajo();

    @Query("""
            SELECT p FROM Producto p
            LEFT JOIN FETCH p.categoria
            WHERE p.estado = com.test.desarrollo_web.Models.Producto.Estado.ACTIVO AND p.stock <= p.stockMinimo
            ORDER BY p.stock ASC
            """)
    List<Producto> findStockBajo();

    @Query("""
            SELECT DISTINCT p FROM Producto p
            LEFT JOIN FETCH p.categoria
            LEFT JOIN FETCH p.imagen
            WHERE p.estado = com.test.desarrollo_web.Models.Producto.Estado.ACTIVO
            ORDER BY p.nombre ASC
            """)
    List<Producto> findActivosWithDetalles();

    @Query("""
            SELECT DISTINCT p FROM Producto p
            LEFT JOIN FETCH p.categoria
            LEFT JOIN FETCH p.imagen
            ORDER BY p.id ASC
            """)
    List<Producto> findAllWithDetalles();
}
