package com.huamanga.tourism.geografia.controller;

import com.huamanga.tourism.geografia.repository.DistritoRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * Catalogo de distritos de la region Ayacucho.
 *
 * <p>Existe porque los formularios de alta —de lugares y, desde el Bloque 9, de
 * eventos— necesitan un desplegable de distritos. Es un catalogo estable de 119
 * filas que no cambia nunca; ni siquiera pagina.</p>
 */
@RestController
@RequestMapping("/distritos")
@Tag(name = "Geografia", description = "Catalogo de provincias y distritos")
public class DistritoController {

    private final DistritoRepository distritoRepository;

    public DistritoController(DistritoRepository distritoRepository) {
        this.distritoRepository = distritoRepository;
    }

    @GetMapping
    @Operation(summary = "Distritos de Ayacucho",
            description = "Los 119 distritos con su provincia, ordenados por nombre.")
    @Transactional(readOnly = true)
    public List<DistritoResponse> listar() {
        return distritoRepository.findAll(Sort.by("nombre")).stream()
                .map(distrito -> new DistritoResponse(
                        distrito.getId(),
                        distrito.getCodigo(),
                        distrito.getNombre(),
                        distrito.getProvincia().getNombre()))
                .toList();
    }

    @Schema(description = "Distrito con su provincia")
    public record DistritoResponse(UUID id, String codigo, String nombre, String provincia) {
    }
}
