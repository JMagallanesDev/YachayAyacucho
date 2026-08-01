package com.huamanga.tourism.horario.domain;

import com.huamanga.tourism.common.domain.EntidadBase;
import com.huamanga.tourism.lugar.domain.Lugar;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalTime;
import java.util.UUID;

/**
 * Horario de un lugar patrimonial, en filas por dia y turno.
 *
 * <p>Es la entidad que hace computable el horario. Un texto como "Lun-Vie
 * 9:00-17:00" no permite calcular si el lugar esta abierto ahora mismo; estas
 * filas si, y con una sola entidad se habilitan tres funcionalidades: el badge
 * abierto/cerrado (RF-09b), el motor de recomendaciones (RF-08) y el
 * planificador de visitas (RF-29).</p>
 *
 * <p>La relacion 1:N admite varias filas por dia, que es como se modela el
 * cierre al mediodia tipico de las iglesias.</p>
 */
@Entity
@Table(name = "horario_lugar")
@Getter
@Setter
@NoArgsConstructor
public class HorarioLugar extends EntidadBase {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "lugar_id", nullable = false)
    private Lugar lugar;

    /** 0 = domingo ... 6 = sabado. La BD lo comprueba con un CHECK. */
    @Column(name = "dia_semana", nullable = false)
    private Short diaSemana;

    @Column(name = "hora_apertura")
    private LocalTime horaApertura;

    @Column(name = "hora_cierre")
    private LocalTime horaCierre;

    @Column(name = "cerrado", nullable = false)
    private boolean cerrado = false;

    @Column(name = "updated_by")
    private UUID updatedBy;

    /**
     * Si el lugar esta abierto a una hora dada segun esta franja.
     *
     * <p>La comparacion incluye la apertura y excluye el cierre: a la hora
     * exacta de cierre el lugar ya no admite visitantes.</p>
     */
    public boolean estaAbiertoA(LocalTime hora) {
        if (cerrado || horaApertura == null || horaCierre == null) {
            return false;
        }
        return !hora.isBefore(horaApertura) && hora.isBefore(horaCierre);
    }
}
