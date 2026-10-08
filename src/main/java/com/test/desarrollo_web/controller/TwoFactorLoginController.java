package com.test.desarrollo_web.controller;

import com.test.desarrollo_web.security.CustomUserDetailsService;
import com.test.desarrollo_web.security.TwoFactorSessionKeys;
import com.test.desarrollo_web.service.PermissionService;
import com.test.desarrollo_web.service.TwoFactorAuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/login")
public class TwoFactorLoginController {

    private final TwoFactorAuthService twoFactorAuthService;
    private final CustomUserDetailsService userDetailsService;
    private final PermissionService permissionService;
    private final SecurityContextRepository securityContextRepository;

    public TwoFactorLoginController(TwoFactorAuthService twoFactorAuthService,
                                    CustomUserDetailsService userDetailsService,
                                    PermissionService permissionService,
                                    SecurityContextRepository securityContextRepository) {
        this.twoFactorAuthService = twoFactorAuthService;
        this.userDetailsService = userDetailsService;
        this.permissionService = permissionService;
        this.securityContextRepository = securityContextRepository;
    }

    @GetMapping("/verificar")
    public String mostrarVerificacion(HttpSession session, Model model) {
        if (Boolean.TRUE.equals(session.getAttribute(TwoFactorSessionKeys.CODIGO_CORRECTO))) {
            String destino = destinoSeguro((String) session.getAttribute(TwoFactorSessionKeys.REDIRECT_AFTER));
            session.removeAttribute(TwoFactorSessionKeys.CODIGO_CORRECTO);
            session.removeAttribute(TwoFactorSessionKeys.REDIRECT_AFTER);
            model.addAttribute("codigoCorrecto", true);
            model.addAttribute("redirectUrl", destino);
            return "login-2fa";
        }
        if (!twoFactorAuthService.tieneVerificacionPendiente(session)) {
            return "redirect:/login";
        }
        prepararModeloVerificacion(session, model);
        return "login-2fa";
    }

    @PostMapping("/verificar")
    public String verificarCodigo(@RequestParam String codigo,
                                  HttpSession session,
                                  HttpServletRequest request,
                                  HttpServletResponse response,
                                  Model model) {
        if (!twoFactorAuthService.tieneVerificacionPendiente(session)) {
            return "redirect:/login";
        }

        if (!twoFactorAuthService.validarCodigo(session, codigo)) {
            prepararModeloVerificacion(session, model);
            model.addAttribute("errorCodigo", true);
            return "login-2fa";
        }

        String username = (String) session.getAttribute(TwoFactorSessionKeys.PENDING_USER);
        UserDetails userDetails = userDetailsService.loadUserByUsername(username);

        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(userDetails, userDetails.getPassword(), userDetails.getAuthorities());

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        securityContextRepository.saveContext(context, request, response);

        String destino = destinoSeguro(permissionService.getRutaInicio());
        twoFactorAuthService.limpiarSesion(session);
        session.setAttribute(TwoFactorSessionKeys.CODIGO_CORRECTO, true);
        session.setAttribute(TwoFactorSessionKeys.REDIRECT_AFTER, destino);

        return "redirect:/login/verificar";
    }

    @PostMapping("/reenviar-codigo")
    public String reenviarCodigo(HttpSession session, Model model) {
        if (!twoFactorAuthService.tieneVerificacionPendiente(session)) {
            return "redirect:/login";
        }

        boolean enviado = twoFactorAuthService.reenviarCodigo(session);
        prepararModeloVerificacion(session, model);
        if (enviado) {
            model.addAttribute("codigoReenviado", true);
        } else {
            model.addAttribute("errorReenvio", true);
        }
        return "login-2fa";
    }

    private void prepararModeloVerificacion(HttpSession session, Model model) {
        model.addAttribute("emailMask", session.getAttribute(TwoFactorSessionKeys.EMAIL_MASK));
        model.addAttribute("envioEmailOk", session.getAttribute(TwoFactorSessionKeys.ENVIO_EMAIL_OK));
        model.addAttribute("errorEnvio", session.getAttribute(TwoFactorSessionKeys.ERROR_ENVIO));
        if (Boolean.TRUE.equals(session.getAttribute(TwoFactorSessionKeys.MOSTRAR_CODIGO_PANTALLA))) {
            model.addAttribute("codigoPantalla", session.getAttribute(TwoFactorSessionKeys.OTP_CODE));
        }
    }

    private String destinoSeguro(String ruta) {
        if (ruta == null || !ruta.startsWith("/") || ruta.startsWith("//")) {
            return "/";
        }
        return ruta;
    }
}
