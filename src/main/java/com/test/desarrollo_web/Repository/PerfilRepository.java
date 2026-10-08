package com.test.desarrollo_web.Repository;

import com.test.desarrollo_web.Models.Perfil;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface PerfilRepository extends JpaRepository<Perfil, Integer> {

    @Override
    @EntityGraph(attributePaths = { "modulos" })
    List<Perfil> findAll();

    @Query("SELECT DISTINCT p FROM Perfil p LEFT JOIN FETCH p.modulos")
    List<Perfil> findAllWithModulos();
}
