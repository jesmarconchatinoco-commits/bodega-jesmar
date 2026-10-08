package com.test.desarrollo_web.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.sql.Connection;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
public class VersionController {

    private final DataSource dataSource;

    @Value("${spring.application.name:sistema-ventas-jesmar}")
    private String aplicacion;

    @Value("${app.version:1.0.0-candidato}")
    private String version;

    public VersionController(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @GetMapping(value = "/version", produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, String> version() {
        Map<String, String> cuerpo = new LinkedHashMap<>();
        cuerpo.put("aplicacion", aplicacion);
        cuerpo.put("version", version);
        cuerpo.put("etiqueta", "v1.0.0-candidato");
        cuerpo.put("runtime", "Java " + Runtime.version().feature());
        cuerpo.put("fecha", LocalDate.now().toString());
        return cuerpo;
    }

    @GetMapping(value = "/salud", produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, String> salud() {
        Map<String, String> cuerpo = new LinkedHashMap<>();
        try (Connection conexion = dataSource.getConnection()) {
            boolean conectada = conexion.isValid(3);
            cuerpo.put("estado", conectada ? "ok" : "error");
            cuerpo.put("baseDatos", conectada ? "conectada" : "no disponible");
        } catch (Exception ex) {
            cuerpo.put("estado", "error");
            cuerpo.put("baseDatos", "no disponible");
        }
        return cuerpo;
    }
}
