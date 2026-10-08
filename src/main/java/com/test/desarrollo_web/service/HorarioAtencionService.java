package com.test.desarrollo_web.service;

import com.test.desarrollo_web.Models.FechaAtencion;
import com.test.desarrollo_web.Models.HorarioAtencion;
import com.test.desarrollo_web.Models.PedidoCatalogo;
import com.test.desarrollo_web.Repository.FechaAtencionRepository;
import com.test.desarrollo_web.Repository.HorarioAtencionRepository;
import com.test.desarrollo_web.Repository.PedidoCatalogoRepository;
import com.test.desarrollo_web.dto.FechaAtencionRequest;
import com.test.desarrollo_web.dto.HorarioAtencionRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.*;

@Service
public class HorarioAtencionService {

    private static final DateTimeFormatter FMT_HORA = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter FMT_FECHA_ISO = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final Locale LOCALE_ES_PE = Locale.of("es", "PE");
    private static final List<PedidoCatalogo.Estado> ESTADOS_OCUPAN_CUPO = List.of(
            PedidoCatalogo.Estado.PENDIENTE,
            PedidoCatalogo.Estado.ATENDIDO
    );

    private final FechaAtencionRepository fechaAtencionRepository;
    private final HorarioAtencionRepository horarioAtencionRepository;
    private final PedidoCatalogoRepository pedidoCatalogoRepository;

    public HorarioAtencionService(FechaAtencionRepository fechaAtencionRepository,
                                    HorarioAtencionRepository horarioAtencionRepository,
                                    PedidoCatalogoRepository pedidoCatalogoRepository) {
        this.fechaAtencionRepository = fechaAtencionRepository;
        this.horarioAtencionRepository = horarioAtencionRepository;
        this.pedidoCatalogoRepository = pedidoCatalogoRepository;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> listarAdmin() {
        List<Map<String, Object>> resultado = new ArrayList<>();
        for (FechaAtencion fecha : fechaAtencionRepository.findAllWithHorarios()) {
            resultado.add(mapearFechaAdmin(fecha));
        }
        return resultado;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> listarDisponiblesPublico() {
        LocalDate hoy = LocalDate.now();
        LocalTime ahora = LocalTime.now();
        List<Map<String, Object>> fechas = new ArrayList<>();

        for (FechaAtencion fecha : fechaAtencionRepository.findDesdeWithHorarios(hoy)) {
            if (!fecha.isActivo()) {
                continue;
            }

            List<Map<String, Object>> horarios = new ArrayList<>();
            for (HorarioAtencion horario : fecha.getHorarios()) {
                if (!horario.isActivo()) {
                    continue;
                }
                if (fecha.getFecha().equals(hoy) && horario.getHoraFin().isBefore(ahora)) {
                    continue;
                }

                long reservados = contarReservas(horario.getId());
                int disponibles = Math.max(0, horario.getCupoMaximo() - (int) reservados);
                boolean completo = disponibles <= 0;

                Map<String, Object> mapa = new LinkedHashMap<>();
                mapa.put("id", horario.getId());
                mapa.put("horaInicio", horario.getHoraInicio().format(FMT_HORA));
                mapa.put("horaFin", horario.getHoraFin().format(FMT_HORA));
                mapa.put("label", formatearRangoHorario(horario));
                mapa.put("cupoMaximo", horario.getCupoMaximo());
                mapa.put("reservados", reservados);
                mapa.put("disponibles", disponibles);
                mapa.put("completo", completo);
                horarios.add(mapa);
            }

            if (horarios.isEmpty()) {
                continue;
            }

            Map<String, Object> fechaMap = new LinkedHashMap<>();
            fechaMap.put("id", fecha.getId());
            fechaMap.put("fecha", fecha.getFecha().format(FMT_FECHA_ISO));
            fechaMap.put("fechaLabel", formatearFechaLabel(fecha.getFecha()));
            fechaMap.put("horarios", horarios);
            fechas.add(fechaMap);
        }

        return Map.of("fechas", fechas);
    }

    @Transactional
    public Map<String, Object> crearFecha(FechaAtencionRequest request) {
        LocalDate fecha = parseFecha(request.getFecha());
        if (fecha.isBefore(LocalDate.now())) {
            throw new RuntimeException("No puede registrar fechas anteriores a hoy.");
        }
        if (fechaAtencionRepository.findByFecha(fecha).isPresent()) {
            throw new RuntimeException("Ya existe una fecha de atención para " + formatearFechaLabel(fecha) + ".");
        }
        if (request.getHorarios() == null || request.getHorarios().isEmpty()) {
            throw new RuntimeException("Agregue al menos un horario de atención.");
        }

        FechaAtencion entidad = new FechaAtencion();
        entidad.setFecha(fecha);
        entidad.setActivo(request.getActivo() == null || request.getActivo());
        aplicarHorarios(entidad, request.getHorarios(), true);
        entidad = fechaAtencionRepository.save(entidad);

        Map<String, Object> res = new LinkedHashMap<>();
        res.put("success", true);
        res.put("message", "Fecha de atención registrada correctamente.");
        res.put("fecha", mapearFechaAdmin(entidad));
        return res;
    }

    @Transactional
    public Map<String, Object> actualizarFecha(Long id, FechaAtencionRequest request) {
        FechaAtencion entidad = fechaAtencionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Fecha de atención no encontrada."));

        if (request.getFecha() != null && !request.getFecha().isBlank()) {
            LocalDate nuevaFecha = parseFecha(request.getFecha());
            if (nuevaFecha.isBefore(LocalDate.now())) {
                throw new RuntimeException("No puede usar fechas anteriores a hoy.");
            }
            Optional<FechaAtencion> existente = fechaAtencionRepository.findByFecha(nuevaFecha);
            if (existente.isPresent() && !existente.get().getId().equals(id)) {
                throw new RuntimeException("Ya existe otra fecha de atención para esa fecha.");
            }
            entidad.setFecha(nuevaFecha);
        }

        if (request.getActivo() != null) {
            entidad.setActivo(request.getActivo());
        }

        if (request.getHorarios() != null) {
            if (request.getHorarios().isEmpty()) {
                throw new RuntimeException("Debe mantener al menos un horario de atención.");
            }
            aplicarHorarios(entidad, request.getHorarios(), false);
        }

        entidad = fechaAtencionRepository.save(entidad);

        Map<String, Object> res = new LinkedHashMap<>();
        res.put("success", true);
        res.put("message", "Fecha de atención actualizada correctamente.");
        res.put("fecha", mapearFechaAdmin(entidad));
        return res;
    }

    @Transactional
    public Map<String, Object> eliminarFecha(Long id) {
        FechaAtencion entidad = fechaAtencionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Fecha de atención no encontrada."));

        for (HorarioAtencion horario : entidad.getHorarios()) {
            if (contarReservas(horario.getId()) > 0) {
                throw new RuntimeException("No se puede eliminar: existen reservas confirmadas en esta fecha.");
            }
        }

        fechaAtencionRepository.delete(entidad);
        return Map.of("success", true, "message", "Fecha de atención eliminada correctamente.");
    }

    @Transactional
    public Map<String, Object> toggleFecha(Long id, boolean activo) {
        FechaAtencion entidad = fechaAtencionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Fecha de atención no encontrada."));
        entidad.setActivo(activo);
        fechaAtencionRepository.save(entidad);
        return Map.of(
                "success", true,
                "message", activo ? "Fecha habilitada." : "Fecha deshabilitada.",
                "fecha", mapearFechaAdmin(entidad)
        );
    }

    @Transactional
    public Map<String, Object> toggleHorario(Long id, boolean activo) {
        HorarioAtencion horario = horarioAtencionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Horario no encontrado."));
        horario.setActivo(activo);
        horarioAtencionRepository.save(horario);
        return Map.of(
                "success", true,
                "message", activo ? "Horario habilitado." : "Horario deshabilitado."
        );
    }

    @Transactional
    public HorarioAtencion reservarHorario(Long horarioId) {
        HorarioAtencion horario = horarioAtencionRepository.findByIdForUpdate(horarioId)
                .orElseThrow(() -> new RuntimeException("El horario seleccionado no existe."));

        FechaAtencion fecha = horario.getFechaAtencion();
        if (fecha == null || !fecha.isActivo() || !horario.isActivo()) {
            throw new RuntimeException("El horario seleccionado no está disponible.");
        }
        if (fecha.getFecha().isBefore(LocalDate.now())) {
            throw new RuntimeException("La fecha de recojo ya no está disponible.");
        }
        if (fecha.getFecha().equals(LocalDate.now()) && horario.getHoraFin().isBefore(LocalTime.now())) {
            throw new RuntimeException("El horario seleccionado ya finalizó.");
        }

        long reservados = contarReservas(horario.getId());
        if (reservados >= horario.getCupoMaximo()) {
            throw new RuntimeException("El horario seleccionado está completo. Elija otro horario.");
        }

        return horario;
    }

    public String formatearRecojo(HorarioAtencion horario) {
        if (horario == null || horario.getFechaAtencion() == null) {
            return "Recojo en tienda";
        }
        return "Recojo en tienda | " + formatearFechaLabel(horario.getFechaAtencion().getFecha())
                + " " + formatearRangoHorario(horario);
    }

    private void aplicarHorarios(FechaAtencion entidad, List<HorarioAtencionRequest> horariosRequest, boolean creando) {
        Map<Long, HorarioAtencion> existentes = new LinkedHashMap<>();
        if (!creando) {
            for (HorarioAtencion h : entidad.getHorarios()) {
                existentes.put(h.getId(), h);
            }
        }

        Set<Long> idsRecibidos = new HashSet<>();
        List<HorarioAtencion> nuevos = new ArrayList<>();

        for (HorarioAtencionRequest req : horariosRequest) {
            HorarioAtencion horario;
            if (req.getId() != null && existentes.containsKey(req.getId())) {
                horario = existentes.get(req.getId());
                idsRecibidos.add(req.getId());
            } else {
                horario = new HorarioAtencion();
                horario.setFechaAtencion(entidad);
            }

            LocalTime inicio = parseHora(req.getHoraInicio(), "hora de inicio");
            LocalTime fin = parseHora(req.getHoraFin(), "hora de fin");
            if (!fin.isAfter(inicio)) {
                throw new RuntimeException("La hora de fin debe ser posterior a la hora de inicio.");
            }

            int cupo = req.getCupoMaximo() != null ? req.getCupoMaximo() : 1;
            if (cupo < 1) {
                throw new RuntimeException("El cupo máximo debe ser al menos 1.");
            }

            long reservados = horario.getId() != null ? contarReservas(horario.getId()) : 0;
            if (cupo < reservados) {
                throw new RuntimeException("El cupo máximo no puede ser menor que las reservas confirmadas (" + reservados + ").");
            }

            horario.setHoraInicio(inicio);
            horario.setHoraFin(fin);
            horario.setCupoMaximo(cupo);
            if (req.getActivo() != null) {
                horario.setActivo(req.getActivo());
            } else if (horario.getId() == null) {
                horario.setActivo(true);
            }

            nuevos.add(horario);
        }

        if (!creando) {
            for (HorarioAtencion existente : new ArrayList<>(entidad.getHorarios())) {
                if (!idsRecibidos.contains(existente.getId())) {
                    if (contarReservas(existente.getId()) > 0) {
                        throw new RuntimeException("No se puede eliminar un horario con reservas confirmadas.");
                    }
                    entidad.getHorarios().remove(existente);
                }
            }
        }

        entidad.getHorarios().clear();
        for (HorarioAtencion horario : nuevos) {
            entidad.addHorario(horario);
        }
    }

    private Map<String, Object> mapearFechaAdmin(FechaAtencion fecha) {
        List<Map<String, Object>> horarios = new ArrayList<>();
        for (HorarioAtencion horario : fecha.getHorarios()) {
            long reservados = contarReservas(horario.getId());
            int disponibles = Math.max(0, horario.getCupoMaximo() - (int) reservados);

            Map<String, Object> mapa = new LinkedHashMap<>();
            mapa.put("id", horario.getId());
            mapa.put("horaInicio", horario.getHoraInicio().format(FMT_HORA));
            mapa.put("horaFin", horario.getHoraFin().format(FMT_HORA));
            mapa.put("label", formatearRangoHorario(horario));
            mapa.put("cupoMaximo", horario.getCupoMaximo());
            mapa.put("reservados", reservados);
            mapa.put("disponibles", disponibles);
            mapa.put("completo", disponibles <= 0);
            mapa.put("activo", horario.isActivo());
            horarios.add(mapa);
        }

        Map<String, Object> mapa = new LinkedHashMap<>();
        mapa.put("id", fecha.getId());
        mapa.put("fecha", fecha.getFecha().format(FMT_FECHA_ISO));
        mapa.put("fechaLabel", formatearFechaLabel(fecha.getFecha()));
        mapa.put("activo", fecha.isActivo());
        mapa.put("horarios", horarios);
        return mapa;
    }

    private long contarReservas(Long horarioId) {
        if (horarioId == null) {
            return 0;
        }
        return pedidoCatalogoRepository.countByHorarioRecojoIdAndEstadoIn(horarioId, ESTADOS_OCUPAN_CUPO);
    }

    private LocalDate parseFecha(String valor) {
        if (valor == null || valor.isBlank()) {
            throw new RuntimeException("La fecha es obligatoria.");
        }
        try {
            return LocalDate.parse(valor.trim(), FMT_FECHA_ISO);
        } catch (Exception e) {
            throw new RuntimeException("Fecha no válida.");
        }
    }

    private LocalTime parseHora(String valor, String campo) {
        if (valor == null || valor.isBlank()) {
            throw new RuntimeException("Ingrese la " + campo + ".");
        }
        try {
            return LocalTime.parse(valor.trim(), FMT_HORA);
        } catch (Exception e) {
            throw new RuntimeException("Formato de " + campo + " no válido (use HH:mm).");
        }
    }

    private String formatearRangoHorario(HorarioAtencion horario) {
        return horario.getHoraInicio().format(FMT_HORA) + " - " + horario.getHoraFin().format(FMT_HORA);
    }

    private String formatearFechaLabel(LocalDate fecha) {
        String dia = fecha.getDayOfWeek().getDisplayName(TextStyle.FULL, LOCALE_ES_PE);
        dia = dia.substring(0, 1).toUpperCase() + dia.substring(1);
        String mes = fecha.getMonth().getDisplayName(TextStyle.FULL, LOCALE_ES_PE);
        return dia + " " + fecha.getDayOfMonth() + " de " + mes + " " + fecha.getYear();
    }
}
