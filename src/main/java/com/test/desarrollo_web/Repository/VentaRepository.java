package com.test.desarrollo_web.Repository;

import com.test.desarrollo_web.Models.Ventas;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface VentaRepository extends JpaRepository<Ventas, Long> {

    @Query(value = "SELECT COALESCE(SUM(total), 0) FROM VENTA " +
            "WHERE DATE(fecha) = CURDATE() AND estado <> 'ANULADO'", nativeQuery = true)
    BigDecimal sumVentasDelDia();

    @Query(value = "SELECT COALESCE(SUM(total), 0) FROM VENTA " +
            "WHERE YEAR(fecha) = YEAR(CURDATE()) AND MONTH(fecha) = MONTH(CURDATE()) " +
            "AND estado <> 'ANULADO'", nativeQuery = true)
    BigDecimal sumVentasDelMes();

    @Query("""
            SELECT v FROM Ventas v
            LEFT JOIN FETCH v.cliente
            LEFT JOIN FETCH v.vendedor
            LEFT JOIN FETCH v.tipoPago
            LEFT JOIN FETCH v.tipoComprobante
            ORDER BY v.fecha DESC
            """)
    List<Ventas> findAllWithDetalles();

    @Query("""
            SELECT v FROM Ventas v
            LEFT JOIN FETCH v.detalles d
            LEFT JOIN FETCH d.producto
            LEFT JOIN FETCH d.presentacion
            WHERE v.id = :id
            """)
    Optional<Ventas> findByIdWithDetalles(Long id);

    @Query(value = "SELECT COUNT(*) FROM VENTA WHERE DATE(fecha) = CURDATE() AND estado <> 'ANULADO'", nativeQuery = true)
    long countVentasDelDia();

    @Query(value = "SELECT COUNT(*) FROM VENTA WHERE YEAR(fecha) = YEAR(CURDATE()) " +
            "AND MONTH(fecha) = MONTH(CURDATE()) AND estado <> 'ANULADO'", nativeQuery = true)
    long countVentasDelMes();

    @Query(value = """
            SELECT DATE(fecha) AS dia, COALESCE(SUM(total), 0) AS monto
            FROM VENTA
            WHERE fecha >= DATE_SUB(CURDATE(), INTERVAL 6 DAY) AND estado <> 'ANULADO'
            GROUP BY DATE(fecha)
            ORDER BY dia ASC
            """, nativeQuery = true)
    List<Object[]> sumVentasPorDiaUltimos7();

    @Query(value = """
            SELECT canal, COALESCE(SUM(total), 0) FROM (
                SELECT v.total,
                    CASE WHEN EXISTS (
                        SELECT 1 FROM PEDIDO_CATALOGO pc WHERE pc.venta_id = v.id
                    ) THEN 'Venta por Web' ELSE 'Ventas POS' END AS canal
                FROM VENTA v
                WHERE YEAR(v.fecha) = YEAR(CURDATE()) AND MONTH(v.fecha) = MONTH(CURDATE())
                AND v.estado <> 'ANULADO'
            ) ventas_canal
            GROUP BY canal
            ORDER BY canal
            """, nativeQuery = true)
    List<Object[]> sumVentasPorCanalMes();

    @Query("""
            SELECT v FROM Ventas v
            LEFT JOIN FETCH v.cliente
            WHERE v.estado <> :estado
            ORDER BY v.fecha DESC
            """)
    List<Ventas> findTop5Recientes(@org.springframework.data.repository.query.Param("estado") Ventas.Estado estado,
                                  org.springframework.data.domain.Pageable pageable);

    default List<Ventas> findTop5Recientes(Ventas.Estado estado) {
        return findTop5Recientes(estado, org.springframework.data.domain.PageRequest.of(0, 5));
    }

    @Query("SELECT v FROM Ventas v LEFT JOIN FETCH v.cliente WHERE v.id = :id")
    Optional<Ventas> findByIdWithCliente(Long id);

    @Query("""
            SELECT DISTINCT v FROM Ventas v
            LEFT JOIN FETCH v.cliente
            LEFT JOIN FETCH v.vendedor
            LEFT JOIN FETCH v.tipoPago
            LEFT JOIN FETCH v.tipoComprobante
            LEFT JOIN FETCH v.detalles d
            LEFT JOIN FETCH d.producto
            LEFT JOIN FETCH d.presentacion
            WHERE v.id = :id
            """)
    Optional<Ventas> findByIdCompleta(Long id);
}
