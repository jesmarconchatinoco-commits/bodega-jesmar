package com.test.desarrollo_web.Repository;

import com.test.desarrollo_web.Models.TipoPago;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TipoPagoRepository extends JpaRepository<TipoPago, Integer> {
    java.util.Optional<TipoPago> findByNombreIgnoreCase(String nombre);
}
