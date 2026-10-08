package com.test.desarrollo_web.controller;

import com.test.desarrollo_web.Models.PasswordResetToken;
import com.test.desarrollo_web.service.PasswordResetService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/recuperar-contrasena")
public class PasswordResetController {

    private final PasswordResetService passwordResetService;

    public PasswordResetController(PasswordResetService passwordResetService) {
        this.passwordResetService = passwordResetService;
    }

    @GetMapping
    public String formularioCorreo(Model model) {
        model.addAttribute("mostrarFormulario", true);
        return "recuperar-contrasena";
    }

    @PostMapping
    public String enviarEnlace(@RequestParam String correo,
                               HttpServletRequest request,
                               Model model) {
        try {
            var resultado = passwordResetService.solicitarRestablecimiento(correo, construirBaseUrl(request));
            if (resultado.exito()) {
                model.addAttribute("mensajeExito", resultado.mensaje());
                model.addAttribute("correoEnviado", true);
                model.addAttribute("mostrarFormulario", false);
            } else {
                model.addAttribute("mensajeError", resultado.mensaje());
                model.addAttribute("mostrarFormulario", true);
            }
        } catch (Exception e) {
            model.addAttribute("mensajeError", e.getMessage() != null
                    ? e.getMessage()
                    : "No se pudo procesar la solicitud.");
            model.addAttribute("mostrarFormulario", true);
        }
        return "recuperar-contrasena";
    }

    @GetMapping("/restablecer")
    public String formularioNuevaContrasena(@RequestParam(required = false) String token, Model model) {
        if (token == null || token.isBlank()) {
            model.addAttribute("mensajeError", "El enlace no es válido. Solicite uno nuevo.");
            model.addAttribute("mostrarFormulario", true);
            return "recuperar-contrasena";
        }

        PasswordResetToken resetToken = passwordResetService.obtenerTokenValido(token);
        if (resetToken == null) {
            model.addAttribute("mensajeError", "El enlace no es válido o ha expirado. Solicite uno nuevo.");
            model.addAttribute("mostrarFormulario", true);
            return "recuperar-contrasena";
        }

        model.addAttribute("token", token);
        return "restablecer-contrasena";
    }

    @PostMapping("/restablecer")
    public String guardarNuevaContrasena(@RequestParam String token,
                                         @RequestParam String password,
                                         @RequestParam String confirmacion,
                                         Model model) {
        var resultado = passwordResetService.restablecerContrasena(token, password, confirmacion);
        if (resultado.exito()) {
            model.addAttribute("mensajeExito", resultado.mensaje());
            model.addAttribute("mostrarFormulario", false);
            return "recuperar-contrasena";
        }

        PasswordResetToken resetToken = passwordResetService.obtenerTokenValido(token);
        if (resetToken == null) {
            model.addAttribute("mensajeError", resultado.mensaje());
            model.addAttribute("mostrarFormulario", true);
            return "recuperar-contrasena";
        }

        model.addAttribute("mensajeError", resultado.mensaje());
        model.addAttribute("token", token);
        return "restablecer-contrasena";
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
}
