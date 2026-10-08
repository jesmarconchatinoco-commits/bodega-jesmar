package com.test.desarrollo_web.Repository;

import com.test.desarrollo_web.Models.Cliente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ClienteRepository extends JpaRepository<Cliente, Integer> {
    Optional<Cliente> findByDocumento(String documento);

    @Query("""
            SELECT c FROM Cliente c
            WHERE REPLACE(REPLACE(TRIM(c.documento), '-', ''), ' ', '') = :doc
            """)
    Optional<Cliente> findByDocumentoNormalizado(@Param("doc") String doc);
}
