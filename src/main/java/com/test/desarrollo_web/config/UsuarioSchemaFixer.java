package com.test.desarrollo_web.config;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class UsuarioSchemaFixer {

    private static final Logger log = LoggerFactory.getLogger(UsuarioSchemaFixer.class);

    private final JdbcTemplate jdbcTemplate;

    public UsuarioSchemaFixer(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @PostConstruct
    public void asegurarColumnaTelefono() {
        try {
            Integer existe = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM information_schema.COLUMNS "
                            + "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'usuario' AND COLUMN_NAME = 'telefono'",
                    Integer.class);
            if (existe == null || existe == 0) {
                jdbcTemplate.execute("ALTER TABLE usuario ADD COLUMN telefono VARCHAR(20) NULL");
                log.info("Columna telefono agregada a la tabla usuario.");
            }
        } catch (Exception e) {
            log.warn("No se pudo verificar/agregar columna telefono en usuario: {}", e.getMessage());
        }
    }
}
