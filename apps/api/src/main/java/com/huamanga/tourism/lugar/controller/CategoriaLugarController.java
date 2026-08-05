package com.huamanga.tourism.lugar.controller;

import com.huamanga.tourism.common.domain.Idioma;
import com.huamanga.tourism.lugar.dto.LugarDetalleResponse.CategoriaResponse;
import com.huamanga.tourism.lugar.repository.CategoriaLugarRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Categorias de lugar, para los filtros del listado (RF-04).
 *
 * <p>Publico y de solo lectura: son datos de catalogo que cambian una vez al
 * anio. El CRUD de categorias, si llega a hacer falta, sera cosa del panel de
 * administracion.</p>
 */
@RestController
@RequestMapping("/categorias")
@Tag(name = "Categorias", description = "Categorias de lugares patrimoniales")
public class CategoriaLugarController {

    private final CategoriaLugarRepository categoriaRepository;

    public CategoriaLugarController(CategoriaLugarRepository categoriaRepository) {
        this.categoriaRepository = categoriaRepository;
    }

    @GetMapping
    @Operation(summary = "Listar categorias con su nombre traducido")
    @Transactional(readOnly = true)
    public List<CategoriaResponse> listar(@RequestParam(required = false) Idioma idioma) {
        Idioma resuelto = idioma != null
                ? idioma
                : ("en".equals(LocaleContextHolder.getLocale().getLanguage()) ? Idioma.EN : Idioma.ES);

        // Una sola consulta con las traducciones ya cargadas: son 8 categorias
        // y cada una necesita su nombre, un N+1 de manual si se dejara perezoso.
        return categoriaRepository.findAllConTraducciones().stream()
                .map(categoria -> new CategoriaResponse(
                        categoria.getId(),
                        categoria.getCodigo(),
                        categoria.getTraducciones().stream()
                                .filter(t -> t.getId().getIdioma() == resuelto)
                                .findFirst()
                                .or(() -> categoria.getTraducciones().stream()
                                        .filter(t -> t.getId().getIdioma() == Idioma.ES)
                                        .findFirst())
                                .map(t -> t.getNombre())
                                .orElse(categoria.getCodigo()),
                        categoria.getIcono(),
                        categoria.getColorHex()))
                .toList();
    }
}
