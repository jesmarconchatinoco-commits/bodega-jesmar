package com.test.desarrollo_web.Repository;

import com.test.desarrollo_web.Models.ConfiguracionPago;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ConfiguracionPagoRepository extends JpaRepository<ConfiguracionPago, Integer> {

    Optional<ConfiguracionPago> findByCanal(ConfiguracionPago.Canal canal);
}
