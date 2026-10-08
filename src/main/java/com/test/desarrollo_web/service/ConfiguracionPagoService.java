package com.test.desarrollo_web.service;

import com.test.desarrollo_web.Models.ConfiguracionPago;
import com.test.desarrollo_web.Repository.ConfiguracionPagoRepository;
import com.test.desarrollo_web.config.CatalogoProperties;
import com.test.desarrollo_web.config.ImageStorageCategory;
import com.test.desarrollo_web.util.ImagenRutas;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class ConfiguracionPagoService {

    private final ConfiguracionPagoRepository configuracionPagoRepository;
    private final FileStorageService fileStorageService;
    private final CatalogoProperties catalogoProperties;

    public ConfiguracionPagoService(ConfiguracionPagoRepository configuracionPagoRepository,
                                    FileStorageService fileStorageService,
                                    CatalogoProperties catalogoProperties) {
        this.configuracionPagoRepository = configuracionPagoRepository;
        this.fileStorageService = fileStorageService;
        this.catalogoProperties = catalogoProperties;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> listarAdmin() {
        return configuracionPagoRepository.findAll().stream()
                .sorted((a, b) -> a.getCanal().name().compareTo(b.getCanal().name()))
                .map(this::mapearAdmin)
                .toList();
    }

    @Transactional(readOnly = true)
    public Map<String, Object> obtenerConfigActiva(String canal) {
        ConfiguracionPago.Canal c = parseCanal(canal);
        ConfiguracionPago config = obtenerActiva(c);
        return mapearPublico(config);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> obtenerConfigCheckout() {
        Map<String, Object> res = new LinkedHashMap<>();
        res.put("costoEnvio", catalogoProperties.getCostoEnvio());
        res.put("yape", mapearPublico(obtenerActiva(ConfiguracionPago.Canal.YAPE)));
        res.put("plin", mapearPublico(obtenerActiva(ConfiguracionPago.Canal.PLIN)));
        return res;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> obtenerConfigPos() {
        Map<String, Object> res = new LinkedHashMap<>();
        Map<String, Object> yape = mapearPublico(obtenerActiva(ConfiguracionPago.Canal.YAPE));
        Map<String, Object> plin = mapearPublico(obtenerActiva(ConfiguracionPago.Canal.PLIN));
        res.put("yapeCelular", yape.get("celular"));
        res.put("plinCelular", plin.get("celular"));
        res.put("yape", yape);
        res.put("plin", plin);
        return res;
    }

    @Transactional
    public Map<String, Object> guardar(Integer id,
                                         String canal,
                                         String celular,
                                         String nombreTitular,
                                         String textoQr,
                                         String estado,
                                         MultipartFile imagenQr) {
        if (celular == null || celular.isBlank()) {
            throw new RuntimeException("El número de celular es obligatorio.");
        }
        if (nombreTitular == null || nombreTitular.isBlank()) {
            throw new RuntimeException("El nombre del titular es obligatorio.");
        }

        ConfiguracionPago config;
        if (id != null) {
            config = configuracionPagoRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Configuración no encontrada."));
        } else {
            config = new ConfiguracionPago();
            config.setCanal(parseCanal(canal));
        }

        config.setCelular(celular.trim());
        config.setNombreTitular(nombreTitular.trim());
        config.setTextoQr(textoQr != null ? textoQr.trim() : null);
        config.setEstado("INACTIVO".equalsIgnoreCase(estado)
                ? ConfiguracionPago.Estado.INACTIVO
                : ConfiguracionPago.Estado.ACTIVO);

        if (imagenQr != null && !imagenQr.isEmpty()) {
            var imagen = fileStorageService.saveFile(imagenQr, ImageStorageCategory.CONFIG_PAGOS);
            config.setImagenQr(imagen.getRuta());
        }

        configuracionPagoRepository.save(config);
        Map<String, Object> res = new LinkedHashMap<>();
        res.put("success", true);
        res.put("config", mapearAdmin(config));
        res.put("message", "Configuración guardada correctamente.");
        return res;
    }

    @Transactional(readOnly = true)
    public ConfiguracionPago obtenerActiva(ConfiguracionPago.Canal canal) {
        return configuracionPagoRepository.findByCanal(canal)
                .filter(c -> c.getEstado() == ConfiguracionPago.Estado.ACTIVO)
                .orElseGet(() -> configuracionPagoRepository.findByCanal(canal).orElse(null));
    }

    private Map<String, Object> mapearAdmin(ConfiguracionPago config) {
        Map<String, Object> m = new LinkedHashMap<>();
        if (config == null) {
            return m;
        }
        m.put("id", config.getId());
        m.put("canal", config.getCanal().name());
        m.put("celular", config.getCelular());
        m.put("nombreTitular", config.getNombreTitular());
        m.put("textoQr", config.getTextoQr());
        m.put("imagenQr", ImagenRutas.toPublicUrl(config.getImagenQr()));
        m.put("imagenQrRuta", config.getImagenQr());
        m.put("estado", config.getEstado().name());
        return m;
    }

    private Map<String, Object> mapearPublico(ConfiguracionPago config) {
        Map<String, Object> m = new LinkedHashMap<>();
        if (config == null) {
            m.put("activo", false);
            m.put("celular", "");
            m.put("nombreTitular", "");
            m.put("textoQr", "");
            m.put("imagenQr", "");
            return m;
        }
        m.put("activo", config.getEstado() == ConfiguracionPago.Estado.ACTIVO);
        m.put("celular", config.getCelular() != null ? config.getCelular() : "");
        m.put("nombreTitular", config.getNombreTitular() != null ? config.getNombreTitular() : "");
        String textoQr = config.getTextoQr();
        if (textoQr == null || textoQr.isBlank()) {
            String etiqueta = config.getCanal() == ConfiguracionPago.Canal.YAPE ? "Yape" : "Plin";
            textoQr = etiqueta + " al " + m.get("celular");
        }
        m.put("textoQr", textoQr);
        m.put("imagenQr", ImagenRutas.toPublicUrl(config.getImagenQr()));
        return m;
    }

    private ConfiguracionPago.Canal parseCanal(String canal) {
        if (canal == null || canal.isBlank()) {
            throw new RuntimeException("Canal de pago no especificado.");
        }
        try {
            return ConfiguracionPago.Canal.valueOf(canal.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Canal de pago inválido.");
        }
    }
}
