package com.test.desarrollo_web.controller;

import com.test.desarrollo_web.Models.Categoria;
import com.test.desarrollo_web.Repository.CategoriaRepository;
import com.test.desarrollo_web.service.CategoriaService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/categorias")
public class CategoriaController {

    private final CategoriaRepository categoriaRepository;
    private final CategoriaService categoriaService;

    public CategoriaController(CategoriaRepository categoriaRepository,
                               CategoriaService categoriaService) {
        this.categoriaRepository = categoriaRepository;
        this.categoriaService = categoriaService;
    }

    @GetMapping
    public String listar(Model model) {
        model.addAttribute("categorias", categoriaService.listarTodas());
        model.addAttribute("productosPorCategoria", categoriaService.contarProductosPorCategoria());
        return "categorias/list";
    }

    @PostMapping("/guardar")
    public String guardar(@ModelAttribute Categoria categoria,
            RedirectAttributes redirectAttributes) {
        try {
            if (categoria.getId() != null) {
                categoriaRepository.findById(categoria.getId()).ifPresent(existente ->
                        categoria.setImagen(existente.getImagen()));
            }
            categoriaRepository.save(categoria);
            redirectAttributes.addFlashAttribute("success", "Categoría guardada correctamente.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error al guardar: " + e.getMessage());
        }
        return "redirect:/categorias";
    }

    @GetMapping("/eliminar/{id}")
    public String eliminar(@PathVariable Integer id, RedirectAttributes redirectAttributes) {
        try {
            categoriaService.eliminarSiNoTieneProductos(id);
            redirectAttributes.addFlashAttribute("success", "Categoría eliminada correctamente.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/categorias";
    }
}
