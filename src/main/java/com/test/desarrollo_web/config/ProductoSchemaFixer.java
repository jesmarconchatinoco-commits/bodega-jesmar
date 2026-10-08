package com.test.desarrollo_web.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;

@Configuration
public class ProductoSchemaFixer {

    private static final Logger log = LoggerFactory.getLogger(ProductoSchemaFixer.class);

    @Bean
    @Order(100)
    CommandLineRunner alinearEsquemaProducto(JdbcTemplate jdbcTemplate,
                                             com.test.desarrollo_web.service.ProductoPresentacionService presentacionService) {
        return args -> {
            unificarColumnaCategoria(jdbcTemplate);
            agregarPrecioCompra(jdbcTemplate);
            agregarCamposPresentacion(jdbcTemplate);
            agregarImagenPresentacion(jdbcTemplate);
            presentacionService.migrarProductosSinPresentacion();
        };
    }

    private void agregarImagenPresentacion(JdbcTemplate jdbc) {
        try {
            if (!tablaExiste(jdbc, "producto_presentacion")) {
                return;
            }
            if (!columnaExiste(jdbc, "producto_presentacion", "imagen_id")) {
                jdbc.execute("""
                        ALTER TABLE producto_presentacion
                        ADD COLUMN imagen_id BIGINT NULL
                        """);
                log.info("Columna imagen_id agregada a producto_presentacion.");
            }
        } catch (Exception e) {
            log.warn("No se pudo agregar imagen_id en producto_presentacion: {}", e.getMessage());
        }
    }

    private void agregarCamposPresentacion(JdbcTemplate jdbc) {
        try {
            if (!tablaExiste(jdbc, "producto_presentacion")) {
                return;
            }
            if (!columnaExiste(jdbc, "producto_presentacion", "precio_compra")) {
                jdbc.execute("""
                        ALTER TABLE producto_presentacion
                        ADD COLUMN precio_compra DECIMAL(10, 2) DEFAULT 0.00
                        """);
                log.info("Columna precio_compra agregada a producto_presentacion.");
            }
            if (!columnaExiste(jdbc, "producto_presentacion", "stock_minimo")) {
                jdbc.execute("""
                        ALTER TABLE producto_presentacion
                        ADD COLUMN stock_minimo INT NOT NULL DEFAULT 0
                        """);
                log.info("Columna stock_minimo agregada a producto_presentacion.");
            }
            jdbc.execute("""
                    UPDATE producto_presentacion pp
                    INNER JOIN producto p ON p.id = pp.producto_id
                    SET pp.precio_compra = COALESCE(pp.precio_compra, p.precio_compra, 0.00),
                        pp.stock_minimo = COALESCE(pp.stock_minimo, p.stock_minimo, 0)
                    WHERE pp.precio_compra IS NULL OR pp.stock_minimo IS NULL
                    """);
        } catch (Exception e) {
            log.warn("No se pudo alinear columnas de producto_presentacion: {}", e.getMessage());
        }
    }

    private void agregarPrecioCompra(JdbcTemplate jdbc) {
        try {
            if (!tablaExiste(jdbc, "producto")) {
                return;
            }
            if (!columnaExiste(jdbc, "producto", "precio_compra")) {
                jdbc.execute("""
                        ALTER TABLE producto
                        ADD COLUMN precio_compra DECIMAL(10, 2) DEFAULT 0.00
                        """);
                log.info("Columna precio_compra agregada a producto.");
            }
            jdbc.execute("""
                    UPDATE producto
                    SET precio_compra = 0.00
                    WHERE precio_compra IS NULL
                    """);
        } catch (Exception e) {
            log.warn("No se pudo agregar precio_compra en producto: {}", e.getMessage());
        }
    }

    private void unificarColumnaCategoria(JdbcTemplate jdbc) {
        try {
            if (!tablaExiste(jdbc, "producto")) {
                return;
            }

            boolean tieneIdCategoria = columnaExiste(jdbc, "producto", "id_categoria");
            boolean tieneCategoriaId = columnaExiste(jdbc, "producto", "categoria_id");

            if (tieneIdCategoria && tieneCategoriaId) {
                jdbc.execute("""
                        UPDATE producto
                        SET id_categoria = categoria_id
                        WHERE (id_categoria IS NULL OR id_categoria = 0) AND categoria_id IS NOT NULL
                        """);
                eliminarRestriccionesColumna(jdbc, "producto", "categoria_id");
                jdbc.execute("ALTER TABLE producto DROP COLUMN categoria_id");
                log.info("Columna duplicada categoria_id eliminada de producto.");
            } else if (!tieneIdCategoria && tieneCategoriaId) {
                eliminarRestriccionesColumna(jdbc, "producto", "categoria_id");
                jdbc.execute("ALTER TABLE producto CHANGE COLUMN categoria_id id_categoria INT NOT NULL");
                log.info("Columna categoria_id renombrada a id_categoria en producto.");
            } else if (tieneCategoriaId) {
                eliminarRestriccionesColumna(jdbc, "producto", "categoria_id");
                jdbc.execute("ALTER TABLE producto DROP COLUMN categoria_id");
                log.info("Columna sobrante categoria_id eliminada de producto.");
            } else if (tieneIdCategoria) {
                log.info("Tabla producto correcta: solo usa id_categoria.");
            }
        } catch (Exception e) {
            log.warn("No se pudo alinear columna de categoría en producto: {}", e.getMessage());
        }
    }

    private void eliminarRestriccionesColumna(JdbcTemplate jdbc, String tabla, String columna) {
        var restricciones = jdbc.queryForList("""
                SELECT CONSTRAINT_NAME
                FROM INFORMATION_SCHEMA.KEY_COLUMN_USAGE
                WHERE TABLE_SCHEMA = DATABASE()
                  AND TABLE_NAME = ?
                  AND COLUMN_NAME = ?
                  AND CONSTRAINT_NAME IS NOT NULL
                """, tabla, columna);

        for (var row : restricciones) {
            String nombre = String.valueOf(row.get("CONSTRAINT_NAME"));
            if (nombre == null || nombre.isBlank() || "PRIMARY".equalsIgnoreCase(nombre)) {
                continue;
            }
            try {
                jdbc.execute("ALTER TABLE " + tabla + " DROP FOREIGN KEY " + nombre);
                log.info("Restricción {} eliminada de {}.{}", nombre, tabla, columna);
            } catch (Exception e) {
                try {
                    jdbc.execute("ALTER TABLE " + tabla + " DROP INDEX " + nombre);
                    log.info("Índice {} eliminado de {}.{}", nombre, tabla, columna);
                } catch (Exception ignored) {
                    log.debug("No se pudo eliminar restricción {}: {}", nombre, e.getMessage());
                }
            }
        }
    }

    private boolean tablaExiste(JdbcTemplate jdbc, String tabla) {
        Integer count = jdbc.queryForObject("""
                SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES
                WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ?
                """, Integer.class, tabla);
        return count != null && count > 0;
    }

    private boolean columnaExiste(JdbcTemplate jdbc, String tabla, String columna) {
        Integer count = jdbc.queryForObject("""
                SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
                WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ? AND COLUMN_NAME = ?
                """, Integer.class, tabla, columna);
        return count != null && count > 0;
    }
}
