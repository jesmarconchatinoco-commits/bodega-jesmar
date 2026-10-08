package com.test.desarrollo_web.service;

import com.test.desarrollo_web.Models.Modulo;
import com.test.desarrollo_web.Models.Usuario;
import com.test.desarrollo_web.Repository.ModuloRepository;
import com.test.desarrollo_web.Repository.UsuarioRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class PermissionService {

    private static final Map<String, List<String>> RUTAS_POR_CODIGO = Map.ofEntries(
            Map.entry("DASHBOARD", List.of("/dashboard", "/")),
            Map.entry("USUARIOS", List.of("/usuarios")),
            Map.entry("PERFILES", List.of("/perfiles")),
            Map.entry("CATEGORIAS", List.of("/categorias")),
            Map.entry("PRODUCTOS", List.of("/productos")),
            Map.entry("INVENTARIO", List.of("/inventario")),
            Map.entry("SLIDER", List.of("/imagen-slider")),
            Map.entry("VENTAS", List.of("/ventas")),
            Map.entry("CLIENTES", List.of("/clientes")),
            Map.entry("PEDIDOS", List.of("/pedidos")),
            Map.entry("HORARIOS", List.of("/horarios-atencion")),
            Map.entry("REPORTES", List.of("/reportes")),
            Map.entry("CONFIG_PAGOS", List.of("/config-pagos"))
    );

    private final UsuarioRepository usuarioRepository;
    private final ModuloRepository moduloRepository;

    public PermissionService(UsuarioRepository usuarioRepository, ModuloRepository moduloRepository) {
        this.usuarioRepository = usuarioRepository;
        this.moduloRepository = moduloRepository;
    }

    public static boolean esPerfilGerente(String nombrePerfil) {
        if (nombrePerfil == null) {
            return false;
        }
        String nombre = nombrePerfil.trim().toLowerCase();
        return nombre.contains("gerente") || nombre.contains("manager");
    }

    public static boolean esPerfilAdministrador(String nombrePerfil) {
        if (nombrePerfil == null) {
            return false;
        }
        String nombre = nombrePerfil.trim().toLowerCase();
        return nombre.equals("administrador") || nombre.equals("admin");
    }

    public static boolean esUsuarioAdminProtegido(Usuario usuario) {
        return usuario != null
                && usuario.getUsuario() != null
                && "admin".equalsIgnoreCase(usuario.getUsuario().trim());
    }

    @Transactional(readOnly = true)
    public Usuario getUsuarioActual() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            return null;
        }
        return usuarioRepository.findByUsuario(auth.getName()).orElse(null);
    }

    @Transactional(readOnly = true)
    public boolean esAdministrador() {
        Usuario usuario = getUsuarioActual();
        return usuario != null
                && usuario.getPerfil() != null
                && esPerfilAdministrador(usuario.getPerfil().getNombre());
    }

    @Transactional(readOnly = true)
    public boolean puedeAccederInventario() {
        if (esAdministrador()) {
            return true;
        }
        return getModulosPermitidos().stream()
                .anyMatch(m -> "INVENTARIO".equals(m.getCodigo()));
    }

    @Transactional(readOnly = true)
    public boolean puedeAccederReportes() {
        if (esAdministrador()) {
            return true;
        }
        Usuario usuario = getUsuarioActual();
        if (usuario == null || usuario.getPerfil() == null) {
            return false;
        }
        if (esPerfilGerente(usuario.getPerfil().getNombre())) {
            return getModulosPermitidos().stream()
                    .anyMatch(m -> "REPORTES".equals(m.getCodigo()));
        }
        return getModulosPermitidos().stream()
                .anyMatch(m -> "REPORTES".equals(m.getCodigo()));
    }

    @Transactional(readOnly = true)
    public List<Modulo> getModulosPermitidos() {
        Usuario usuario = getUsuarioActual();
        if (esAdministrador() || esUsuarioAdminProtegido(usuario)) {
            return moduloRepository.findByEstadoOrderByOrdenAsc(Modulo.Estado.ACTIVO);
        }

        if (usuario == null || usuario.getPerfil() == null || usuario.getPerfil().getModulos() == null) {
            return List.of();
        }

        return usuario.getPerfil().getModulos().stream()
                .filter(m -> m.getEstado() == Modulo.Estado.ACTIVO)
                .sorted(Comparator.comparing(m -> m.getOrden(), Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();
    }

    @Transactional(readOnly = true)
    public boolean puedeAcceder(String path) {
        if (path == null) {
            return false;
        }
        if (esRutaPublica(path)) {
            return true;
        }
        if (esAdministrador()) {
            return true;
        }
        String normalizada = normalizarRuta(path);
        return getModulosPermitidos().stream()
                .anyMatch(modulo -> coincideRuta(modulo.getCodigo(), normalizada));
    }

    public String resolverCodigoModulo(String path) {
        String normalizada = normalizarRuta(path);
        for (Map.Entry<String, List<String>> entry : RUTAS_POR_CODIGO.entrySet()) {
            for (String ruta : entry.getValue()) {
                if (normalizada.equals(ruta) || normalizada.startsWith(ruta + "/")) {
                    return entry.getKey();
                }
            }
        }
        return "";
    }

    @Transactional(readOnly = true)
    public String getRutaInicio() {
        List<Modulo> modulos = getModulosPermitidos();
        if (modulos.isEmpty()) {
            return "/login?sinPermisos";
        }
        return modulos.get(0).getRuta();
    }

    public boolean esRutaPublica(String path) {
        String normalizada = normalizarRuta(path);
        return normalizada.startsWith("/css/")
                || normalizada.startsWith("/js/")
                || normalizada.startsWith("/img/")
                || normalizada.startsWith("/uploads/")
                || normalizada.startsWith("/imagen/")
                || normalizada.startsWith("/archivos/")
                || normalizada.equals("/login")
                || normalizada.equals("/login/verificar")
                || normalizada.equals("/login/reenviar-codigo")
                || normalizada.equals("/recuperar-contrasena")
                || normalizada.startsWith("/recuperar-contrasena/")
                || normalizada.startsWith("/catalogo")
                || normalizada.equals("/error")
                || normalizada.equals("/logout")
                || normalizada.equals("/version")
                || normalizada.equals("/salud");
    }

    private boolean coincideRuta(String codigo, String path) {
        List<String> rutas = RUTAS_POR_CODIGO.get(codigo);
        if (rutas == null) {
            return false;
        }
        for (String ruta : rutas) {
            if (path.equals(ruta) || path.startsWith(ruta + "/")) {
                return true;
            }
        }
        return false;
    }

    private String normalizarRuta(String path) {
        if (path == null || path.isBlank()) {
            return "/";
        }
        int queryIndex = path.indexOf('?');
        if (queryIndex >= 0) {
            path = path.substring(0, queryIndex);
        }
        return path.endsWith("/") && path.length() > 1 ? path.substring(0, path.length() - 1) : path;
    }

    public Set<String> codigosMenu() {
        return RUTAS_POR_CODIGO.keySet();
    }
}
