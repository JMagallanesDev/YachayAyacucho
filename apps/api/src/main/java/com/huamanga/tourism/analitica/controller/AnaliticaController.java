package com.huamanga.tourism.analitica.controller;

import com.huamanga.tourism.analitica.domain.TipoPagina;
import com.huamanga.tourism.analitica.service.RegistroVisitasService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Recogida de visitas (RF-52b).
 *
 * <p>Es publico porque tiene que serlo: la mayor parte del sitio se navega sin
 * cuenta (RF-34) y son justo esas visitas las que interesan. El abuso lo frenan
 * el rate limit general del Bloque 2 y la ventana anti-recarga del servicio.</p>
 *
 * <p>Responde <strong>204 siempre</strong>, tanto si la visita se conto como si
 * cayo dentro de la ventana. Devolver cosas distintas convertiria el endpoint en
 * un oraculo para averiguar si una huella ya habia visitado la seccion, que es
 * precisamente el tipo de dato que este modulo no quiere dar.</p>
 */
@RestController
@RequestMapping("/analitica")
@Tag(name = "Analitica", description = "Registro anonimo de visitas agregadas")
public class AnaliticaController {

    private final RegistroVisitasService registroVisitas;

    public AnaliticaController(RegistroVisitasService registroVisitas) {
        this.registroVisitas = registroVisitas;
    }

    @PostMapping("/visitas")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Anotar una visita a una seccion",
            description = """
                    No guarda ningun identificador de quien visita: la cuenta se
                    lleva por una huella HMAC efimera que vive solo en Redis.
                    Recargar la pagina dentro de 30 minutos no suma otra visita.
                    """)
    @ApiResponse(responseCode = "204", description = "Recibido (se contara o no, no se dice)")
    public void visita(@RequestParam TipoPagina tipo, HttpServletRequest peticion) {
        registroVisitas.registrar(tipo, peticion);
    }

    @PostMapping("/negocios/{negocioId}/{interaccion}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Anotar una interaccion con un negocio",
            description = "Operativo, pero sin datos hasta que el Bloque 11 cree el directorio.")
    public void negocio(@PathVariable UUID negocioId,
                        @PathVariable RegistroVisitasService.Interaccion interaccion,
                        HttpServletRequest peticion) {
        registroVisitas.registrarNegocio(negocioId, interaccion, peticion);
    }
}
