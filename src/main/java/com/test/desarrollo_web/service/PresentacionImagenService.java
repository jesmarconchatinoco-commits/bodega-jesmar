package com.test.desarrollo_web.service;

import com.test.desarrollo_web.Models.Imagen;
import com.test.desarrollo_web.Models.PresentacionImagen;
import com.test.desarrollo_web.Models.ProductoPresentacion;
import com.test.desarrollo_web.Repository.ImagenRepository;
import com.test.desarrollo_web.Repository.PresentacionImagenRepository;
import com.test.desarrollo_web.Repository.ProductoPresentacionRepository;
import com.test.desarrollo_web.config.ImageStorageCategory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.Comparator;
import java.util.List;

@Service
public class PresentacionImagenService {

    private final ProductoPresentacionRepository presentacionRepository;
    private final PresentacionImagenRepository presentacionImagenRepository;
    private final ImagenRepository imagenRepository;
    private final FileStorageService fileStorageService;

    public PresentacionImagenService(ProductoPresentacionRepository presentacionRepository,
                                       PresentacionImagenRepository presentacionImagenRepository,
                                       ImagenRepository imagenRepository,
                                       FileStorageService fileStorageService) {
        this.presentacionRepository = presentacionRepository;
        this.presentacionImagenRepository = presentacionImagenRepository;
        this.imagenRepository = imagenRepository;
        this.fileStorageService = fileStorageService;
    }

    @Transactional
    public int agregarImagenes(Integer presentacionId, MultipartFile[] imagenFiles) {
        ProductoPresentacion presentacion = presentacionRepository.findById(presentacionId)
                .orElseThrow(() -> new RuntimeException("Presentación no encontrada."));

        List<PresentacionImagen> existentes = presentacionImagenRepository
                .findByPresentacion_IdOrderByOrdenAsc(presentacionId);
        int ordenBase = existentes.size();
        int agregadas = 0;
        Long primeraNuevaImagenId = null;

        if (imagenFiles != null) {
            for (MultipartFile file : imagenFiles) {
                if (file == null || file.isEmpty()) {
                    continue;
                }

                Imagen imagen = fileStorageService.saveFile(file, ImageStorageCategory.PRESENTACIONES);
                if (primeraNuevaImagenId == null) {
                    primeraNuevaImagenId = imagen.getId();
                }

                PresentacionImagen registro = new PresentacionImagen();
                registro.setPresentacion(presentacion);
                registro.setImagen(imagenRepository.getReferenceById(imagen.getId()));
                registro.setOrden(ordenBase + agregadas);
                presentacionImagenRepository.save(registro);
                agregadas++;
            }
        }

        if (agregadas > 0 && presentacion.getImagen() == null && primeraNuevaImagenId != null) {
            presentacion.setImagen(imagenRepository.getReferenceById(primeraNuevaImagenId));
            presentacionRepository.save(presentacion);
        }

        return agregadas;
    }

    @Transactional
    public void eliminarImagen(Long presentacionImagenId) {
        PresentacionImagen registro = presentacionImagenRepository.findById(presentacionImagenId)
                .orElseThrow(() -> new RuntimeException("Registro de imagen no encontrado."));

        Integer presentacionId = registro.getPresentacion().getId();
        ProductoPresentacion presentacion = presentacionRepository.findById(presentacionId)
                .orElseThrow(() -> new RuntimeException("Presentación no encontrada."));

        Imagen imagen = registro.getImagen();
        Long imagenId = imagen != null ? imagen.getId() : null;

        presentacionImagenRepository.delete(registro);

        if (imagen != null) {
            fileStorageService.deleteFile(imagen.getRuta());
            imagenRepository.delete(imagen);
        }

        if (presentacion.getImagen() != null && imagenId != null
                && presentacion.getImagen().getId().equals(imagenId)) {
            presentacionImagenRepository.findByPresentacion_IdOrderByOrdenAsc(presentacionId).stream()
                    .min(Comparator.comparing(
                            pi -> pi.getOrden(),
                            Comparator.nullsLast(Comparator.naturalOrder())))
                    .map(pi -> pi.getImagen())
                    .map(img -> img.getId())
                    .ifPresentOrElse(
                            id -> presentacion.setImagen(imagenRepository.getReferenceById(id)),
                            () -> presentacion.setImagen(null)
                    );
            presentacionRepository.save(presentacion);
        }
    }

    @Transactional(readOnly = true)
    public List<PresentacionImagen> listarPorPresentacion(Integer presentacionId) {
        return presentacionImagenRepository.findByPresentacion_IdOrderByOrdenAsc(presentacionId);
    }
}
