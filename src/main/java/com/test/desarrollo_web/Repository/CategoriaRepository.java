package com.test.desarrollo_web.Repository;

import com.test.desarrollo_web.Models.Categoria;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface CategoriaRepository extends JpaRepository<Categoria, Integer> {
    List<Categoria> findByEstado(Categoria.Estado estado);
}
