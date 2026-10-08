package com.test.desarrollo_web.Repository;

import com.test.desarrollo_web.Models.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Integer> {
    // Método personalizado útil para el login en Spring Security
    @Query("""
                SELECT DISTINCT u
                FROM Usuario u
                LEFT JOIN FETCH u.perfil p
                LEFT JOIN FETCH p.modulos
                WHERE u.usuario = :usuario
            """)
    Optional<Usuario> findByUsuario(@Param("usuario") String usuario);

    Optional<Usuario> findByCorreoIgnoreCase(String correo);
}
