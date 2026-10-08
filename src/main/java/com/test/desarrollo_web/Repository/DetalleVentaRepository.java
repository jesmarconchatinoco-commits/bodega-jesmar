package com.test.desarrollo_web.Repository;

import com.test.desarrollo_web.Models.DetalleVenta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface DetalleVentaRepository extends JpaRepository<DetalleVenta, Long> {

    @Query(value = "SELECT p.nombre, SUM(dv.cantidad), SUM(dv.subtotal) " +
            "FROM DETALLE_VENTA dv " +
            "INNER JOIN PRODUCTO p ON dv.id_producto = p.id " +
            "INNER JOIN VENTA v ON dv.id_venta = v.id " +
            "WHERE v.estado <> 'ANULADO' " +
            "GROUP BY p.id, p.nombre " +
            "ORDER BY SUM(dv.cantidad) DESC " +
            "LIMIT 5", nativeQuery = true)
    List<Object[]> findTop5ProductosVendidos();

    @Query(value = """
            SELECT CAST(v.fecha AS DATE) AS periodo,
                   COALESCE(SUM((dv.precio_unitario - COALESCE(p.precio_compra, 0)) * dv.cantidad), 0) AS ganancia
            FROM DETALLE_VENTA dv
            INNER JOIN VENTA v ON dv.id_venta = v.id
            INNER JOIN producto p ON dv.id_producto = p.id
            WHERE CAST(v.fecha AS DATE) >= :desde AND v.estado <> 'ANULADO'
            GROUP BY CAST(v.fecha AS DATE)
            ORDER BY periodo ASC
            """, nativeQuery = true)
    List<Object[]> sumGananciasPorDiaDesde(@Param("desde") LocalDate desde);

    default List<Object[]> sumGananciasPorDiaUltimos7() {
        return sumGananciasPorDiaDesde(LocalDate.now().minusDays(6));
    }

    @Query(value = """
            SELECT EXTRACT(YEAR FROM v.fecha) AS anio, EXTRACT(MONTH FROM v.fecha) AS mes,
                   COALESCE(SUM((dv.precio_unitario - COALESCE(p.precio_compra, 0)) * dv.cantidad), 0) AS ganancia
            FROM DETALLE_VENTA dv
            INNER JOIN VENTA v ON dv.id_venta = v.id
            INNER JOIN producto p ON dv.id_producto = p.id
            WHERE CAST(v.fecha AS DATE) >= :desde AND v.estado <> 'ANULADO'
            GROUP BY EXTRACT(YEAR FROM v.fecha), EXTRACT(MONTH FROM v.fecha)
            ORDER BY anio ASC, mes ASC
            """, nativeQuery = true)
    List<Object[]> sumGananciasPorMesDesde(@Param("desde") LocalDate desde);

    default List<Object[]> sumGananciasPorMesUltimos12() {
        return sumGananciasPorMesDesde(LocalDate.now().minusMonths(11));
    }

    @Query(value = """
            SELECT EXTRACT(YEAR FROM v.fecha) AS anio,
                   COALESCE(SUM((dv.precio_unitario - COALESCE(p.precio_compra, 0)) * dv.cantidad), 0) AS ganancia
            FROM DETALLE_VENTA dv
            INNER JOIN VENTA v ON dv.id_venta = v.id
            INNER JOIN producto p ON dv.id_producto = p.id
            WHERE CAST(v.fecha AS DATE) >= :desde AND v.estado <> 'ANULADO'
            GROUP BY EXTRACT(YEAR FROM v.fecha)
            ORDER BY anio ASC
            """, nativeQuery = true)
    List<Object[]> sumGananciasPorAnioDesde(@Param("desde") LocalDate desde);

    default List<Object[]> sumGananciasPorAnioUltimos5() {
        return sumGananciasPorAnioDesde(LocalDate.now().minusYears(4));
    }

    @Query("""
            SELECT v.fecha, dv.precioUnitario, dv.cantidad, COALESCE(p.precioCompra, 0)
            FROM DetalleVenta dv
            INNER JOIN dv.venta v
            INNER JOIN dv.producto p
            WHERE v.estado <> com.test.desarrollo_web.Models.Ventas.Estado.ANULADO
              AND v.fecha >= :desde
            """)
    List<Object[]> findLineasGananciaDesde(@org.springframework.data.repository.query.Param("desde") java.time.LocalDateTime desde);

    @Query("""
            SELECT dv FROM DetalleVenta dv
            JOIN FETCH dv.venta v
            JOIN FETCH dv.producto p
            LEFT JOIN FETCH dv.presentacion pr
            WHERE p.id = :productoId AND v.estado <> com.test.desarrollo_web.Models.Ventas.Estado.ANULADO
            ORDER BY v.fecha DESC
            """)
    List<DetalleVenta> findMovimientosByProducto(Integer productoId);

    @Query("""
            SELECT dv FROM DetalleVenta dv
            JOIN FETCH dv.venta v
            JOIN FETCH dv.producto p
            JOIN FETCH dv.presentacion pr
            WHERE pr.id = :presentacionId AND v.estado <> com.test.desarrollo_web.Models.Ventas.Estado.ANULADO
            ORDER BY v.fecha DESC
            """)
    List<DetalleVenta> findMovimientosByPresentacion(Integer presentacionId);
}
