package com.test.desarrollo_web.controller;

import com.test.desarrollo_web.Models.Cliente;
import com.test.desarrollo_web.Models.Imagen;
import com.test.desarrollo_web.Repository.ClienteRepository;
import com.test.desarrollo_web.config.ImageStorageCategory;
import com.test.desarrollo_web.service.ClienteService;
import com.test.desarrollo_web.service.FileStorageService;
import com.test.desarrollo_web.service.ValidacionDniRucService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Map;
import java.util.Optional;

@Controller
@RequestMapping("/clientes")
public class ClienteController {

    private final ClienteRepository clienteRepository;
    private final ClienteService clienteService;
    private final FileStorageService fileStorageService;
    private final ValidacionDniRucService validacionDniRucService;

    public ClienteController(ClienteRepository clienteRepository,
                             ClienteService clienteService,
                             FileStorageService fileStorageService,
                             ValidacionDniRucService validacionDniRucService) {
        this.clienteRepository = clienteRepository;
        this.clienteService = clienteService;
        this.fileStorageService = fileStorageService;
        this.validacionDniRucService = validacionDniRucService;
    }

    @GetMapping
    public String listar(Model model) {
        model.addAttribute("clientes", clienteRepository.findAll());
        return "clientes/list";
    }

    @GetMapping("/api/local")
    @ResponseBody
    public Map<String, Object> buscarLocal(@RequestParam String documento) {
        return clienteService.buscarSoloEnBodega(documento);
    }

    @GetMapping("/api/buscar")
    @ResponseBody
    public Map<String, Object> buscarDocumento(@RequestParam String documento) {
        Map<String, Object> local = clienteService.buscarSoloEnBodega(documento);
        if (Boolean.TRUE.equals(local.get("found"))) {
            return local;
        }

        String doc = clienteService.normalizarDocumento(documento);
        if (doc.isBlank()) {
            return Map.of("success", false, "message", "Ingrese el DNI o RUC del cliente.");
        }

        Map<String, Object> validacion = validacionDniRucService.validarDocumento(doc);
        if (Boolean.FALSE.equals(validacion.get("success"))) {
            return Map.of(
                    "success", false,
                    "message", validacion.getOrDefault("message", "No se pudo validar el documento.")
            );
        }

        String nombre = validacionDniRucService.extraerNombre(validacion);
        return Map.of(
                "success", true,
                "found", false,
                "origen", Boolean.TRUE.equals(validacion.get("modoManual")) ? "manual" : "api",
                "nombre", nombre != null ? nombre : "",
                "documento", doc,
                "telefono", "",
                "correo", "",
                "estado", "ACTIVO",
                "message", Boolean.TRUE.equals(validacion.get("modoManual"))
                        ? "Cliente no registrado. Complete el nombre y guarde."
                        : "Documento validado con ApiPeru. Complete los datos y guarde el cliente."
        );
    }

    @PostMapping("/guardar")
    public String guardar(@RequestParam(required = false) Integer id,
                          @RequestParam String documento,
                          @RequestParam String nombre,
                          @RequestParam(required = false) String telefono,
                          @RequestParam(required = false) String correo,
                          @RequestParam(defaultValue = "ACTIVO") String estado,
                          @RequestParam(value = "imagenFile", required = false) MultipartFile imagenFile,
                          RedirectAttributes redirectAttributes) {
        String doc = clienteService.normalizarDocumento(documento);
        String nombreCliente = nombre != null ? nombre.trim() : "";

        if (doc.isBlank() || nombreCliente.isBlank()) {
            redirectAttributes.addFlashAttribute("error", "El documento y el nombre del cliente son obligatorios.");
            return "redirect:/clientes";
        }

        Optional<Cliente> duplicado = clienteService.buscarEnBodega(doc);
        if (duplicado.isPresent() && (id == null || !duplicado.get().getId().equals(id))) {
            redirectAttributes.addFlashAttribute("error", "Ya existe un cliente con ese DNI/RUC.");
            return "redirect:/clientes";
        }

        Cliente cliente = id != null
                ? clienteRepository.findById(id).orElse(new Cliente())
                : new Cliente();

        if (id == null && clienteService.buscarEnBodega(doc).isEmpty()) {
            Map<String, Object> validacion;
            if (validacionDniRucService.estaConfigurada() && nombreCliente.isBlank()) {
                validacion = validacionDniRucService.validarDocumento(doc);
            } else {
                validacion = validacionDniRucService.validarFormatoLocal(doc);
            }
            if (Boolean.FALSE.equals(validacion.get("success"))) {
                redirectAttributes.addFlashAttribute("error", validacion.get("message"));
                return "redirect:/clientes";
            }
        }

        cliente.setDocumento(doc);
        cliente.setNombre(nombreCliente);
        cliente.setTelefono(telefono != null ? telefono.trim() : null);
        cliente.setCorreo(correo != null ? correo.trim() : null);
        cliente.setEstado(Cliente.Estado.valueOf(estado));

        if (imagenFile != null && !imagenFile.isEmpty()) {
            Imagen imagen = fileStorageService.saveFile(imagenFile, ImageStorageCategory.CLIENTES);
            cliente.setImagen(imagen);
        }

        clienteRepository.save(cliente);
        redirectAttributes.addFlashAttribute("success", id != null
                ? "Cliente actualizado correctamente"
                : "Cliente registrado correctamente");
        return "redirect:/clientes";
    }

    @GetMapping("/eliminar/{id}")
    public String eliminar(@PathVariable Integer id, RedirectAttributes redirectAttributes) {
        try {
            Cliente cliente = clienteRepository.findById(id).orElse(null);
            if (cliente != null && cliente.getImagen() != null) {
                fileStorageService.deleteFile(cliente.getImagen().getRuta());
            }
            clienteRepository.deleteById(id);
            redirectAttributes.addFlashAttribute("success", "Cliente eliminado correctamente");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error al eliminar: " + e.getMessage());
        }
        return "redirect:/clientes";
    }

}
