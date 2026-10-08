package com.test.desarrollo_web.Repository;

import com.test.desarrollo_web.Models.Modulo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ModuloRepository extends JpaRepository<Modulo, Integer> {
    List<Modulo> findByEstadoOrderByOrdenAsc(Modulo.Estado estado);

    java.util.Optional<Modulo> findByCodigo(String codigo);
}
