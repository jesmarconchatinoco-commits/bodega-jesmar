package com.test.desarrollo_web.config;

import com.test.desarrollo_web.Models.Modulo;
import com.test.desarrollo_web.Models.Perfil;
import com.test.desarrollo_web.Repository.ModuloRepository;
import com.test.desarrollo_web.Repository.PerfilRepository;
import com.test.desarrollo_web.service.PermissionService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Configuration
public class ModuloInitializer {

    @Bean
    @Order(100)
    public CommandLineRunner sincronizarModulos(ModuloRepository moduloRepository,
                                                PerfilRepository perfilRepository) {
        return args -> {
            List<ModuloDef> definiciones = List.of(
                    new ModuloDef("DASHBOARD", "Dashboard", "/dashboard", "fas fa-home", 1),
                    new ModuloDef("USUARIOS", "Gestión de Usuarios", "/usuarios", "fas fa-users", 2),
                    new ModuloDef("PERFILES", "Gestión de Perfiles", "/perfiles", "fas fa-id-badge", 3),
                    new ModuloDef("CATEGORIAS", "Gestión de Categorías", "/categorias", "fas fa-tags", 4),
                    new ModuloDef("PRODUCTOS", "Gestión Productos", "/productos", "fas fa-box", 5),
                    new ModuloDef("INVENTARIO", "Gestión de Inventario", "/inventario", "fas fa-warehouse", 6),
                    new ModuloDef("SLIDER", "Gestión Slider y Logo", "/imagen-slider", "fas fa-images", 7),
                    new ModuloDef("VENTAS", "Gestión Ventas", "/ventas", "fas fa-shopping-cart", 8),
                    new ModuloDef("CLIENTES", "Gestión Clientes", "/clientes", "fas fa-user-tie", 9),
                    new ModuloDef("PEDIDOS", "Gestión Pedidos", "/pedidos", "fas fa-shopping-bag", 10),
                    new ModuloDef("HORARIOS", "Horarios de Atención", "/horarios-atencion", "fas fa-clock", 11),
                    new ModuloDef("REPORTES", "Gestión de Reportes", "/reportes", "fas fa-chart-bar", 12),
                    new ModuloDef("CONFIG_PAGOS", "Configuración de Pagos", "/config-pagos", "fas fa-credit-card", 13)
            );

            moduloRepository.findAll().stream()
                    .filter(m -> m.getCodigo() == null || m.getCodigo().isBlank())
                    .forEach(m -> {
                        m.setEstado(Modulo.Estado.INACTIVO);
                        moduloRepository.save(m);
                    });

            for (ModuloDef def : definiciones) {
                Modulo modulo = moduloRepository.findByCodigo(def.codigo()).orElseGet(Modulo::new);
                modulo.setCodigo(def.codigo());
                modulo.setNombre(def.nombre());
                modulo.setRuta(def.ruta());
                modulo.setIcono(def.icono());
                modulo.setOrden(def.orden());
                modulo.setEstado(Modulo.Estado.ACTIVO);
                if (modulo.getDescripcion() == null || modulo.getDescripcion().isBlank()) {
                    modulo.setDescripcion("Acceso a " + def.nombre());
                }
                moduloRepository.save(modulo);
            }

            Set<Modulo> todos = new HashSet<>(moduloRepository.findByEstadoOrderByOrdenAsc(Modulo.Estado.ACTIVO));
            Modulo moduloPedidos = moduloRepository.findByCodigo("PEDIDOS").orElse(null);
            Modulo moduloVentas = moduloRepository.findByCodigo("VENTAS").orElse(null);
            Modulo moduloInventario = moduloRepository.findByCodigo("INVENTARIO").orElse(null);
            Modulo moduloReportes = moduloRepository.findByCodigo("REPORTES").orElse(null);

            for (Perfil perfil : perfilRepository.findAllWithModulos()) {
                if (PermissionService.esPerfilAdministrador(perfil.getNombre())) {
                    perfil.setModulos(todos);
                    perfilRepository.save(perfil);
                    continue;
                }

                if (moduloReportes != null && (PermissionService.esPerfilGerente(perfil.getNombre())
                        || PermissionService.esPerfilAdministrador(perfil.getNombre()))) {
                    Set<Modulo> actualizados = new HashSet<>(perfil.getModulos());
                    if (!actualizados.contains(moduloReportes)) {
                        actualizados.add(moduloReportes);
                        perfil.setModulos(actualizados);
                        perfilRepository.save(perfil);
                    }
                }

                if (moduloPedidos == null || moduloVentas == null) {
                    continue;
                }

                boolean tieneVentas = perfil.getModulos().stream()
                        .anyMatch(m -> "VENTAS".equals(m.getCodigo()));
                boolean tienePedidos = perfil.getModulos().stream()
                        .anyMatch(m -> "PEDIDOS".equals(m.getCodigo()));
                boolean tieneInventario = perfil.getModulos().stream()
                        .anyMatch(m -> "INVENTARIO".equals(m.getCodigo()));

                Set<Modulo> actualizados = null;

                if (tieneVentas && !tienePedidos) {
                    actualizados = new HashSet<>(perfil.getModulos());
                    actualizados.add(moduloPedidos);
                }
                if (tieneVentas && moduloInventario != null && !tieneInventario) {
                    if (actualizados == null) {
                        actualizados = new HashSet<>(perfil.getModulos());
                    }
                    actualizados.add(moduloInventario);
                }
                if (actualizados != null) {
                    perfil.setModulos(actualizados);
                    perfilRepository.save(perfil);
                }
            }
        };
    }

    private record ModuloDef(String codigo, String nombre, String ruta, String icono, int orden) {
    }
}
