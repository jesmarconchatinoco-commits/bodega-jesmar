package com.test.desarrollo_web.Repository;

import com.test.desarrollo_web.Models.VentaCuota;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface VentaCuotaRepository extends JpaRepository<VentaCuota, Long> {

    List<VentaCuota> findByVenta_IdOrderByNumeroCuotaAsc(Long ventaId);

    List<VentaCuota> findByVenta_IdAndEstadoOrderByNumeroCuotaAsc(Long ventaId, VentaCuota.Estado estado);

    @Query("SELECT c FROM VentaCuota c JOIN FETCH c.venta WHERE c.id = :id")
    Optional<VentaCuota> findByIdWithVenta(@Param("id") Long id);

    long countByVenta_IdAndEstado(Long ventaId, VentaCuota.Estado estado);

    void deleteByVenta_Id(Long ventaId);
}
