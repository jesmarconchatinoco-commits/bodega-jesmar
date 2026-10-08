package com.test.desarrollo_web.Repository;

import com.test.desarrollo_web.Models.FechaAtencion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface FechaAtencionRepository extends JpaRepository<FechaAtencion, Long> {

    Optional<FechaAtencion> findByFecha(LocalDate fecha);

    @Query("""
            SELECT DISTINCT f FROM FechaAtencion f
            LEFT JOIN FETCH f.horarios
            WHERE f.fecha >= :desde
            ORDER BY f.fecha ASC
            """)
    List<FechaAtencion> findDesdeWithHorarios(@Param("desde") LocalDate desde);

    @Query("""
            SELECT DISTINCT f FROM FechaAtencion f
            LEFT JOIN FETCH f.horarios
            ORDER BY f.fecha ASC
            """)
    List<FechaAtencion> findAllWithHorarios();
}
