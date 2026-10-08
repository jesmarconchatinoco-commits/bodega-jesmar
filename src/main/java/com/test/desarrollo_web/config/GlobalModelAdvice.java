package com.test.desarrollo_web.config;

import com.test.desarrollo_web.Models.Modulo;
import com.test.desarrollo_web.Models.Usuario;
import com.test.desarrollo_web.service.LogoService;
import com.test.desarrollo_web.service.PermissionService;
import com.test.desarrollo_web.service.ValidacionDniRucService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.util.List;

@ControllerAdvice
public class GlobalModelAdvice {

    private final PermissionService permissionService;
    private final LogoService logoService;
    private final ValidacionDniRucService validacionDniRucService;

    public GlobalModelAdvice(PermissionService permissionService,
                             LogoService logoService,
                             ValidacionDniRucService validacionDniRucService) {
        this.permissionService = permissionService;
        this.logoService = logoService;
        this.validacionDniRucService = validacionDniRucService;
    }

    @ModelAttribute("menuModulos")
    public List<Modulo> menuModulos() {
        return permissionService.getModulosPermitidos();
    }

    @ModelAttribute("esAdministrador")
    public boolean esAdministrador() {
        return permissionService.esAdministrador();
    }

    @ModelAttribute("usuarioActual")
    public String usuarioActual() {
        Usuario usuario = permissionService.getUsuarioActual();
        return usuario != null ? usuario.getUsuario() : "Usuario";
    }

    @ModelAttribute("moduloActivo")
    public String moduloActivo(HttpServletRequest request) {
        return permissionService.resolverCodigoModulo(request.getRequestURI());
    }

    @ModelAttribute("logoUrl")
    public String logoUrl() {
        return logoService.getLogoUrl().orElse(null);
    }

    @ModelAttribute("miapiConfigurado")
    public boolean miapiConfigurado() {
        return validacionDniRucService.estaConfigurada();
    }
}
