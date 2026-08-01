package com.huamanga.tourism.insignia.domain;

import com.huamanga.tourism.common.domain.EntidadBase;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Insignia del pasaporte patrimonial (8 valores sembrados en la V14).
 *
 * <p>{@code criterio} es JSONB porque cada insignia se gana de una forma
 * distinta: por numero de check-ins, por categoria visitada, por completar una
 * ruta o por resenas publicadas. Modelar eso en columnas obligaria a una
 * tabla con decenas de campos casi siempre nulos.</p>
 *
 * <p>No viola 1FN: es un valor de configuracion opaco al modelo. La base de
 * datos nunca hace JOIN ni evalua logica dentro del JSON; lo interpreta el
 * service de gamificacion (seccion 6.6).</p>
 */
@Entity
@Table(name = "insignia")
@Getter
@Setter
@NoArgsConstructor
public class Insignia extends EntidadBase {

    @Column(name = "codigo", nullable = false, unique = true, length = 30)
    private String codigo;

    @Column(name = "icono", nullable = false, length = 50)
    private String icono;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "criterio", nullable = false, columnDefinition = "jsonb")
    private String criterio;

    @Column(name = "orden", nullable = false)
    private Short orden = 0;

    @OneToMany(mappedBy = "insignia", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<InsigniaTraduccion> traducciones = new LinkedHashSet<>();
}
