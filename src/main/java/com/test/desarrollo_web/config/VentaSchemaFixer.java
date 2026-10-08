package com.test.desarrollo_web.config;



import org.slf4j.Logger;

import org.slf4j.LoggerFactory;

import org.springframework.boot.CommandLineRunner;

import org.springframework.context.annotation.Bean;

import org.springframework.context.annotation.Configuration;

import org.springframework.core.annotation.Order;

import org.springframework.jdbc.core.JdbcTemplate;



@Configuration

public class VentaSchemaFixer {



    private static final Logger log = LoggerFactory.getLogger(VentaSchemaFixer.class);



    private static final String ESTADO_VENTA_ENUM =

            "ENUM('PENDIENTE','PAGADO','ANULADO') NULL DEFAULT 'PAGADO'";



    private static final String ESTADO_CUOTA_ENUM =

            "ENUM('PENDIENTE','PAGADA') NULL DEFAULT 'PENDIENTE'";



    @Bean

    @Order(10)

    CommandLineRunner alinearEsquemaVenta(JdbcTemplate jdbcTemplate, MotorBaseDatos motor) {

        return args -> {

            completarFechasVacias(jdbcTemplate);

            if (motor.esPostgres()) {
                log.info("PostgreSQL: Hibernate crea las tablas. Se omite el SQL de MySQL.");
                return;
            }

            for (String tabla : new String[]{"venta", "VENTA", "ventas"}) {

                normalizarEstadosVenta(jdbcTemplate, tabla);

            }

            for (String tabla : new String[]{"venta_cuota", "VENTA_CUOTA"}) {

                normalizarEstadosCuota(jdbcTemplate, tabla);

                agregarColumnasCuota(jdbcTemplate, tabla);

            }

            crearTablaCuotasSiFalta(jdbcTemplate);

        };

    }



    private void completarFechasVacias(JdbcTemplate jdbc) {
        for (String tabla : new String[]{"venta", "VENTA"}) {
            try {
                int filas = jdbc.update("UPDATE " + tabla + " SET fecha = CURRENT_TIMESTAMP WHERE fecha IS NULL");
                if (filas > 0) {
                    log.info("Fecha asignada a {} venta(s) sin fecha en {}", filas, tabla);
                }
                return;
            } catch (Exception e) {
                log.debug("No se pudo completar fecha en {}: {}", tabla, e.getMessage());
            }
        }
    }

    private void normalizarEstadosVenta(JdbcTemplate jdbc, String tabla) {

        try {

            jdbc.execute("UPDATE " + tabla + " SET estado = 'PAGADO' WHERE estado = 'COMPLETADA'");

            jdbc.execute("ALTER TABLE " + tabla + " MODIFY COLUMN estado " + ESTADO_VENTA_ENUM);

            log.info("Tabla {} alineada con ENUM PENDIENTE/PAGADO/ANULADO", tabla);

        } catch (Exception e) {

            log.debug("Sin cambios en {}: {}", tabla, e.getMessage());

        }

    }



    private void normalizarEstadosCuota(JdbcTemplate jdbc, String tabla) {

        try {

            jdbc.execute("ALTER TABLE " + tabla + " MODIFY COLUMN estado " + ESTADO_CUOTA_ENUM);

            log.info("Tabla {} alineada con ENUM PENDIENTE/PAGADA", tabla);

        } catch (Exception e) {

            log.debug("Sin cambios en {}: {}", tabla, e.getMessage());

        }

    }



    private void agregarColumnasCuota(JdbcTemplate jdbc, String tabla) {
        try {
            jdbc.execute("ALTER TABLE " + tabla + " ADD COLUMN fecha_pago_real DATE NULL");
        } catch (Exception e) {
            log.debug("Columna fecha_pago_real ya existe en {}: {}", tabla, e.getMessage());
        }
        try {
            jdbc.execute("ALTER TABLE " + tabla + " ADD COLUMN medio_pago VARCHAR(30) NULL");
        } catch (Exception e) {
            log.debug("Columna medio_pago ya existe en {}: {}", tabla, e.getMessage());
        }
    }

    private void crearTablaCuotasSiFalta(JdbcTemplate jdbc) {

        try {

            jdbc.execute("""

                    CREATE TABLE IF NOT EXISTS VENTA_CUOTA (

                        id BIGINT AUTO_INCREMENT PRIMARY KEY,

                        id_venta BIGINT NOT NULL,

                        numero_cuota INT NOT NULL,

                        fecha_pago DATE NOT NULL,

                        monto DECIMAL(10,2) NOT NULL,

                        estado ENUM('PENDIENTE','PAGADA') NULL DEFAULT 'PENDIENTE',

                        fecha_pago_real DATE NULL,

                        medio_pago VARCHAR(30) NULL,

                        CONSTRAINT fk_cuota_venta FOREIGN KEY (id_venta) REFERENCES VENTA(id)

                    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4

                    """);

            log.info("Tabla VENTA_CUOTA verificada/creada");

        } catch (Exception e) {

            log.debug("VENTA_CUOTA ya existe o no se pudo crear: {}", e.getMessage());

        }

    }

}


