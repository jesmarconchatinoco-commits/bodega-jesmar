package com.test.desarrollo_web.Models;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "FECHA_ATENCION", uniqueConstraints = @UniqueConstraint(columnNames = "fecha"))
public class FechaAtencion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDate fecha;

    @Column(nullable = false)
    private boolean activo = true;

    @OneToMany(mappedBy = "fechaAtencion", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("horaInicio ASC")
    private List<HorarioAtencion> horarios = new ArrayList<>();

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public LocalDate getFecha() {
        return fecha;
    }

    public void setFecha(LocalDate fecha) {
        this.fecha = fecha;
    }

    public boolean isActivo() {
        return activo;
    }

    public void setActivo(boolean activo) {
        this.activo = activo;
    }

    public List<HorarioAtencion> getHorarios() {
        return horarios;
    }

    public void setHorarios(List<HorarioAtencion> horarios) {
        this.horarios = horarios;
    }

    public void addHorario(HorarioAtencion horario) {
        horarios.add(horario);
        horario.setFechaAtencion(this);
    }
}
