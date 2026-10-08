package com.test.desarrollo_web.dto;

import java.util.ArrayList;
import java.util.List;

public class FechaAtencionRequest {
    private String fecha;
    private Boolean activo;
    private List<HorarioAtencionRequest> horarios = new ArrayList<>();

    public String getFecha() {
        return fecha;
    }

    public void setFecha(String fecha) {
        this.fecha = fecha;
    }

    public Boolean getActivo() {
        return activo;
    }

    public void setActivo(Boolean activo) {
        this.activo = activo;
    }

    public List<HorarioAtencionRequest> getHorarios() {
        return horarios;
    }

    public void setHorarios(List<HorarioAtencionRequest> horarios) {
        this.horarios = horarios;
    }
}
