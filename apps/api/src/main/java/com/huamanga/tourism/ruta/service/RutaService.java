package com.huamanga.tourism.ruta.service;

import com.huamanga.tourism.common.domain.Idioma;
import com.huamanga.tourism.common.exception.RecursoNoEncontradoException;
import com.huamanga.tourism.lugar.domain.EstadoLugar;
import com.huamanga.tourism.lugar.domain.Lugar;
import com.huamanga.tourism.ruta.domain.LugarRuta;
import com.huamanga.tourism.ruta.domain.RutaTematica;
import com.huamanga.tourism.ruta.dto.RutaResponse;
import com.huamanga.tourism.ruta.repository.RutaTematicaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

/**
 * Rutas tematicas (RF-20).
 *
 * <p>Solo lectura en este bloque: las rutas se administran desde el panel del
 * Bloque 10. Aqui se leen para pintarlas en el mapa.</p>
 */
@Service
public class RutaService {

    private final RutaTematicaRepository rutaRepository;

    public RutaService(RutaTematicaRepository rutaRepository) {
        this.rutaRepository = rutaRepository;
    }

    @Transactional(readOnly = true)
    public List<RutaResponse> listarActivas(Idioma idioma) {
        return rutaRepository.findActivasConRecorrido().stream()
                .map(ruta -> aRespuesta(ruta, idioma))
                .toList();
    }

    @Transactional(readOnly = true)
    public RutaResponse buscarPorSlug(String slug, Idioma idioma) {
        return rutaRepository.findBySlugConRecorrido(slug)
                .map(ruta -> aRespuesta(ruta, idioma))
                .orElseThrow(() -> new RecursoNoEncontradoException("ruta", slug));
    }

    private RutaResponse aRespuesta(RutaTematica ruta, Idioma idioma) {
        var traduccion = ruta.getTraducciones().stream()
                .filter(t -> t.getId().getIdioma() == idioma)
                .findFirst()
                .or(() -> ruta.getTraducciones().stream()
                        .filter(t -> t.getId().getIdioma() == Idioma.ES)
                        .findFirst());

        List<RutaResponse.Parada> paradas = ruta.getLugares().stream()
                // Un lugar despublicado o dado de baja no puede seguir
                // apareciendo en el recorrido: la polilinea llevaria a una
                // ficha que devuelve 404.
                .filter(enlace -> enlace.getLugar() != null
                        && enlace.getLugar().getEstado() == EstadoLugar.PUBLICADO)
                .sorted(Comparator.comparing(LugarRuta::getOrden))
                .map(enlace -> aParada(enlace, idioma))
                .toList();

        return new RutaResponse(
                ruta.getId(),
                ruta.getSlug(),
                traduccion.map(t -> t.getNombre()).orElse(ruta.getSlug()),
                traduccion.map(t -> t.getDescripcion()).orElse(null),
                ruta.getColorHex(),
                ruta.getIcono(),
                paradas);
    }

    private RutaResponse.Parada aParada(LugarRuta enlace, Idioma idioma) {
        Lugar lugar = enlace.getLugar();
        return new RutaResponse.Parada(
                lugar.getId(),
                lugar.getSlug(),
                lugar.getTraducciones().stream()
                        .filter(t -> t.getId().getIdioma() == idioma)
                        .findFirst()
                        .or(() -> lugar.getTraducciones().stream()
                                .filter(t -> t.getId().getIdioma() == Idioma.ES)
                                .findFirst())
                        .map(t -> t.getNombre())
                        .orElse(lugar.getSlug()),
                enlace.getOrden(),
                lugar.getUbicacion().getX(),
                lugar.getUbicacion().getY());
    }
}
