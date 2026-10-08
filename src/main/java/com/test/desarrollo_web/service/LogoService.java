package com.test.desarrollo_web.service;

import com.test.desarrollo_web.Models.Imagen;
import com.test.desarrollo_web.Models.ImagenSlider;
import com.test.desarrollo_web.Repository.ImagenSliderRepository;
import com.test.desarrollo_web.config.ImageStorageCategory;
import com.test.desarrollo_web.util.ImagenRutas;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

@Service
public class LogoService {

    private final ImagenSliderRepository imagenSliderRepository;
    private final FileStorageService fileStorageService;

    public LogoService(ImagenSliderRepository imagenSliderRepository,
                       FileStorageService fileStorageService) {
        this.imagenSliderRepository = imagenSliderRepository;
        this.fileStorageService = fileStorageService;
    }

    @Transactional(readOnly = true)
    public Optional<Path> getLogoFilePath() {
        return findLogoActivo()
                .flatMap(slider -> Optional.ofNullable(slider.getImagen()).filter(this::tieneRuta))
                .map(imagen -> fileStorageService.resolveStoredFile(imagen.getRuta()))
                .filter(Files::exists);
    }

    @Transactional(readOnly = true)
    public Optional<String> getLogoUrl() {
        return findLogoActivo()
                .flatMap(slider -> Optional.ofNullable(slider.getImagen()).filter(this::tieneRuta))
                .map(imagen -> ImagenRutas.toPublicUrl(imagen.getRuta()));
    }

    @Transactional(readOnly = true)
    public Optional<ImagenSlider> findLogoActivo() {
        Optional<ImagenSlider> activo = imagenSliderRepository
                .findActiveLogoWithImagen(ImagenSlider.TITULO_LOGO, ImagenSlider.Estado.ACTIVO);
        if (activo.isPresent()) {
            return activo;
        }
        List<ImagenSlider> logos = imagenSliderRepository
                .findLogosWithImagenOrderByIdDesc(ImagenSlider.TITULO_LOGO);
        return logos.isEmpty() ? Optional.empty() : Optional.of(logos.get(0));
    }

    @Transactional(readOnly = true)
    public List<ImagenSlider> listarLogos() {
        return imagenSliderRepository.findLogosWithImagenOrderByIdDesc(ImagenSlider.TITULO_LOGO);
    }

    @Transactional
    public ImagenSlider agregarLogo(MultipartFile imagenFile) {
        Imagen imagen = fileStorageService.saveFile(imagenFile, ImageStorageCategory.LOGO);

        ImagenSlider logo = new ImagenSlider();
        logo.setTitulo(ImagenSlider.TITULO_LOGO);
        logo.setDescripcion(imagen.getNombre() != null ? imagen.getNombre() : "Logo de la tienda");
        logo.setImagen(imagen);

        boolean existeActivo = imagenSliderRepository
                .findActiveLogoWithImagen(ImagenSlider.TITULO_LOGO, ImagenSlider.Estado.ACTIVO)
                .isPresent();
        logo.setEstado(existeActivo ? ImagenSlider.Estado.INACTIVO : ImagenSlider.Estado.ACTIVO);

        return imagenSliderRepository.save(logo);
    }

    @Transactional
    public void activarLogo(Integer id) {
        ImagenSlider seleccionado = imagenSliderRepository.findByIdWithImagen(id)
                .orElseThrow(() -> new RuntimeException("Logo no encontrado."));
        if (!ImagenSlider.TITULO_LOGO.equalsIgnoreCase(seleccionado.getTitulo())) {
            throw new RuntimeException("El registro seleccionado no es un logo.");
        }

        List<ImagenSlider> logos = imagenSliderRepository
                .findLogosWithImagenOrderByIdDesc(ImagenSlider.TITULO_LOGO);
        for (ImagenSlider logo : logos) {
            logo.setEstado(logo.getId().equals(id)
                    ? ImagenSlider.Estado.ACTIVO
                    : ImagenSlider.Estado.INACTIVO);
        }
        imagenSliderRepository.saveAll(logos);
    }

    @Transactional
    public void eliminarLogo(Integer id) {
        ImagenSlider logo = imagenSliderRepository.findByIdWithImagen(id)
                .orElseThrow(() -> new RuntimeException("Logo no encontrado."));
        if (!ImagenSlider.TITULO_LOGO.equalsIgnoreCase(logo.getTitulo())) {
            throw new RuntimeException("El registro seleccionado no es un logo.");
        }
        if (logo.getEstado() == ImagenSlider.Estado.ACTIVO) {
            throw new RuntimeException("No puede eliminar el logo activo. Seleccione otro logo primero.");
        }
        if (logo.getImagen() != null && logo.getImagen().getRuta() != null) {
            fileStorageService.deleteFile(logo.getImagen().getRuta());
        }
        imagenSliderRepository.delete(logo);
    }

    private boolean tieneRuta(Imagen imagen) {
        return imagen != null && imagen.getRuta() != null && !imagen.getRuta().isBlank();
    }
}
