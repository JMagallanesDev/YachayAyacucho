package com.huamanga.tourism.lugar.service;

import com.huamanga.tourism.common.exception.RecursoNoEncontradoException;
import com.huamanga.tourism.lugar.domain.LugarImagenHistorica;
import com.huamanga.tourism.lugar.dto.ImagenHistoricaResponse;
import com.huamanga.tourism.lugar.repository.LugarImagenHistoricaRepository;
import com.huamanga.tourism.lugar.repository.LugarRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Fotos historicas de un lugar, para el slider antes/despues (RF-11).
 *
 * <p>Publico y sin cuenta: es contenido patrimonial, que es justo lo que esta
 * aplicacion existe para difundir.</p>
 */
@Service
public class ImagenHistoricaService {

    private final LugarImagenHistoricaRepository imagenRepository;
    private final LugarRepository lugarRepository;

    public ImagenHistoricaService(LugarImagenHistoricaRepository imagenRepository,
                                  LugarRepository lugarRepository) {
        this.imagenRepository = imagenRepository;
        this.lugarRepository = lugarRepository;
    }

    /**
     * Las fotos historicas de un lugar, en orden.
     *
     * <p>Devuelve lista vacia si el lugar no tiene ninguna, que es el caso
     * habitual: solo unos pocos monumentos tienen fotografia antigua
     * localizable. El frontend simplemente no pinta la seccion.</p>
     */
    @Transactional(readOnly = true)
    public List<ImagenHistoricaResponse> deLugar(String slug) {
        var lugar = lugarRepository.findBySlug(slug)
                .orElseThrow(() -> new RecursoNoEncontradoException("lugar", slug));

        return imagenRepository.findByLugarIdOrderByOrdenAsc(lugar.getId()).stream()
                .map(this::aRespuesta)
                .toList();
    }

    /**
     * Todas las que tienen punto de captura, para el modo «Parate aqui».
     *
     * <p>Se piden todas de golpe y no una por lugar: el modo geolocalizado
     * compara tu posicion contra <em>cualquier</em> punto de captura de la
     * ciudad, y hacerlo con una peticion por monumento seria absurdo. Son unas
     * pocas decenas de filas con seis campos.</p>
     */
    @Transactional(readOnly = true)
    public List<ImagenHistoricaResponse> conPuntoDeCaptura() {
        return imagenRepository.findConPuntoDeCaptura().stream()
                .map(this::aRespuesta)
                .toList();
    }

    private ImagenHistoricaResponse aRespuesta(LugarImagenHistorica imagen) {
        var punto = imagen.getPuntoCaptura();

        return new ImagenHistoricaResponse(
                imagen.getId(),
                imagen.getTitulo(),
                imagen.getUrlHistorica(),
                imagen.getAnioHistorico(),
                imagen.getUrlActual(),
                imagen.getCreditoHistorico(),
                punto != null ? punto.getX() : null,
                punto != null ? punto.getY() : null,
                imagen.getOrden());
    }
}
