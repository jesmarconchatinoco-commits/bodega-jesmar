package com.test.desarrollo_web.Repository;

import com.test.desarrollo_web.Models.TipoComprobante;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TipoComprobanteRepository extends JpaRepository<TipoComprobante, Integer> {
    List<TipoComprobante> findByEstado(TipoComprobante.Estado estado);
}
