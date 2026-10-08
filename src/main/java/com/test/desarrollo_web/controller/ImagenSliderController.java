package com.test.desarrollo_web.controller;

import com.test.desarrollo_web.Models.Imagen;
import com.test.desarrollo_web.Models.ImagenSlider;
import com.test.desarrollo_web.Repository.ImagenSliderRepository;
import com.test.desarrollo_web.config.ImageStorageCategory;
import com.test.desarrollo_web.service.FileStorageService;
import com.test.desarrollo_web.service.LogoService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/imagen-slider")
public class ImagenSliderController {

    private final ImagenSliderRepository imagenSliderRepository;
    private final FileStorageService fileStorageService;
    private final LogoService logoService;

    public ImagenSliderController(ImagenSliderRepository imagenSliderRepository,
                                  FileStorageService fileStorageService,
                                  LogoService logoService) {
        this.imagenSliderRepository = imagenSliderRepository;
        this.fileStorageService = fileStorageService;
        this.logoService = logoService;
    }

    @GetMapping
    public String listar(Model model) {
        model.addAttribute("logos", logoService.listarLogos());
        model.addAttribute("logoActivo", logoService.findLogoActivo().orElse(null));
        model.addAttribute("sliders", imagenSliderRepository
                .findSlidersWithImagenOrderByIdDesc(ImagenSlider.TITULO_LOGO));
        return "imagen-slider/list";
    }

    @PostMapping("/logo")
    public String agregarLogo(@RequestParam("imagenFile") MultipartFile imagenFile,
                              RedirectAttributes redirectAttributes) {
        try {
            logoService.agregarLogo(imagenFile);
            redirectAttributes.addFlashAttribute("success", "Logo agregado a la galería correctamente");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/imagen-slider";
    }

    @PostMapping("/logo/seleccionar/{id}")
    public String seleccionarLogo(@PathVariable Integer id, RedirectAttributes redirectAttributes) {
        try {
            logoService.activarLogo(id);
            redirectAttributes.addFlashAttribute("success", "Logo activo actualizado en login, menú y catálogo");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/imagen-slider";
    }

    @GetMapping("/logo/eliminar/{id}")
    public String eliminarLogo(@PathVariable Integer id, RedirectAttributes redirectAttributes) {
        try {
            logoService.eliminarLogo(id);
            redirectAttributes.addFlashAttribute("success", "Logo eliminado correctamente");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/imagen-slider";
    }

    @PostMapping("/slider")
    public String guardarSlider(@RequestParam("imagenFiles") MultipartFile[] imagenFiles,
                                RedirectAttributes redirectAttributes) {
        try {
            if (imagenFiles == null || imagenFiles.length == 0) {
                redirectAttributes.addFlashAttribute("error", "Seleccione al menos una imagen para el slider.");
                return "redirect:/imagen-slider";
            }
            int agregadas = 0;
            for (MultipartFile imagenFile : imagenFiles) {
                if (imagenFile == null || imagenFile.isEmpty()) {
                    continue;
                }
                Imagen imagen = fileStorageService.saveFile(imagenFile, ImageStorageCategory.SLIDERS);
                ImagenSlider slider = new ImagenSlider();
                slider.setTitulo(ImagenSlider.TITULO_SLIDER);
                slider.setDescripcion("");
                slider.setImagen(imagen);
                slider.setEstado(ImagenSlider.Estado.ACTIVO);
                imagenSliderRepository.save(slider);
                agregadas++;
            }
            if (agregadas == 0) {
                redirectAttributes.addFlashAttribute("error", "Seleccione al menos una imagen para el slider.");
                return "redirect:/imagen-slider";
            }
            redirectAttributes.addFlashAttribute("success",
                    agregadas == 1 ? "Imagen del slider agregada correctamente"
                            : agregadas + " imágenes del slider agregadas correctamente");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/imagen-slider";
    }

    @GetMapping("/eliminar/{id}")
    public String eliminar(@PathVariable Integer id, RedirectAttributes redirectAttributes) {
        try {
            ImagenSlider slider = imagenSliderRepository.findByIdWithImagen(id).orElseThrow();
            if (ImagenSlider.TITULO_LOGO.equalsIgnoreCase(slider.getTitulo())) {
                redirectAttributes.addFlashAttribute("error", "Use la galería de logos para eliminar logos.");
                return "redirect:/imagen-slider";
            }
            if (slider.getImagen() != null) {
                fileStorageService.deleteFile(slider.getImagen().getRuta());
            }
            imagenSliderRepository.deleteById(id);
            redirectAttributes.addFlashAttribute("success", "Imagen del slider eliminada correctamente");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error al eliminar: " + e.getMessage());
        }
        return "redirect:/imagen-slider";
    }
}
