package com.test.desarrollo_web.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Service
public class ValidacionDniRucService {

    private static final String PANEL_URL = "https://apiperu.dev/app/dashboard";

    private final RestTemplate restTemplate = new RestTemplate();
    private final String apiBaseUrl;
    private final String apiToken;
    private final boolean habilitada;

    public ValidacionDniRucService(
            @Value("${app.validacion.api-url:https://apiperu.dev/api}") String apiBaseUrl,
            @Value("${app.validacion.api-token:}") String apiToken,
            @Value("${app.validacion.habilitada:false}") boolean habilitada) {
        this.apiBaseUrl = apiBaseUrl != null ? apiBaseUrl.replaceAll("/+$", "") : "https://apiperu.dev/api";
        this.apiToken = apiToken != null ? apiToken.trim() : "";
        this.habilitada = habilitada;
    }

    public boolean estaConfigurada() {
        return habilitada && !apiToken.isBlank();
    }

    public boolean esFormatoValido(String documento) {
        if (documento == null || documento.isBlank()) {
            return false;
        }
        String doc = documento.replaceAll("\\D", "").trim();
        return doc.length() == 8 || doc.length() == 11;
    }

    public Map<String, Object> validarFormatoLocal(String documento) {
        if (documento == null || documento.isBlank()) {
            return Map.of("success", false, "message", "Ingrese el DNI o RUC del cliente de la bodega.");
        }

        String doc = documento.replaceAll("\\D", "").trim();
        if (doc.length() == 8 || doc.length() == 11) {
            return Map.of("success", true, "documento", doc, "message", "Formato de documento válido.");
        }
        return Map.of("success", false, "message", "El cliente debe tener DNI (8 dígitos) o RUC (11 dígitos).");
    }

    public Map<String, Object> validarDocumento(String documento) {
        if (documento == null || documento.isBlank()) {
            return Map.of("success", false, "message", "Ingrese el DNI o RUC del cliente de la bodega.");
        }

        String doc = documento.replaceAll("\\D", "").trim();
        String tipo = doc.length() == 8 ? "dni" : doc.length() == 11 ? "ruc" : "unknown";
        if ("unknown".equals(tipo)) {
            return Map.of("success", false, "message", "El cliente debe tener DNI (8 dígitos) o RUC (11 dígitos) para facturar.");
        }

        if (!estaConfigurada()) {
            Map<String, Object> manual = new HashMap<>();
            manual.put("success", true);
            manual.put("modoManual", true);
            manual.put("documento", doc);
            manual.put("message", "ApiPeru no está activo. Ingrese el nombre del cliente manualmente.");
            return manual;
        }

        String requestUrl = apiBaseUrl + "/" + tipo;
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(apiToken);
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setAccept(MediaType.parseMediaTypes("application/json"));

            Map<String, String> body = Map.of(tipo, doc);
            HttpEntity<Map<String, String>> entity = new HttpEntity<>(body, headers);

            @SuppressWarnings("unchecked")
            ResponseEntity<Map<String, Object>> response = (ResponseEntity<Map<String, Object>>) (ResponseEntity<?>)
                    restTemplate.exchange(requestUrl, HttpMethod.POST, entity, Map.class);

            if (!response.getStatusCode().is2xxSuccessful()) {
                return Map.of("success", false, "message", "No se pudo validar el DNI/RUC. Verifique el número e intente de nuevo.");
            }

            Map<String, Object> responseBody = response.getBody();
            if (responseBody == null || responseBody.isEmpty()) {
                return Map.of("success", false, "message", "No se recibió respuesta al validar el documento del cliente.");
            }

            return normalizarRespuesta(responseBody);
        } catch (HttpStatusCodeException ex) {
            if (ex.getStatusCode().value() == 401 || ex.getStatusCode().value() == 403) {
                String detalle = extraerMensajeErrorApi(ex.getResponseBodyAsString());
                return Map.of("success", false, "message",
                        detalle != null ? detalle
                                : "Token de ApiPeru inválido. Obtenga uno en " + PANEL_URL);
            }
            return Map.of("success", false, "message", "No se pudo validar el documento (HTTP " + ex.getStatusCode().value() + ").");
        } catch (Exception ex) {
            return Map.of("success", false, "message", "No se pudo validar el documento del cliente: " + ex.getMessage());
        }
    }

    public String extraerNombre(Map<String, Object> data) {
        if (data == null || data.isEmpty()) {
            return "";
        }
        if (data.get("nombre") != null && !data.get("nombre").toString().isBlank()) {
            return data.get("nombre").toString().trim();
        }
        if (data.get("nombre_completo") != null && !data.get("nombre_completo").toString().isBlank()) {
            return data.get("nombre_completo").toString().trim();
        }
        if (data.get("razonSocial") != null && !data.get("razonSocial").toString().isBlank()) {
            return data.get("razonSocial").toString().trim();
        }
        if (data.get("nombre_o_razon_social") != null && !data.get("nombre_o_razon_social").toString().isBlank()) {
            return data.get("nombre_o_razon_social").toString().trim();
        }

        String nombres = data.getOrDefault("nombres", "").toString().trim();
        String paterno = data.getOrDefault("apellidoPaterno",
                data.getOrDefault("apellido_paterno", data.getOrDefault("ape_paterno", ""))).toString().trim();
        String materno = data.getOrDefault("apellidoMaterno",
                data.getOrDefault("apellido_materno", data.getOrDefault("ape_materno", ""))).toString().trim();
        return (nombres + " " + paterno + " " + materno).trim();
    }

    private String extraerMensajeErrorApi(String body) {
        if (body == null || body.isBlank()) {
            return null;
        }
        if (body.contains("Unauthenticated") || body.contains("unauthenticated")) {
            return "Token de ApiPeru inválido. Obtenga su token en " + PANEL_URL;
        }
        if (body.contains("error") || body.contains("message")) {
            return "Error de ApiPeru. Verifique su token en " + PANEL_URL;
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> normalizarRespuesta(Map<String, Object> body) {
        Map<String, Object> result = new HashMap<>();

        Object success = body.get("success");
        if (success instanceof Boolean boolSuccess && !boolSuccess) {
            result.put("success", false);
            result.put("message", body.getOrDefault("message", "Documento no encontrado en ApiPeru."));
            return result;
        }

        Object datosObj = body.get("data");
        if (datosObj == null) {
            datosObj = body.get("datos");
        }

        if (datosObj instanceof Map<?, ?> datosRaw) {
            Map<String, Object> datos = (Map<String, Object>) datosRaw;
            result.putAll(datos);

            if (datos.containsKey("nombre_completo")) {
                result.put("nombre", datos.get("nombre_completo"));
            }
            if (datos.containsKey("nombre_o_razon_social")) {
                result.put("razonSocial", datos.get("nombre_o_razon_social"));
            }
            if (datos.containsKey("razon_social")) {
                result.put("razonSocial", datos.get("razon_social"));
            }
            if (datos.containsKey("apellido_paterno")) {
                result.put("apellidoPaterno", datos.get("apellido_paterno"));
            }
            if (datos.containsKey("apellido_materno")) {
                result.put("apellidoMaterno", datos.get("apellido_materno"));
            }
            if (datos.containsKey("ape_paterno")) {
                result.put("apellidoPaterno", datos.get("ape_paterno"));
            }
            if (datos.containsKey("ape_materno")) {
                result.put("apellidoMaterno", datos.get("ape_materno"));
            }
        } else {
            result.putAll(body);
        }

        String nombre = extraerNombre(result);
        if (!nombre.isBlank()) {
            result.put("nombre", nombre);
        }

        if (nombre.isBlank()) {
            result.put("success", false);
            result.put("message", "No se encontró nombre para este DNI/RUC en ApiPeru.");
            return result;
        }

        result.put("success", true);
        result.putIfAbsent("message", "Documento del cliente validado correctamente");
        return result;
    }
}
