package com.test.desarrollo_web.Repository;

import com.test.desarrollo_web.Models.PasswordResetToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {

    Optional<PasswordResetToken> findByToken(String token);

    @Modifying
    @Query("UPDATE PasswordResetToken t SET t.usado = true WHERE t.usuario.id = :usuarioId AND t.usado = false")
    void invalidarTokensActivos(@Param("usuarioId") Integer usuarioId);
}
