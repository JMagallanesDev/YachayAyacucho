package com.huamanga.tourism.lugar.controller;

import com.huamanga.tourism.common.domain.Idioma;
import com.huamanga.tourism.lugar.domain.OrdenLugares;
import com.huamanga.tourism.lugar.dto.LugarDetalleResponse;
import com.huamanga.tourism.lugar.dto.LugarRequest;
import com.huamanga.tourism.lugar.dto.LugarResumenResponse;
import com.huamanga.tourism.lugar.service.LugarService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Lugares patrimoniales: consulta publica y gestion por el administrador.
 *
 * <p><strong>Reparto de permisos (RF-47, RNF-16).</strong> Leer es publico:
 * la navegacion anonima esta permitida en todo el sitio (RF-34). Crear,
 * editar y borrar es exclusivo del rol ADMIN, y se comprueba con
 * {@code @PreAuthorize} en cada metodo, no confiando en que el frontend
 * esconda un boton.</p>
 */
@RestController
@RequestMapping("/lugares")
@Tag(name = "Lugares", description = "Consulta y gestion de lugares patrimoniales")
public class LugarController {

    private final LugarService lugarService;

    public LugarController(LugarService lugarService) {
        this.lugarService = lugarService;
    }

    // ---------------------------------------------------------------
    //  Publico
    // ---------------------------------------------------------------

    @GetMapping
    @Operation(summary = "Explorar lugares publicados",
            description = """
                    Buscar, filtrar y ordenar en una sola llamada
                    (RF-01, RF-02, RF-04, RF-05, RF-06). Todos los parametros
                    son opcionales: sin ninguno devuelve el catalogo completo
                    ordenado por nombre.

                    Cada resultado incluye su grilla horaria, para que el
                    cliente calcule el estado abierto/cerrado en el momento de
                    mirarlo, y sus coordenadas, para calcular la distancia a
                    pie sin llamadas adicionales.
                    """)
    public Page<LugarResumenResponse> explorar(
            @Parameter(description = "Idioma del contenido; si se omite se usa Accept-Language")
            @RequestParam(required = false) Idioma idioma,
            @Parameter(description = "Texto libre: busca en nombre, descripcion e historia")
            @RequestParam(required = false) String q,
            @Parameter(description = "Filtrar por categoria")
            @RequestParam(required = false) UUID categoriaId,
            @Parameter(description = "Calificacion minima, de 0 a 5")
            @RequestParam(required = false) BigDecimal calificacionMinima,
            @Parameter(description = "ALFABETICO, MEJOR_VALORADOS o MAS_VISITADOS")
            @RequestParam(required = false) OrdenLugares orden,
            @PageableDefault(size = 12) Pageable pagina) {

        var criterios = new LugarService.CriteriosBusqueda(q, categoriaId, calificacionMinima, orden);
        return lugarService.explorar(criterios, resolverIdioma(idioma), pagina);
    }

    @GetMapping("/{slug}")
    @Operation(summary = "Ficha completa de un lugar",
            description = """
                    Devuelve el contenido en el idioma pedido, con sus horarios,
                    los datos practicos del bloque "Antes de ir" y el estado
                    abierto/cerrado calculado al momento.
                    """)
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Ficha del lugar"),
            @ApiResponse(responseCode = "404", description = "No existe, o es un borrador y quien pregunta no es administrador")
    })
    public LugarDetalleResponse detalle(
            @PathVariable String slug,
            @RequestParam(required = false) Idioma idioma) {
        return lugarService.buscarPorSlug(slug, resolverIdioma(idioma));
    }

    // ---------------------------------------------------------------
    //  Solo administracion (RF-47)
    // ---------------------------------------------------------------

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Crear un lugar",
            description = "Guarda el lugar, sus traducciones y su grilla horaria en una sola transaccion.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Lugar creado"),
            @ApiResponse(responseCode = "400", description = "Datos invalidos o coordenadas fuera de Ayacucho"),
            @ApiResponse(responseCode = "401", description = "Sin autenticar"),
            @ApiResponse(responseCode = "403", description = "Autenticado pero sin rol ADMIN"),
            @ApiResponse(responseCode = "409", description = "El identificador de URL ya esta en uso")
    })
    public ResponseEntity<LugarDetalleResponse> crear(
            @Valid @RequestBody LugarRequest peticion,
            @RequestParam(required = false) Idioma idioma) {

        LugarDetalleResponse creado = lugarService.crear(peticion, resolverIdioma(idioma));
        return ResponseEntity.status(HttpStatus.CREATED).body(creado);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Actualizar un lugar",
            description = "Reemplaza el lugar completo, incluidas traducciones y horarios.")
    public LugarDetalleResponse actualizar(
            @PathVariable UUID id,
            @Valid @RequestBody LugarRequest peticion,
            @RequestParam(required = false) Idioma idioma) {
        return lugarService.actualizar(id, peticion, resolverIdioma(idioma));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Dar de baja un lugar",
            description = "Baja logica: la fila se conserva para auditoria y se puede revertir.")
    @ApiResponse(responseCode = "204", description = "Lugar dado de baja")
    public ResponseEntity<Void> eliminar(@PathVariable UUID id) {
        lugarService.eliminar(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * El parametro explicito manda sobre la cabecera.
     *
     * <p>Las paginas ISR se generan una por idioma, y tener el idioma visible
     * en la URL hace evidente cual es la clave de cache. Sin parametro se cae
     * a lo que negocie Accept-Language.</p>
     */
    private Idioma resolverIdioma(Idioma explicito) {
        if (explicito != null) {
            return explicito;
        }
        return "en".equals(LocaleContextHolder.getLocale().getLanguage()) ? Idioma.EN : Idioma.ES;
    }
}
