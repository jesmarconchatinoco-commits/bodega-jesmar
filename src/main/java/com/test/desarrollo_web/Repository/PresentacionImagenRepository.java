package com.test.desarrollo_web.Repository;

import com.test.desarrollo_web.Models.PresentacionImagen;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PresentacionImagenRepository extends JpaRepository<PresentacionImagen, Long> {

    List<PresentacionImagen> findByPresentacion_IdOrderByOrdenAsc(Integer presentacionId);

    void deleteByPresentacion_Id(Integer presentacionId);
}
