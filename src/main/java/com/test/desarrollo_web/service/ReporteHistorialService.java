package com.test.desarrollo_web.service;

import com.test.desarrollo_web.Models.ReporteHistorial;
import com.test.desarrollo_web.Models.Usuario;
import com.test.desarrollo_web.Repository.ReporteHistorialRepository;
import com.test.desarrollo_web.dto.ReporteFiltroDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;

@Service
public class ReporteHistorialService {

    private static final Logger log = LoggerFactory.getLogger(ReporteHistorialService.class);

    private final ReporteHistorialRepository historialRepository;
    private final PermissionService permissionService;
    private final ObjectMapper objectMapper;
    private final TransactionTemplate escrituraTemplate;

    public ReporteHistorialService(ReporteHistorialRepository historialRepository,
                                   PermissionService permissionService,
                                   ObjectMapper objectMapper,
                                   PlatformTransactionManager transactionManager) {
        this.historialRepository = historialRepository;
        this.permissionService = permissionService;
        this.objectMapper = objectMapper;
        this.escrituraTemplate = new TransactionTemplate(transactionManager);
        this.escrituraTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        this.escrituraTemplate.setReadOnly(false);
    }

    /**
     * Registra auditoría en una transacción de escritura independiente.
     * Si falla, no interrumpe la consulta del reporte.
     */
    public void registrarSeguro(String tipo, String accion, String formato, ReporteFiltroDto filtro) {
        try {
            escrituraTemplate.executeWithoutResult(status -> persistir(tipo, accion, formato, filtro));
        } catch (Exception e) {
            log.warn("No se pudo registrar historial de reporte [{} / {}]: {}",
                    tipo, accion, e.getMessage());
        }
    }

    private void persistir(String tipo, String accion, String formato, ReporteFiltroDto filtro) {
        Usuario usuario = permissionService.getUsuarioActual();
        ReporteHistorial h = new ReporteHistorial();
        h.setUsuarioId(usuario != null ? usuario.getId() : null);
        h.setUsuarioNombre(usuario != null ? usuario.getNombre() : "Sistema");
        h.setTipoReporte(tipo);
        h.setAccion(accion);
        h.setFormato(formato);
        try {
            h.setFiltrosJson(objectMapper.writeValueAsString(filtro));
        } catch (Exception e) {
            h.setFiltrosJson("{}");
        }
        h.setFecha(LocalDateTime.now());
        historialRepository.save(h);
    }
}
