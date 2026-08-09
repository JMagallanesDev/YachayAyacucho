package com.huamanga.tourism.negocio.service;

import com.huamanga.tourism.analitica.service.RegistroVisitasService;
import com.huamanga.tourism.common.domain.Idioma;
import com.huamanga.tourism.common.exception.RecursoNoEncontradoException;
import com.huamanga.tourism.negocio.domain.EstadoNegocio;
import com.huamanga.tourism.negocio.domain.Negocio;
import com.huamanga.tourism.negocio.dto.NegocioResponse;
import com.huamanga.tourism.negocio.mapper.NegocioMapper;
import com.huamanga.tourism.negocio.repository.NegocioRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Directorio publico de negocios (RF-105).
 *
 * <p><strong>La regla que gobierna esta clase: lo que no esta APROBADO no
 * existe aqui.</strong> No es un filtro que se anade en cada consulta y que
 * alguien puede olvidar manana; es que ninguno de estos metodos acepta un estado
 * como parametro. Un negocio pendiente, rechazado o suspendido no tiene forma de
 * salir por este servicio, ni por el listado ni por su ficha.</p>
 */
@Service
public class NegocioService {

    private final NegocioRepository negocioRepository;
    private final NegocioMapper mapper;
    private final RegistroVisitasService registroVisitas;

    public NegocioService(NegocioRepository negocioRepository,
                          NegocioMapper mapper,
                          RegistroVisitasService registroVisitas) {
        this.negocioRepository = negocioRepository;
        this.mapper = mapper;
        this.registroVisitas = registroVisitas;
    }

    @Transactional(readOnly = true)
    public Page<NegocioResponse> directorio(UUID categoriaId, Idioma idioma, Pageable pagina) {
        Page<Negocio> negocios = categoriaId == null
                ? negocioRepository.findAprobadosConDetalle(pagina)
                : negocioRepository.findAprobadosPorCategoria(categoriaId, pagina);

        return negocios.map(negocio -> mapper.aRespuesta(negocio, idioma));
    }

    /**
     * Ficha publica de un negocio, y anota la visita (RF-52b).
     *
     * <p>El conteo va aqui y no en el frontend porque abrir la ficha <em>es</em>
     * la visita. Lo que si viene del navegador son los clics de WhatsApp y de
     * como llegar, que ocurren despues y sin recargar nada.</p>
     *
     * <p><strong>Sin {@code readOnly}, y no es un descuido.</strong> Leer la
     * ficha es una lectura, pero contarla es una escritura, y las dos ocurren en
     * la misma transaccion: marcada como de solo lectura, PostgreSQL rechaza el
     * INSERT del contador y la ficha entera devuelve un error. Separarlas en dos
     * transacciones costaria una conexion mas por visita para ahorrar una
     * anotacion.</p>
     */
    @Transactional
    public NegocioResponse ficha(UUID id, Idioma idioma, HttpServletRequest peticion) {
        Negocio negocio = negocioRepository.findAprobadoConDetalle(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("negocio", id.toString()));

        registroVisitas.registrarNegocio(
                negocio.getId(), RegistroVisitasService.Interaccion.VISITA, peticion);

        return mapper.aRespuesta(negocio, idioma);
    }

    /** Cuantos negocios hay publicados, para la portada del directorio. */
    @Transactional(readOnly = true)
    public long publicados() {
        return negocioRepository.countByEstado(EstadoNegocio.APROBADO);
    }
}
