package com.test.desarrollo_web.security;

import com.test.desarrollo_web.service.TwoFactorAuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Tras validar usuario/contraseña (autenticación), inicia el segundo paso (2FA)
 * antes de conceder acceso completo al sistema.
 */
@Component
public class TwoFactorAuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    private final TwoFactorAuthService twoFactorAuthService;

    public TwoFactorAuthenticationSuccessHandler(TwoFactorAuthService twoFactorAuthService) {
        this.twoFactorAuthService = twoFactorAuthService;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        if (!twoFactorAuthService.estaHabilitado()) {
            response.sendRedirect(request.getContextPath() + "/dashboard");
            return;
        }

        HttpSession session = request.getSession(true);
        String username = authentication.getName();

        SecurityContextHolder.clearContext();
        twoFactorAuthService.iniciarVerificacion(username, session);

        response.sendRedirect(request.getContextPath() + "/login/verificar");
    }
}
