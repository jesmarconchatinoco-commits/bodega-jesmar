package com.test.desarrollo_web.Repository;

import com.test.desarrollo_web.Models.HorarioAtencion;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface HorarioAtencionRepository extends JpaRepository<HorarioAtencion, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT h FROM HorarioAtencion h JOIN FETCH h.fechaAtencion WHERE h.id = :id")
    Optional<HorarioAtencion> findByIdForUpdate(@Param("id") Long id);
}
