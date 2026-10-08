package com.test.desarrollo_web.service;

import com.test.desarrollo_web.Models.Cliente;
import com.test.desarrollo_web.Repository.ClienteRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Service
public class ClienteService {

    private final ClienteRepository clienteRepository;
    private final ValidacionDniRucService validacionDniRucService;

    public ClienteService(ClienteRepository clienteRepository,
                        ValidacionDniRucService validacionDniRucService) {
        this.clienteRepository = clienteRepository;
        this.validacionDniRucService = validacionDniRucService;
    }

    public String normalizarDocumento(String documento) {
        if (documento == null) {
            return "";
        }
        return documento.replaceAll("\\D", "").trim();
    }

    @Transactional(readOnly = true)
    public Optional<Cliente> buscarEnBodega(String documento) {
        String doc = normalizarDocumento(documento);
        if (doc.isBlank()) {
            return Optional.empty();
        }

        Optional<Cliente> directo = clienteRepository.findByDocumento(doc);
        if (directo.isPresent()) {
            return directo;
        }

        return clienteRepository.findByDocumentoNormalizado(doc);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> buscarSoloEnBodega(String documento) {
        String doc = normalizarDocumento(documento);
        if (doc.isBlank()) {
            return Map.of("success", false, "message", "Ingrese el DNI o RUC del cliente.");
        }

        return buscarEnBodega(doc)
                .map(this::respuestaDesdeBodega)
                .orElseGet(() -> Map.of(
                        "success", true,
                        "found", false,
                        "origen", "bodega",
                        "documento", doc,
                        "message", "Cliente no registrado en la bodega."
                ));
    }

    @Transactional
    public Map<String, Object> buscarOCrearCliente(String documento, String nombreManual) {
        String doc = normalizarDocumento(documento);
        if (doc.isBlank()) {
            return Map.of("success", false, "message", "Ingrese el DNI o RUC del cliente.");
        }

        Optional<Cliente> existente = buscarEnBodega(doc);
        if (existente.isPresent()) {
            return respuestaDesdeBodega(existente.get());
        }

        Map<String, Object> validacion = validacionDniRucService.validarDocumento(doc);
        if (Boolean.FALSE.equals(validacion.get("success"))) {
            return Map.of(
                    "success", false,
                    "found", false,
                    "origen", "api",
                    "message", validacion.getOrDefault("message", "No se pudo validar el documento.")
            );
        }

        String nombre = validacionDniRucService.extraerNombre(validacion);
        if (Boolean.TRUE.equals(validacion.get("modoManual"))) {
            nombre = nombreManual != null ? nombreManual.trim() : "";
            if (nombre.isBlank()) {
                return Map.of(
                        "success", true,
                        "found", false,
                        "origen", "manual",
                        "requiereNombre", true,
                        "documento", doc,
                        "message", "Cliente no registrado. Escriba el nombre y pulse buscar de nuevo."
                );
            }
        } else if (nombre.isBlank()) {
            return Map.of(
                    "success", false,
                    "found", false,
                    "origen", "api",
                    "message", "No se obtuvo el nombre del DNI/RUC. Verifique el documento."
            );
        }

        Cliente nuevo = new Cliente();
        nuevo.setDocumento(doc);
        nuevo.setNombre(nombre);
        nuevo.setEstado(Cliente.Estado.ACTIVO);
        nuevo = clienteRepository.save(nuevo);

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("found", true);
        result.put("created", true);
        result.put("origen", Boolean.TRUE.equals(validacion.get("modoManual")) ? "manual" : "api");
        result.put("clienteId", nuevo.getId());
        result.put("nombre", nuevo.getNombre());
        result.put("documento", nuevo.getDocumento());
        result.put("message", "Nuevo cliente creado y seleccionado.");
        return result;
    }

    private Map<String, Object> respuestaDesdeBodega(Cliente cliente) {
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("found", true);
        result.put("created", false);
        result.put("origen", "bodega");
        result.put("clienteId", cliente.getId());
        result.put("nombre", cliente.getNombre());
        result.put("documento", cliente.getDocumento());
        result.put("telefono", cliente.getTelefono() != null ? cliente.getTelefono() : "");
        result.put("correo", cliente.getCorreo() != null ? cliente.getCorreo() : "");
        result.put("estado", cliente.getEstado() != null ? cliente.getEstado().name() : "ACTIVO");
        result.put("message", "Cliente encontrado en la bodega (sin consultar ApiPeru).");
        return result;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> consultarDocumento(String documento) {
        String doc = normalizarDocumento(documento);
        if (!validacionDniRucService.esFormatoValido(doc)) {
            return Map.of("success", false, "message", "Ingrese un DNI (8 dígitos) o RUC (11 dígitos).");
        }

        Optional<Cliente> existente = buscarEnBodega(doc);
        if (existente.isPresent()) {
            return respuestaDesdeBodega(existente.get());
        }

        Map<String, Object> validacion = validacionDniRucService.validarDocumento(doc);
        if (Boolean.FALSE.equals(validacion.get("success"))) {
            return Map.of(
                    "success", false,
                    "found", false,
                    "message", validacion.getOrDefault("message", "No se pudo validar el documento.")
            );
        }

        if (Boolean.TRUE.equals(validacion.get("modoManual"))) {
            return Map.of(
                    "success", true,
                    "found", false,
                    "requiereNombre", true,
                    "documento", doc,
                    "message", "Ingrese el nombre manualmente."
            );
        }

        String nombre = validacionDniRucService.extraerNombre(validacion);
        if (nombre.isBlank()) {
            return Map.of(
                    "success", false,
                    "found", false,
                    "message", "No se obtuvo el nombre del DNI/RUC. Verifique el documento."
            );
        }

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("found", true);
        result.put("origen", "api");
        result.put("documento", doc);
        result.put("nombre", nombre);
        result.put("message", "Nombre cargado automáticamente.");
        return result;
    }

    @Transactional
    public Map<String, Object> registrarDesdeCatalogo(String documento, String nombre, String telefono) {
        String doc = normalizarDocumento(documento);
        if (doc.length() != 8) {
            return Map.of("success", false, "message", "Ingrese un DNI válido de 8 dígitos.");
        }

        String nombreLimpio = nombre != null ? nombre.trim() : "";
        if (nombreLimpio.isBlank()) {
            return Map.of("success", false, "message", "Ingrese su nombre completo.");
        }

        String tel = telefono != null ? telefono.replaceAll("\\D", "").trim() : "";
        if (!tel.matches("^9\\d{8}$")) {
            return Map.of("success", false, "message", "El teléfono debe tener 9 dígitos y comenzar con 9.");
        }

        Optional<Cliente> existente = buscarEnBodega(doc);
        Cliente cliente;
        boolean actualizado;

        if (existente.isPresent()) {
            cliente = existente.get();
            cliente.setNombre(nombreLimpio);
            cliente.setTelefono(tel);
            actualizado = true;
        } else {
            cliente = new Cliente();
            cliente.setDocumento(doc);
            cliente.setNombre(nombreLimpio);
            cliente.setTelefono(tel);
            cliente.setEstado(Cliente.Estado.ACTIVO);
            actualizado = false;
        }

        cliente = clienteRepository.save(cliente);
        Map<String, Object> result = respuestaDesdeBodega(cliente);
        result.put("message", actualizado
                ? "Datos actualizados correctamente."
                : "Registro completado. Bienvenido a Bodega Jesmar.");
        return result;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> iniciarSesionCatalogo(String documento) {
        String doc = normalizarDocumento(documento);
        if (doc.length() != 8) {
            return Map.of("success", false, "message", "Ingrese un DNI válido de 8 dígitos.");
        }

        Optional<Cliente> existente = buscarEnBodega(doc);
        if (existente.isPresent()) {
            Map<String, Object> result = respuestaDesdeBodega(existente.get());
            result.put("message", "Bienvenido, " + existente.get().getNombre() + ".");
            return result;
        }

        Map<String, Object> consulta = consultarDocumento(doc);
        if (!Boolean.TRUE.equals(consulta.get("success"))) {
            return consulta;
        }

        if (Boolean.TRUE.equals(consulta.get("requiereNombre"))) {
            consulta.put("message", "No encontramos su DNI. Regístrese completando sus datos.");
            return consulta;
        }

        if (Boolean.TRUE.equals(consulta.get("found"))) {
            consulta.put("telefono", "");
            consulta.put("message", "Sesión iniciada. Complete su teléfono al registrarse si aún no lo hizo.");
            return consulta;
        }

        return Map.of(
                "success", false,
                "found", false,
                "message", "No se encontró el DNI. Use la pestaña Registrarse."
        );
    }

    @Transactional
    public Cliente resolverClienteDesdePedido(String documento, String nombre, String telefono, Long pedidoId) {
        String nombreLimpio = nombre != null ? nombre.trim() : "";
        if (nombreLimpio.isBlank()) {
            nombreLimpio = "Cliente catálogo";
        }

        String doc = normalizarDocumento(documento);
        if (!doc.isBlank()) {
            Optional<Cliente> existente = buscarEnBodega(doc);
            if (existente.isPresent()) {
                Cliente cliente = existente.get();
                if (telefono != null && !telefono.isBlank()) {
                    cliente.setTelefono(telefono.trim());
                    clienteRepository.save(cliente);
                }
                return cliente;
            }
            Cliente nuevo = new Cliente();
            nuevo.setDocumento(doc);
            nuevo.setNombre(nombreLimpio);
            nuevo.setTelefono(telefono != null ? telefono.trim() : null);
            nuevo.setEstado(Cliente.Estado.ACTIVO);
            return clienteRepository.save(nuevo);
        }

        String tel = telefono != null ? telefono.replaceAll("\\D", "").trim() : "";
        String docCatalogo = tel.length() >= 8
                ? tel.substring(0, Math.min(tel.length(), 11))
                : "CAT" + String.format("%08d", pedidoId);

        Optional<Cliente> porDoc = clienteRepository.findByDocumento(docCatalogo);
        if (porDoc.isPresent()) {
            Cliente cliente = porDoc.get();
            cliente.setNombre(nombreLimpio);
            if (telefono != null && !telefono.isBlank()) {
                cliente.setTelefono(telefono.trim());
            }
            return clienteRepository.save(cliente);
        }

        Cliente nuevo = new Cliente();
        nuevo.setDocumento(docCatalogo);
        nuevo.setNombre(nombreLimpio);
        nuevo.setTelefono(telefono != null ? telefono.trim() : null);
        nuevo.setEstado(Cliente.Estado.ACTIVO);
        return clienteRepository.save(nuevo);
    }
}
