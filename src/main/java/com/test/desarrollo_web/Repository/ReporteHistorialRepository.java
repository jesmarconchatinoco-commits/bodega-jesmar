package com.test.desarrollo_web.Repository;

import com.test.desarrollo_web.Models.ReporteHistorial;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReporteHistorialRepository extends JpaRepository<ReporteHistorial, Long> {

    List<ReporteHistorial> findTop50ByOrderByFechaDesc();
}
