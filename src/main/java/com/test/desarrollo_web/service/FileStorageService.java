package com.test.desarrollo_web.service;

import com.test.desarrollo_web.config.ImageStorageCategory;
import com.test.desarrollo_web.config.StoragePathResolver;
import com.test.desarrollo_web.Models.Imagen;
import com.test.desarrollo_web.Repository.ImagenRepository;
import com.test.desarrollo_web.util.ImagenRutas;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;

@Service
public class FileStorageService {

    private static final Logger log = LoggerFactory.getLogger(FileStorageService.class);
    private static final Set<String> MIME_PERMITIDOS = Set.of("image/jpeg", "image/png");
    private static final Set<String> EXTENSIONES_PERMITIDAS = Set.of(".jpg", ".jpeg", ".png");
    private static final long MAX_SIZE_BYTES = 2 * 1024 * 1024; // 2 MB

    private final Path uploadDir;
    private final Path legacyUploadDir;
    private final ImagenRepository imagenRepository;

    public FileStorageService(@Value("${app.upload.dir:src/main/resources/imagen}") String uploadDir,
                              StoragePathResolver storagePathResolver,
                              ImagenRepository imagenRepository) {
        this.uploadDir = storagePathResolver.resolve(uploadDir);
        this.legacyUploadDir = storagePathResolver.resolve("uploads");
        this.imagenRepository = imagenRepository;
        inicializarCarpetas();
    }

    @PostConstruct
    void logRutaAlmacenamiento() {
        log.info("Imágenes del sistema se guardan en: {}", uploadDir);
        for (ImageStorageCategory categoria : ImageStorageCategory.values()) {
            log.info("  └─ {}/", categoria.getFolder());
        }
        migrarArchivosAntiguos();
    }

    private void migrarArchivosAntiguos() {
        if (!Files.exists(legacyUploadDir)) {
            return;
        }
        Map<String, String> carpetas = Map.of(
                "productos", ImageStorageCategory.PRESENTACIONES.getFolder(),
                "producto", ImageStorageCategory.PRESENTACIONES.getFolder(),
                "categorias", ImageStorageCategory.CATEGORIAS.getFolder(),
                "clientes", ImageStorageCategory.CLIENTES.getFolder(),
                "perfiles", ImageStorageCategory.PERFILES.getFolder(),
                "sliders", ImageStorageCategory.SLIDERS.getFolder(),
                "logos", ImageStorageCategory.LOGO.getFolder(),
                "login", ImageStorageCategory.LOGO.getFolder(),
                "pedidos", ImageStorageCategory.PEDIDOS.getFolder()
        );
        for (Map.Entry<String, String> entry : carpetas.entrySet()) {
            Path origen = legacyUploadDir.resolve(entry.getKey());
            if (!Files.isDirectory(origen)) {
                continue;
            }
            Path destino = uploadDir.resolve(entry.getValue());
            try {
                Files.createDirectories(destino);
                try (Stream<Path> archivos = Files.list(origen)) {
                    archivos.filter(Files::isRegularFile).forEach(archivo -> {
                        Path target = destino.resolve(archivo.getFileName().toString());
                        if (!Files.exists(target)) {
                            try {
                                Files.copy(archivo, target, StandardCopyOption.REPLACE_EXISTING);
                                log.info("Imagen migrada a subcarpeta: {}", target);
                            } catch (IOException e) {
                                log.warn("No se pudo migrar {}: {}", archivo, e.getMessage());
                            }
                        }
                    });
                }
            } catch (IOException e) {
                log.warn("Error migrando carpeta {}: {}", origen, e.getMessage());
            }
        }
    }

    private void inicializarCarpetas() {
        try {
            Files.createDirectories(this.uploadDir);
            for (ImageStorageCategory categoria : ImageStorageCategory.values()) {
                Files.createDirectories(this.uploadDir.resolve(categoria.getFolder()));
            }
        } catch (IOException e) {
            throw new RuntimeException("No se pudo crear la carpeta de imágenes: " + this.uploadDir, e);
        }
    }

    public Imagen saveFile(MultipartFile file, ImageStorageCategory categoria) {
        if (file == null || file.isEmpty()) {
            throw new RuntimeException("No se recibió ninguna imagen.");
        }

        validarSoloJpgPng(file);

        try {
            Path folderPath = this.uploadDir.resolve(categoria.getFolder()).normalize();
            Files.createDirectories(folderPath);

            String extension = obtenerExtensionPermitida(file);
            String filename = UUID.randomUUID().toString() + extension;
            Path targetPath = folderPath.resolve(filename);

            Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);

            if (!Files.exists(targetPath)) {
                throw new RuntimeException("No se pudo guardar el archivo en: " + targetPath);
            }

            log.info("Imagen guardada en: {}", targetPath);

            String rutaRelativa = ImagenRutas.normalizarRuta(categoria.getFolder() + "/" + filename);
            String nombreOriginal = file.getOriginalFilename() != null ? file.getOriginalFilename() : filename;

            Imagen imagen = new Imagen();
            imagen.setNombre(nombreOriginal);
            imagen.setRuta(rutaRelativa);
            imagen.setTipo(extension.equals(".png") ? "image/png" : "image/jpeg");
            imagen.setTamano(file.getSize());
            imagen.setFechaSubida(LocalDateTime.now());
            imagen.setEstado(Imagen.Estado.ACTIVO);

            return imagenRepository.save(imagen);
        } catch (IOException e) {
            throw new RuntimeException("Error guardando el archivo: " + file.getOriginalFilename(), e);
        }
    }

    public List<Imagen> saveMultipleFiles(List<MultipartFile> files, ImageStorageCategory categoria) {
        List<Imagen> saved = new ArrayList<>();
        if (files != null) {
            for (MultipartFile file : files) {
                if (!file.isEmpty()) {
                    saved.add(saveFile(file, categoria));
                }
            }
        }
        return saved;
    }

    public void validarSoloJpgPng(MultipartFile file) {
        String nombre = file.getOriginalFilename() != null ? file.getOriginalFilename().toLowerCase(Locale.ROOT) : "";
        String contentType = file.getContentType() != null ? file.getContentType().toLowerCase(Locale.ROOT) : "";

        if (nombre.endsWith(".pdf") || nombre.endsWith(".doc") || nombre.endsWith(".docx")
                || nombre.endsWith(".xls") || nombre.endsWith(".xlsx") || nombre.endsWith(".txt")) {
            throw new RuntimeException("No se permiten PDF, Word u otros documentos. Solo fotos JPG o PNG.");
        }

        String extension = extraerExtension(nombre);
        if (!EXTENSIONES_PERMITIDAS.contains(extension)) {
            throw new RuntimeException("Formato no permitido. Solo fotos JPG o PNG.");
        }

        if (!contentType.isEmpty() && !MIME_PERMITIDOS.contains(contentType)) {
            throw new RuntimeException("Tipo de archivo no permitido. Use solo JPG o PNG.");
        }

        if (file.getSize() > MAX_SIZE_BYTES) {
            throw new RuntimeException("La foto supera el tamaño máximo de 2 MB.");
        }
    }

    private String obtenerExtensionPermitida(MultipartFile file) {
        String nombre = file.getOriginalFilename() != null ? file.getOriginalFilename().toLowerCase(Locale.ROOT) : "";
        String extension = extraerExtension(nombre);

        if (".jpeg".equals(extension) || ".jpg".equals(extension)) {
            return ".jpg";
        }
        if (".png".equals(extension)) {
            return ".png";
        }
        throw new RuntimeException("Formato no permitido. Solo JPG y PNG.");
    }

    private String extraerExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf(".")).toLowerCase(Locale.ROOT);
    }

    public Resource getFileAsResource(String filepath) throws MalformedURLException {
        Path filePath = resolverRutaArchivo(filepath);
        if (!Files.exists(filePath)) {
            throw new RuntimeException("Archivo no encontrado: " + filepath);
        }
        return new UrlResource(filePath.toUri());
    }

    public Path getUploadDir() {
        return this.uploadDir;
    }

    public Path resolveStoredFile(String filepath) {
        if (filepath == null || filepath.isBlank()) {
            return null;
        }
        return resolverRutaArchivo(filepath);
    }

    public String deleteFile(String filepath) {
        try {
            Path filePath = resolverRutaArchivo(filepath);
            if (Files.exists(filePath)) {
                Files.delete(filePath);
                log.info("Imagen eliminada: {}", filePath);
                return "Archivo eliminado correctamente";
            }
            return "El archivo no existe";
        } catch (IOException e) {
            throw new RuntimeException("Error eliminando el archivo: " + e.getMessage());
        }
    }

    private Path resolverRutaArchivo(String filepath) {
        if (filepath == null || filepath.isBlank()) {
            throw new RuntimeException("Ruta de archivo vacía");
        }

        Path directa = this.uploadDir.resolve(filepath).normalize();
        if (Files.exists(directa)) {
            return directa;
        }

        String rutaNormalizada = ImagenRutas.normalizarRuta(filepath);
        if (!rutaNormalizada.equals(filepath)) {
            Path alternativa = this.uploadDir.resolve(rutaNormalizada).normalize();
            if (Files.exists(alternativa)) {
                return alternativa;
            }
        }

        Path legacy = legacyUploadDir.resolve(filepath).normalize();
        if (Files.exists(legacy)) {
            return legacy;
        }
        Path legacyAlt = legacyUploadDir.resolve(rutaNormalizada).normalize();
        if (Files.exists(legacyAlt)) {
            return legacyAlt;
        }

        return directa;
    }
}
