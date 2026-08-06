package com.huamanga.tourism.favorito.service;

import com.huamanga.tourism.common.domain.Idioma;
import com.huamanga.tourism.common.exception.RecursoNoEncontradoException;
import com.huamanga.tourism.common.seguridad.UsuarioActual;
import com.huamanga.tourism.favorito.domain.Favorito;
import com.huamanga.tourism.favorito.domain.FavoritoId;
import com.huamanga.tourism.favorito.repository.FavoritoRepository;
import com.huamanga.tourism.lugar.domain.EstadoLugar;
import com.huamanga.tourism.lugar.domain.Lugar;
import com.huamanga.tourism.lugar.dto.LugarResumenResponse;
import com.huamanga.tourism.lugar.mapper.LugarMapper;
import com.huamanga.tourism.lugar.repository.EstadisticaLugarRepository;
import com.huamanga.tourism.lugar.repository.LugarRepository;
import com.huamanga.tourism.usuario.repository.UsuarioRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Favoritos (RF-35).
 *
 * <p>Se guardan en el servidor, no en el navegador. Es una decision del plan:
 * esto es una web tradicional sin cache offline, y una lista guardada en
 * {@code localStorage} se perderia al cambiar de dispositivo o al limpiar el
 * navegador, que es justo cuando mas duele.</p>
 */
@Service
public class FavoritoService {

    private final FavoritoRepository favoritoRepository;
    private final LugarRepository lugarRepository;
    private final UsuarioRepository usuarioRepository;
    private final EstadisticaLugarRepository estadisticaRepository;
    private final LugarMapper mapper;

    public FavoritoService(FavoritoRepository favoritoRepository,
                           LugarRepository lugarRepository,
                           UsuarioRepository usuarioRepository,
                           EstadisticaLugarRepository estadisticaRepository,
                           LugarMapper mapper) {
        this.favoritoRepository = favoritoRepository;
        this.lugarRepository = lugarRepository;
        this.usuarioRepository = usuarioRepository;
        this.estadisticaRepository = estadisticaRepository;
        this.mapper = mapper;
    }

    /**
     * Marca o desmarca, devolviendo el estado resultante.
     *
     * <p>Es idempotente por diseno: marcar dos veces deja el favorito marcado
     * en lugar de fallar. En un boton que se pulsa con el pulgar en un movil,
     * el doble toque accidental es la norma, y un 409 seria un castigo por algo
     * que no es un error.</p>
     *
     * @return true si quedo marcado
     */
    @Transactional
    public boolean alternar(String slugLugar) {
        UUID usuarioId = UsuarioActual.idObligatorio();
        Lugar lugar = lugarPublicado(slugLugar);
        FavoritoId id = new FavoritoId(usuarioId, lugar.getId());

        if (favoritoRepository.existsById(id)) {
            favoritoRepository.deleteById(id);
            return false;
        }

        Favorito favorito = new Favorito();
        favorito.setId(id);
        favorito.setUsuario(usuarioRepository.getReferenceById(usuarioId));
        favorito.setLugar(lugar);
        favoritoRepository.save(favorito);
        return true;
    }

    @Transactional(readOnly = true)
    public boolean esFavorito(String slugLugar) {
        Lugar lugar = lugarPublicado(slugLugar);
        return favoritoRepository.existsById(
                new FavoritoId(UsuarioActual.idObligatorio(), lugar.getId()));
    }

    /**
     * Lista del perfil.
     *
     * <p>Se devuelven como {@code LugarResumenResponse}, el mismo DTO del
     * listado: asi la tarjeta de favoritos es exactamente la del catalogo, con
     * su insignia de abierto/cerrado y su distancia a pie, sin duplicar
     * componentes en el frontend.</p>
     */
    @Transactional(readOnly = true)
    public Page<LugarResumenResponse> mios(Idioma idioma, Pageable pagina) {
        return favoritoRepository
                .findByUsuarioConLugar(UsuarioActual.idObligatorio(), pagina)
                .map(favorito -> {
                    Lugar lugar = favorito.getLugar();
                    return mapper.aResumen(lugar, idioma,
                            estadisticaRepository.findById(lugar.getId()).orElse(null));
                });
    }

    private Lugar lugarPublicado(String slug) {
        return lugarRepository.findBySlug(slug)
                .filter(l -> l.getEstado() == EstadoLugar.PUBLICADO)
                .orElseThrow(() -> new RecursoNoEncontradoException("lugar", slug));
    }
}
