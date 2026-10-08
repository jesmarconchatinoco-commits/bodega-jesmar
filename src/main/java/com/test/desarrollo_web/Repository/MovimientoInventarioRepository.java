package com.test.desarrollo_web.Repository;

import com.test.desarrollo_web.Models.MovimientoInventario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface MovimientoInventarioRepository extends JpaRepository<MovimientoInventario, Long> {

    @Query("""
            SELECT m FROM MovimientoInventario m
            JOIN FETCH m.presentacion pp
            JOIN FETCH m.producto p
            LEFT JOIN FETCH m.usuario
            LEFT JOIN FETCH m.compra
            WHERE m.presentacion.id = :presentacionId
            ORDER BY m.fecha DESC, m.id DESC
            """)
    List<MovimientoInventario> findByPresentacionIdOrderByFechaDesc(Integer presentacionId);

    @Query("""
            SELECT m FROM MovimientoInventario m
            JOIN FETCH m.presentacion pp
            JOIN FETCH m.producto p
            LEFT JOIN FETCH p.categoria
            LEFT JOIN FETCH m.usuario
            LEFT JOIN FETCH m.compra
            ORDER BY m.fecha DESC, m.id DESC
            """)
    List<MovimientoInventario> findAllConDetalleOrderByFechaDesc();
}
