package com.test.desarrollo_web.controller;

import com.test.desarrollo_web.Models.Modulo;
import com.test.desarrollo_web.Models.Perfil;
import com.test.desarrollo_web.Repository.ModuloRepository;
import com.test.desarrollo_web.Repository.PerfilRepository;
import com.test.desarrollo_web.service.PermissionService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/perfiles")
public class PerfilController {

    private final PerfilRepository perfilRepository;
    private final ModuloRepository moduloRepository;
    private final PermissionService permissionService;

    public PerfilController(PerfilRepository perfilRepository,
            ModuloRepository moduloRepository,
            PermissionService permissionService) {
        this.perfilRepository = perfilRepository;
        this.moduloRepository = moduloRepository;
        this.permissionService = permissionService;
    }

    @GetMapping
    public String listar(Model model) {
        List<Perfil> perfiles = perfilRepository.findAll();
        List<Modulo> modulos = moduloRepository.findByEstadoOrderByOrdenAsc(Modulo.Estado.ACTIVO);
        if (modulos == null) {
            modulos = List.of();
        }

        model.addAttribute("perfiles", perfiles);
        model.addAttribute("modulos", modulos);
        model.addAttribute("nuevoPerfil", new Perfil());

        var usuarioActual = permissionService.getUsuarioActual();
        Integer perfilActualId = usuarioActual != null && usuarioActual.getPerfil() != null
                ? usuarioActual.getPerfil().getId()
                : null;
        model.addAttribute("perfilActualId", perfilActualId);

        // JSON seguro para el JS del modal de permisos
        String perfilesJson = buildPerfilesJson(perfiles);
        model.addAttribute("perfilesJson", perfilesJson);

        return "perfiles/list";
    }

    @PostMapping("/guardar")
    public String guardar(@ModelAttribute Perfil perfil,
            @RequestParam(value = "moduloIds", required = false) List<Integer> moduloIds,
            RedirectAttributes ra) {
        try {
            if (moduloIds != null) {
                perfil.setModulos(new java.util.HashSet<>(moduloRepository.findAllById(moduloIds)));
            } else if (perfil.getId() != null) {
                perfilRepository.findById(perfil.getId()).ifPresent(existente ->
                        perfil.setModulos(existente.getModulos()));
            } else {
                perfil.setModulos(new java.util.HashSet<>());
            }
            perfilRepository.save(perfil);
            ra.addFlashAttribute("success", "Perfil guardado correctamente.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Error al guardar: " + e.getMessage());
        }
        return "redirect:/perfiles";
    }

    @PostMapping("/permisos/{id}")
    public String guardarPermisos(@PathVariable Integer id,
            @RequestParam(value = "moduloIds", required = false) List<Integer> moduloIds,
            RedirectAttributes ra) {
        try {
            Perfil perfil = perfilRepository.findById(id).orElseThrow();
            if (PermissionService.esPerfilAdministrador(perfil.getNombre())) {
                perfil.setModulos(new java.util.HashSet<>(
                        moduloRepository.findByEstadoOrderByOrdenAsc(Modulo.Estado.ACTIVO)));
            } else {
                perfil.setModulos(moduloIds != null
                        ? new java.util.HashSet<>(moduloRepository.findAllById(moduloIds))
                        : new java.util.HashSet<>());
            }
            perfilRepository.save(perfil);
            ra.addFlashAttribute("success", "Permisos guardados correctamente.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Error al guardar permisos: " + e.getMessage());
        }
        return "redirect:/perfiles";
    }

    @GetMapping("/eliminar/{id}")
    public String eliminar(@PathVariable Integer id, RedirectAttributes ra) {
        try {
            Perfil perfil = perfilRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("El perfil no existe."));

            if (PermissionService.esPerfilAdministrador(perfil.getNombre())) {
                ra.addFlashAttribute("error",
                        "El perfil Administrador no puede eliminarse porque es un perfil protegido del sistema.");
                return "redirect:/perfiles";
            }

            var usuarioActual = permissionService.getUsuarioActual();
            if (usuarioActual != null && usuarioActual.getPerfil() != null
                    && perfil.getId().equals(usuarioActual.getPerfil().getId())) {
                ra.addFlashAttribute("error",
                        "No puede eliminar el perfil con el que tiene la sesión iniciada.");
                return "redirect:/perfiles";
            }

            perfilRepository.delete(perfil);
            ra.addFlashAttribute("success", "Perfil eliminado correctamente.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Error al eliminar: " + e.getMessage());
        }
        return "redirect:/perfiles";
    }

    // ── Construye JSON seguro sin depender de Jackson sobre entidades Lazy ──
    private String buildPerfilesJson(List<Perfil> perfiles) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < perfiles.size(); i++) {
            Perfil p = perfiles.get(i);
            sb.append("{");
            sb.append("\"id\":").append(p.getId()).append(",");
            sb.append("\"nombre\":\"").append(escapar(p.getNombre())).append("\",");
            sb.append("\"descripcion\":\"").append(escapar(p.getDescripcion())).append("\",");
            sb.append("\"estado\":\"").append(p.getEstado() != null ? p.getEstado().name() : "").append("\",");
            sb.append("\"modulos\":[");
            if (p.getModulos() != null && !p.getModulos().isEmpty()) {
                List<Modulo> mods = p.getModulos().stream().collect(Collectors.toList());
                for (int j = 0; j < mods.size(); j++) {
                    Modulo m = mods.get(j);
                    sb.append("{\"id\":").append(m.getId())
                            .append(",\"nombre\":\"").append(escapar(m.getNombre())).append("\"}");
                    if (j < mods.size() - 1)
                        sb.append(",");
                }
            }
            sb.append("]}");
            if (i < perfiles.size() - 1)
                sb.append(",");
        }
        sb.append("]");
        return sb.toString();
    }

    private String escapar(String s) {
        if (s == null)
            return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r");
    }
}