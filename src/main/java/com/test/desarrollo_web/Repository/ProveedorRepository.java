package com.test.desarrollo_web.Repository;

import com.test.desarrollo_web.Models.Proveedor;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProveedorRepository extends JpaRepository<Proveedor, Integer> {

    List<Proveedor> findByEstadoOrderByNombreAsc(Proveedor.Estado estado);
}
