package com.huamanga.tourism.reporte.service;

import com.huamanga.tourism.common.domain.Idioma;
import com.huamanga.tourism.reporte.dto.TipoIncidenteResponse;
import com.huamanga.tourism.reporte.repository.TipoIncidenteRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Catalogo de tipos de incidente (RF-70).
 *
 * <p>Son siete y cambian una vez cada varios anios; se leen enteros de una vez
 * con sus traducciones. El formulario los pinta como botones grandes, no como
 * un desplegable: elegir tipo debe costar un toque, porque el requisito pide
 * completar el reporte en menos de un minuto.</p>
 */
@Service
public class TipoIncidenteService {

    private final TipoIncidenteRepository repositorio;

    public TipoIncidenteService(TipoIncidenteRepository repositorio) {
        this.repositorio = repositorio;
    }

    @Transactional(readOnly = true)
    public List<TipoIncidenteResponse> listar(Idioma idioma) {
        return repositorio.findAllConTraducciones().stream()
                .map(tipo -> new TipoIncidenteResponse(
                        tipo.getId(),
                        tipo.getCodigo(),
                        tipo.getTraducciones().stream()
                                .filter(t -> t.getId().getIdioma() == idioma)
                                .findFirst()
                                .or(() -> tipo.getTraducciones().stream()
                                        .filter(t -> t.getId().getIdioma() == Idioma.ES)
                                        .findFirst())
                                .map(t -> t.getNombre())
                                .orElse(tipo.getCodigo()),
                        tipo.getIcono(),
                        tipo.getColorHex()))
                .toList();
    }
}
