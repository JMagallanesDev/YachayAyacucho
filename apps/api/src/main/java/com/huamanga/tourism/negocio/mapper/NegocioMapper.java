package com.huamanga.tourism.negocio.mapper;

import com.huamanga.tourism.common.domain.Idioma;
import com.huamanga.tourism.negocio.domain.Negocio;
import com.huamanga.tourism.negocio.dto.NegocioResponse;
import org.springframework.stereotype.Component;

/**
 * Entidad a DTO. Los controllers nunca devuelven entidades JPA.
 *
 * <p>Se escribe a mano en vez de con MapStruct porque casi todos los campos
 * necesitan una decision —elegir traduccion, extraer coordenadas del punto,
 * decidir si el RUC sale o no— y una interfaz llena de {@code @Mapping} seria
 * mas larga y menos clara que esto.</p>
 */
@Component
public class NegocioMapper {

    /**
     * Ficha publica. <strong>Nunca incluye el RUC</strong>: es un dato fiscal
     * que se pide para verificar, no para publicar.
     */
    public NegocioResponse aRespuesta(Negocio negocio, Idioma idioma) {
        return new NegocioResponse(
                negocio.getId(),
                negocio.getNombre(),
                descripcion(negocio, idioma),
                negocio.getCategoria().getCodigo(),
                nombreDeCategoria(negocio, idioma),
                negocio.getCategoria().getIcono(),
                negocio.getDistrito().getNombre(),
                negocio.getTelefono(),
                negocio.getWhatsapp(),
                negocio.getDireccion(),
                negocio.getUbicacion() != null ? negocio.getUbicacion().getX() : null,
                negocio.getUbicacion() != null ? negocio.getUbicacion().getY() : null,
                negocio.getHorarioTexto(),
                negocio.getEstado(),
                negocio.getCreatedAt());
    }

    private String descripcion(Negocio negocio, Idioma idioma) {
        return negocio.getTraducciones().stream()
                .filter(t -> t.getId().getIdioma() == idioma)
                .findFirst()
                .or(() -> negocio.getTraducciones().stream()
                        .filter(t -> t.getId().getIdioma() == Idioma.ES)
                        .findFirst())
                .map(t -> t.getDescripcion())
                .orElse(null);
    }

    private String nombreDeCategoria(Negocio negocio, Idioma idioma) {
        var categoria = negocio.getCategoria();
        return categoria.getTraducciones().stream()
                .filter(t -> t.getId().getIdioma() == idioma)
                .findFirst()
                .or(() -> categoria.getTraducciones().stream()
                        .filter(t -> t.getId().getIdioma() == Idioma.ES)
                        .findFirst())
                .map(t -> t.getNombre())
                .orElse(categoria.getCodigo());
    }
}
