package com.huamanga.tourism.negocio.controller;

import com.huamanga.tourism.common.domain.Idioma;
import com.huamanga.tourism.negocio.dto.MiNegocioResponse;
import com.huamanga.tourism.negocio.dto.NegocioRequest;
import com.huamanga.tourism.negocio.dto.NegocioResponse;
import com.huamanga.tourism.negocio.repository.CategoriaNegocioRepository;
import com.huamanga.tourism.negocio.service.MiNegocioService;
import com.huamanga.tourism.negocio.service.NegocioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * Directorio de negocios: consulta publica y panel del dueno
 * (RF-104, RF-105, RF-107).
 *
 * <p><strong>Dos niveles de permiso muy distintos.</strong> Leer el directorio
 * no exige cuenta. Registrar y editar exigen sesion, y ademas <em>ser el gestor
 * de ese negocio concreto</em>: eso ultimo no lo puede expresar una anotacion,
 * asi que lo comprueba {@code GuardaDePropiedad} dentro del servicio. Tener el
 * rol NEGOCIO no da acceso al negocio de otra persona.</p>
 */
@RestController
@RequestMapping("/negocios")
@Tag(name = "Negocios", description = "Directorio local y panel del propietario")
public class NegocioController {

    private final NegocioService negocioService;
    private final MiNegocioService miNegocioService;
    private final CategoriaNegocioRepository categoriaRepository;

    public NegocioController(NegocioService negocioService,
                             MiNegocioService miNegocioService,
                             CategoriaNegocioRepository categoriaRepository) {
        this.negocioService = negocioService;
        this.miNegocioService = miNegocioService;
        this.categoriaRepository = categoriaRepository;
    }

    // ---------------------------------------------------------------
    //  Publico (RF-105)
    // ---------------------------------------------------------------

    @GetMapping
    @Operation(summary = "Directorio de negocios aprobados",
            description = """
                    Solo devuelve negocios APROBADOS. El estado esta escrito
                    dentro de la consulta, no llega por parametro: no hay forma
                    de pedir por aqui uno pendiente.
                    """)
    public Page<NegocioResponse> directorio(
            @RequestParam(required = false) UUID categoriaId,
            @RequestParam(required = false) Idioma idioma,
            @PageableDefault(size = 12) Pageable pagina) {

        return negocioService.directorio(categoriaId, resolverIdioma(idioma), pagina);
    }

    @GetMapping("/categorias")
    @Operation(summary = "Categorias del directorio")
    @Transactional(readOnly = true)
    public List<CategoriaResponse> categorias(@RequestParam(required = false) Idioma idioma) {
        Idioma elegido = resolverIdioma(idioma);

        return categoriaRepository.findAll(org.springframework.data.domain.Sort.by("orden")).stream()
                .map(categoria -> new CategoriaResponse(
                        categoria.getId(),
                        categoria.getCodigo(),
                        categoria.getTraducciones().stream()
                                .filter(t -> t.getId().getIdioma() == elegido)
                                .findFirst()
                                .or(() -> categoria.getTraducciones().stream()
                                        .filter(t -> t.getId().getIdioma() == Idioma.ES)
                                        .findFirst())
                                .map(t -> t.getNombre())
                                .orElse(categoria.getCodigo()),
                        categoria.getIcono()))
                .toList();
    }

    @Schema(description = "Categoria de negocio")
    public record CategoriaResponse(UUID id, String codigo, String nombre, String icono) {
    }

    // ---------------------------------------------------------------
    //  Panel propio (RF-104, RF-107)
    // ---------------------------------------------------------------
    //  Ojo con el orden: /mios va ANTES que /{id}. Al reves, Spring
    //  intentaria interpretar "mios" como un UUID y devolveria un 400.

    @GetMapping("/mios")
    @PreAuthorize("isAuthenticated()")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Mis negocios, en cualquier estado",
            description = "Incluye el RUC y las metricas agregadas, que no salen en la ficha publica.")
    public List<MiNegocioResponse> mios(@RequestParam(required = false) Idioma idioma) {
        return miNegocioService.mios(resolverIdioma(idioma));
    }

    @GetMapping("/mios/{negocioId}")
    @PreAuthorize("isAuthenticated()")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Un negocio propio",
            description = "Devuelve 403 si el negocio existe pero lo gestiona otra persona.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "El negocio"),
            @ApiResponse(responseCode = "403", description = "El negocio es de otra persona"),
            @ApiResponse(responseCode = "404", description = "No existe")
    })
    public MiNegocioResponse mio(@PathVariable UUID negocioId,
                                 @RequestParam(required = false) Idioma idioma) {
        return miNegocioService.uno(negocioId, resolverIdioma(idioma));
    }

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Registrar un negocio (RF-104)",
            description = """
                    Cualquier usuario con cuenta puede solicitarlo. El negocio
                    nace PENDIENTE y no aparece en el directorio hasta que un
                    administrador lo aprueba: la peticion no admite el campo
                    estado, asi que no hay forma de publicarse sin revision.
                    """)
    @ApiResponse(responseCode = "201", description = "Registrado, a la espera de revision")
    public ResponseEntity<MiNegocioResponse> registrar(
            @Valid @RequestBody NegocioRequest peticion,
            @RequestParam(required = false) Idioma idioma) {

        MiNegocioResponse creado = miNegocioService.registrar(peticion, resolverIdioma(idioma));
        return ResponseEntity.status(HttpStatus.CREATED).body(creado);
    }

    @PutMapping("/mios/{negocioId}")
    @PreAuthorize("isAuthenticated()")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Editar el negocio propio (RF-107)",
            description = """
                    Si cambian el nombre, la categoria o la descripcion, el
                    negocio vuelve a PENDIENTE: si no, se aprobaria una cosa y
                    se publicaria otra.
                    """)
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Actualizado"),
            @ApiResponse(responseCode = "403", description = "El negocio es de otra persona")
    })
    public MiNegocioResponse actualizar(@PathVariable UUID negocioId,
                                        @Valid @RequestBody NegocioRequest peticion,
                                        @RequestParam(required = false) Idioma idioma) {
        return miNegocioService.actualizar(negocioId, peticion, resolverIdioma(idioma));
    }

    // ---------------------------------------------------------------
    //  Ficha publica: va la ultima para no capturar /mios ni /categorias
    // ---------------------------------------------------------------

    @GetMapping("/{id}")
    @Operation(summary = "Ficha de un negocio aprobado",
            description = "Anota la visita en la analitica agregada, sin guardar quien entro.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Ficha"),
            @ApiResponse(responseCode = "404", description = "No existe o no esta aprobado")
    })
    public NegocioResponse ficha(@PathVariable UUID id,
                                 @RequestParam(required = false) Idioma idioma,
                                 HttpServletRequest peticion) {
        return negocioService.ficha(id, resolverIdioma(idioma), peticion);
    }

    private Idioma resolverIdioma(Idioma explicito) {
        if (explicito != null) {
            return explicito;
        }
        return "en".equals(LocaleContextHolder.getLocale().getLanguage()) ? Idioma.EN : Idioma.ES;
    }
}
