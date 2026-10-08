package com.test.desarrollo_web.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Si hay verificación 2FA pendiente en sesión, solo permite rutas de login/verificación
 * hasta completar el segundo factor.
 */
@Component
public class TwoFactorEnforcementFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute(TwoFactorSessionKeys.PENDING_USER) == null) {
            filterChain.doFilter(request, response);
            return;
        }

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean autenticado = auth != null
                && auth.isAuthenticated()
                && !"anonymousUser".equals(String.valueOf(auth.getPrincipal()));

        if (!autenticado && !esRutaPermitidaDurante2fa(request.getRequestURI())) {
            response.sendRedirect(request.getContextPath() + "/login/verificar");
            return;
        }

        filterChain.doFilter(request, response);
    }

    private boolean esRutaPermitidaDurante2fa(String path) {
        if (path == null) {
            return false;
        }
        return path.equals("/login")
                || path.equals("/login/verificar")
                || path.equals("/login/reenviar-codigo")
                || path.equals("/recuperar-contrasena")
                || path.startsWith("/recuperar-contrasena/")
                || path.equals("/logout")
                || path.startsWith("/css/")
                || path.startsWith("/js/")
                || path.startsWith("/img/")
                || path.startsWith("/uploads/")
                || path.startsWith("/imagen/")
                || path.startsWith("/archivos/")
                || path.startsWith("/catalogo")
                || path.equals("/error")
                || path.equals("/version")
                || path.equals("/salud");
    }
}
