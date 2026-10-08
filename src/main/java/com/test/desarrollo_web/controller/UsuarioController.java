package com.test.desarrollo_web.controller;

import com.test.desarrollo_web.Models.Imagen;
import com.test.desarrollo_web.Models.Usuario;
import com.test.desarrollo_web.Repository.PerfilRepository;
import com.test.desarrollo_web.Repository.UsuarioRepository;
import com.test.desarrollo_web.config.ImageStorageCategory;
import com.test.desarrollo_web.service.FileStorageService;
import com.test.desarrollo_web.service.PasswordResetService;
import com.test.desarrollo_web.service.PermissionService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/usuarios")
public class UsuarioController {

    private final UsuarioRepository usuarioRepository;
    private final PerfilRepository perfilRepository;
    private final FileStorageService fileStorageService;
    private final PasswordEncoder passwordEncoder;
    private final PasswordResetService passwordResetService;

    public UsuarioController(UsuarioRepository usuarioRepository,
            PerfilRepository perfilRepository,
            FileStorageService fileStorageService,
            PasswordEncoder passwordEncoder,
            PasswordResetService passwordResetService) {
        this.usuarioRepository = usuarioRepository;
        this.perfilRepository = perfilRepository;
        this.fileStorageService = fileStorageService;
        this.passwordEncoder = passwordEncoder;
        this.passwordResetService = passwordResetService;
    }

    @GetMapping
    public String listar(Model model) {
        model.addAttribute("usuarios", usuarioRepository.findAll());
        model.addAttribute("perfiles", perfilRepository.findAll());
        model.addAttribute("nuevoUsuario", new Usuario()); // para el modal
        return "usuarios/list";
    }

    @PostMapping("/guardar")
    public String guardar(@RequestParam(required = false) Integer id,
                          @RequestParam String nombre,
                          @RequestParam String usuarioLogin,
                          @RequestParam String correo,
                          @RequestParam(required = false) String telefono,
                          @RequestParam(name = "perfilId") Integer perfilId,
                          @RequestParam Usuario.Estado estado,
                          @RequestParam(required = false) String password,
                          @RequestParam(value = "imagenFile", required = false) MultipartFile imagenFile,
                          RedirectAttributes redirectAttributes) {
        try {
            if (nombre == null || !nombre.matches("^[a-zA-ZáéíóúÁÉÍÓÚñÑüÜ\\s]+$")) {
                redirectAttributes.addFlashAttribute("error",
                        "El nombre solo puede contener letras (sin números ni caracteres especiales).");
                return "redirect:/usuarios";
            }
            if (usuarioLogin == null || !usuarioLogin.matches("^[a-zA-ZáéíóúÁÉÍÓÚñÑüÜ]+$")) {
                redirectAttributes.addFlashAttribute("error",
                        "El usuario solo puede contener letras (sin números ni caracteres especiales).");
                return "redirect:/usuarios";
            }

            Usuario usuario = id != null
                    ? usuarioRepository.findById(id).orElse(new Usuario())
                    : new Usuario();

            usuario.setNombre(nombre);
            usuario.setUsuario(usuarioLogin);
            usuario.setCorreo(correo);
            if (telefono != null && !telefono.isBlank()) {
                String telLimpio = telefono.replaceAll("\\D", "");
                if (!telLimpio.matches("^9\\d{8}$")) {
                    redirectAttributes.addFlashAttribute("error",
                            "El teléfono debe tener 9 dígitos y comenzar con 9 (ej: 987654321).");
                    return "redirect:/usuarios";
                }
                usuario.setTelefono(telLimpio);
            } else {
                usuario.setTelefono(null);
            }
            usuario.setEstado(estado);

            perfilRepository.findById(perfilId)
                    .ifPresentOrElse(usuario::setPerfil, () -> {
                        throw new RuntimeException("El perfil seleccionado no existe.");
                    });

            if (imagenFile != null && !imagenFile.isEmpty()) {
                Imagen imagen = fileStorageService.saveFile(imagenFile, ImageStorageCategory.PERFILES);
                usuario.setImagen(imagen);
            } else if (id != null) {
                usuarioRepository.findById(id).ifPresent(existente ->
                        usuario.setImagen(existente.getImagen()));
            }

            if (password != null && !password.isBlank()) {
                usuario.setPassword(passwordEncoder.encode(password));
            } else if (id != null) {
                usuarioRepository.findById(id).ifPresent(existente ->
                        usuario.setPassword(existente.getPassword()));
            } else {
                redirectAttributes.addFlashAttribute("error", "La contraseña es obligatoria para nuevos empleados de la bodega.");
                return "redirect:/usuarios";
            }

            usuarioRepository.save(usuario);
            redirectAttributes.addFlashAttribute("success", "Usuario guardado correctamente.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error al guardar: " + e.getMessage());
        }
        return "redirect:/usuarios";
    }

    @PostMapping("/{id}/actualizar-contrasena")
    public String actualizarContrasena(@PathVariable Integer id,
                                       @RequestParam String password,
                                       @RequestParam String confirmacion,
                                       RedirectAttributes redirectAttributes) {
        var resultado = passwordResetService.actualizarContrasenaDirecta(id, password, confirmacion);
        if (resultado.exito()) {
            redirectAttributes.addFlashAttribute("success", resultado.mensaje());
        } else {
            redirectAttributes.addFlashAttribute("error", resultado.mensaje());
        }
        return "redirect:/usuarios";
    }

    @PostMapping("/{id}/enviar-enlace-contrasena")
    public String enviarEnlaceContrasena(@PathVariable Integer id,
                                         HttpServletRequest request,
                                         RedirectAttributes redirectAttributes) {
        try {
            var resultado = passwordResetService.enviarEnlacePorUsuario(id, construirBaseUrl(request));
            if (resultado.exito()) {
                redirectAttributes.addFlashAttribute("success", resultado.mensaje());
            } else {
                redirectAttributes.addFlashAttribute("error", resultado.mensaje());
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage() != null
                    ? e.getMessage()
                    : "No se pudo enviar el enlace de recuperación.");
        }
        return "redirect:/usuarios";
    }

    private String construirBaseUrl(HttpServletRequest request) {
        String scheme = request.getScheme();
        String server = request.getServerName();
        int port = request.getServerPort();
        String context = request.getContextPath() != null ? request.getContextPath() : "";

        boolean puertoEstandar = ("http".equals(scheme) && port == 80)
                || ("https".equals(scheme) && port == 443);
        String puerto = puertoEstandar ? "" : ":" + port;
        return scheme + "://" + server + puerto + context;
    }

    @GetMapping("/eliminar/{id}")
    public String eliminar(@PathVariable Integer id, RedirectAttributes redirectAttributes) {
        try {
            Usuario usuario = usuarioRepository.findById(id).orElse(null);
            if (usuario == null) {
                redirectAttributes.addFlashAttribute("error", "El usuario no existe.");
                return "redirect:/usuarios";
            }
            if (PermissionService.esUsuarioAdminProtegido(usuario)) {
                redirectAttributes.addFlashAttribute("error",
                        "El usuario administrador principal no puede eliminarse.");
                return "redirect:/usuarios";
            }
            if (usuario.getImagen() != null) {
                fileStorageService.deleteFile(usuario.getImagen().getRuta());
            }
            usuarioRepository.deleteById(id);
            redirectAttributes.addFlashAttribute("success", "Usuario eliminado correctamente.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error al eliminar: " + e.getMessage());
        }
        return "redirect:/usuarios";
    }
}
