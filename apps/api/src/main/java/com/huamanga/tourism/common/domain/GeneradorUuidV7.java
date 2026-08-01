package com.huamanga.tourism.common.domain;

import com.github.f4b6a3.uuid.UuidCreator;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.generator.BeforeExecutionGenerator;
import org.hibernate.generator.EventType;
import org.hibernate.generator.EventTypeSets;
import org.hibernate.generator.GeneratorCreationContext;

import java.lang.reflect.Member;
import java.util.EnumSet;

/**
 * Genera identificadores UUID v7 (RFC 9562) en el backend.
 *
 * <p>Por que UUID v7 y no autoincremental: no revela el tamano de la base de
 * datos, es unico globalmente sin coordinacion y, a diferencia del v4, es
 * ordenable por tiempo, asi que los indices B-tree no se fragmentan al
 * insertar.</p>
 *
 * <p>Se genera aqui y no en PostgreSQL porque ni Java 21 ni PostgreSQL 16
 * traen v7 nativo. La funcion {@code uuid_generar_v7()} de la migracion V1
 * cubre el caso simetrico: lo que se inserta por SQL.</p>
 */
public class GeneradorUuidV7 implements BeforeExecutionGenerator {

    /**
     * Hibernate exige exactamente esta firma para instanciar un generador
     * declarado con {@link org.hibernate.annotations.IdGeneratorType}.
     *
     * <p>Los tres tipos deben coincidir al detalle. Si el tercer parametro se
     * declara como {@code Object} en lugar de {@link GeneratorCreationContext},
     * Hibernate no reconoce el constructor y delega la creacion al contenedor
     * de beans, que intenta inyectar la anotacion como si fuera un bean y el
     * arranque falla.</p>
     */
    public GeneradorUuidV7(UuidV7 configuracion, Member miembro, GeneratorCreationContext contexto) {
        // Sin estado: el generador no necesita configuracion.
    }

    @Override
    public Object generate(SharedSessionContractImplementor sesion,
                           Object entidad,
                           Object valorActual,
                           EventType tipoEvento) {
        return UuidCreator.getTimeOrderedEpoch();
    }

    @Override
    public EnumSet<EventType> getEventTypes() {
        // Solo al insertar: la clave primaria de una fila nunca cambia.
        return EventTypeSets.INSERT_ONLY;
    }
}
