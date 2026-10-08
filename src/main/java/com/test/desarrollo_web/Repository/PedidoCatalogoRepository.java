package com.test.desarrollo_web.Repository;

import com.test.desarrollo_web.Models.PedidoCatalogo;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PedidoCatalogoRepository extends JpaRepository<PedidoCatalogo, Long> {

    long countByEstado(PedidoCatalogo.Estado estado);

    List<PedidoCatalogo> findAllByOrderByFechaDesc();

    List<PedidoCatalogo> findByEstadoOrderByFechaDesc(PedidoCatalogo.Estado estado);

    @Query("""
            SELECT DISTINCT p FROM PedidoCatalogo p
            LEFT JOIN FETCH p.comprobantePago
            ORDER BY p.fecha DESC
            """)
    List<PedidoCatalogo> findAllWithComprobanteOrderByFechaDesc();

    @Query("""
            SELECT DISTINCT p FROM PedidoCatalogo p
            LEFT JOIN FETCH p.comprobantePago
            WHERE p.estado = :estado
            ORDER BY p.fecha DESC
            """)
    List<PedidoCatalogo> findByEstadoWithComprobanteOrderByFechaDesc(@Param("estado") PedidoCatalogo.Estado estado);

    List<PedidoCatalogo> findAllByOrderByFechaDesc(Pageable pageable);

    default List<PedidoCatalogo> findTop5ByOrderByFechaDesc() {
        return findAllByOrderByFechaDesc(Pageable.ofSize(5));
    }

    @Query("""
            SELECT DISTINCT p FROM PedidoCatalogo p
            LEFT JOIN FETCH p.detalles
            LEFT JOIN FETCH p.horarioRecojo hr
            LEFT JOIN FETCH hr.fechaAtencion
            WHERE p.id = :id
            """)
    Optional<PedidoCatalogo> findByIdWithDetalles(@Param("id") Long id);

    long countByHorarioRecojoIdAndEstadoIn(Long horarioRecojoId, java.util.Collection<PedidoCatalogo.Estado> estados);

    boolean existsByVentaId(Long ventaId);
}
