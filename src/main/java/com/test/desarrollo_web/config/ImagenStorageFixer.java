package com.test.desarrollo_web.config;

import com.test.desarrollo_web.util.ImagenRutas;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

@Configuration
public class ImagenStorageFixer {

    private static final Logger log = LoggerFactory.getLogger(ImagenStorageFixer.class);

    @Bean
    @Order(50)
    CommandLineRunner alinearAlmacenamientoImagenes(JdbcTemplate jdbcTemplate,
                                                    StoragePathResolver storagePathResolver,
                                                    @Value("${app.upload.dir:src/main/resources/imagen}") String uploadDir) {
        return args -> {
            Path baseImagen = storagePathResolver.resolve(uploadDir);
            moverCarpetaLegacy(baseImagen, "producto", ImageStorageCategory.PRESENTACIONES.getFolder());
            moverCarpetaLegacy(baseImagen, "productos", ImageStorageCategory.PRESENTACIONES.getFolder());
            normalizarRutasEnBaseDeDatos(jdbcTemplate);
            migrarProductoImagenAPresentacion(jdbcTemplate);
            sincronizarImagenPrincipalProducto(jdbcTemplate);
            eliminarTablaProductoImagen(jdbcTemplate);
        };
    }

    private void moverCarpetaLegacy(Path baseImagen, String carpetaOrigen, String carpetaDestino) {
        Path origen = baseImagen.resolve(carpetaOrigen);
        if (!Files.isDirectory(origen)) {
            return;
        }
        Path destino = baseImagen.resolve(carpetaDestino);
        try {
            Files.createDirectories(destino);
            try (Stream<Path> archivos = Files.list(origen)) {
                archivos.filter(Files::isRegularFile).forEach(archivo -> {
                    Path target = destino.resolve(archivo.getFileName().toString());
                    if (!Files.exists(target)) {
                        try {
                            Files.move(archivo, target, StandardCopyOption.REPLACE_EXISTING);
                            log.info("Archivo movido: {} -> {}", archivo, target);
                        } catch (IOException e) {
                            log.warn("No se pudo mover {}: {}", archivo, e.getMessage());
                        }
                    }
                });
            }
            try (Stream<Path> restantes = Files.list(origen)) {
                if (restantes.findAny().isEmpty()) {
                    Files.deleteIfExists(origen);
                }
            }
        } catch (IOException e) {
            log.warn("No se pudo procesar carpeta legacy {}: {}", origen, e.getMessage());
        }
    }

    private void normalizarRutasEnBaseDeDatos(JdbcTemplate jdbc) {
        if (!tablaExiste(jdbc, "imagen")) {
            return;
        }
        List<Map<String, Object>> filas = jdbc.queryForList("SELECT id, ruta FROM imagen");
        int actualizadas = 0;
        for (Map<String, Object> fila : filas) {
            Long id = ((Number) fila.get("id")).longValue();
            String rutaActual = String.valueOf(fila.get("ruta"));
            String rutaNueva = ImagenRutas.normalizarRuta(rutaActual);
            if (rutaNueva != null && !rutaNueva.equals(rutaActual)) {
                jdbc.update("UPDATE imagen SET ruta = ? WHERE id = ?", rutaNueva, id);
                actualizadas++;
            }
        }
        if (actualizadas > 0) {
            log.info("Rutas de imagen normalizadas en BD: {} registro(s).", actualizadas);
        }
    }

    private void migrarProductoImagenAPresentacion(JdbcTemplate jdbc) {
        if (!tablaExiste(jdbc, "producto_imagen") || !tablaExiste(jdbc, "presentacion_imagen")) {
            return;
        }
        int insertadas = jdbc.update("""
                INSERT INTO presentacion_imagen (presentacion_id, imagen_id, orden)
                SELECT pp.id, pi.imagen_id, pi.orden
                FROM producto_imagen pi
                INNER JOIN (
                    SELECT producto_id, MIN(id) AS presentacion_id
                    FROM producto_presentacion
                    GROUP BY producto_id
                ) primera ON primera.producto_id = pi.producto_id
                INNER JOIN producto_presentacion pp ON pp.id = primera.presentacion_id
                WHERE NOT EXISTS (
                    SELECT 1 FROM presentacion_imagen ppi
                    WHERE ppi.presentacion_id = pp.id AND ppi.imagen_id = pi.imagen_id
                )
                """);
        if (insertadas > 0) {
            log.info("Imágenes migradas de producto_imagen a presentacion_imagen: {}.", insertadas);
        }
    }

    private void sincronizarImagenPrincipalProducto(JdbcTemplate jdbc) {
        if (!tablaExiste(jdbc, "producto") || !tablaExiste(jdbc, "producto_presentacion")) {
            return;
        }
        int actualizadas = jdbc.update("""
                UPDATE producto_presentacion pp
                INNER JOIN producto p ON p.id = pp.producto_id
                INNER JOIN (
                    SELECT producto_id, MIN(id) AS presentacion_id
                    FROM producto_presentacion
                    GROUP BY producto_id
                ) primera ON primera.presentacion_id = pp.id
                SET pp.imagen_id = p.imagen_id
                WHERE p.imagen_id IS NOT NULL AND pp.imagen_id IS NULL
                """);
        if (actualizadas > 0) {
            log.info("Imagen principal de producto copiada a presentación: {} registro(s).", actualizadas);
        }
        jdbc.update("UPDATE producto SET imagen_id = NULL WHERE imagen_id IS NOT NULL");
    }

    private void eliminarTablaProductoImagen(JdbcTemplate jdbc) {
        if (!tablaExiste(jdbc, "producto_imagen")) {
            return;
        }
        try {
            jdbc.execute("DROP TABLE producto_imagen");
            log.info("Tabla legacy producto_imagen eliminada.");
        } catch (Exception e) {
            log.warn("No se pudo eliminar producto_imagen: {}", e.getMessage());
        }
    }

    private boolean tablaExiste(JdbcTemplate jdbc, String tabla) {
        Integer count = jdbc.queryForObject("""
                SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES
                WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ?
                """, Integer.class, tabla);
        return count != null && count > 0;
    }
}
