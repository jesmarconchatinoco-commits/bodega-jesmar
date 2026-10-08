package com.test.desarrollo_web.Repository;

import com.test.desarrollo_web.Models.ImagenSlider;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ImagenSliderRepository extends JpaRepository<ImagenSlider, Integer> {

    List<ImagenSlider> findByEstado(ImagenSlider.Estado estado);

    Optional<ImagenSlider> findFirstByTituloIgnoreCase(String titulo);

    List<ImagenSlider> findByTituloNotIgnoreCaseOrderByIdDesc(String titulo);

    @Query("SELECT s FROM ImagenSlider s JOIN FETCH s.imagen i " +
           "WHERE UPPER(s.titulo) = UPPER(:titulo) AND s.estado = :estado " +
           "ORDER BY s.id DESC")
    Optional<ImagenSlider> findActiveLogoWithImagen(@Param("titulo") String titulo,
                                                    @Param("estado") ImagenSlider.Estado estado);

    @Query("SELECT s FROM ImagenSlider s JOIN FETCH s.imagen i " +
           "WHERE UPPER(s.titulo) = UPPER(:titulo) ORDER BY s.id DESC")
    List<ImagenSlider> findLogosWithImagenOrderByIdDesc(@Param("titulo") String titulo);

    @Query("SELECT s FROM ImagenSlider s JOIN FETCH s.imagen i " +
           "WHERE UPPER(s.titulo) <> UPPER(:titulo) ORDER BY s.id DESC")
    List<ImagenSlider> findSlidersWithImagenOrderByIdDesc(@Param("titulo") String titulo);

    @Query("SELECT s FROM ImagenSlider s JOIN FETCH s.imagen i " +
           "WHERE UPPER(s.titulo) = UPPER(:titulo) AND s.estado = :estado " +
           "ORDER BY s.id DESC")
    List<ImagenSlider> findActiveSlidersWithImagen(@Param("titulo") String titulo,
                                                   @Param("estado") ImagenSlider.Estado estado);

    @Query("SELECT s FROM ImagenSlider s JOIN FETCH s.imagen i WHERE s.id = :id")
    Optional<ImagenSlider> findByIdWithImagen(@Param("id") Integer id);
}
