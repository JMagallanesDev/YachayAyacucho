package com.huamanga.tourism.lugar.evento;

import java.util.UUID;

/**
 * Los agregados de un lugar quedaron desfasados.
 *
 * <p>Lo publica cualquier operacion que cambie lo que la vista materializada
 * {@code estadistica_lugar} resume: crear, editar o borrar una reseña, y
 * tambien moderarla —ocultar una reseña de una estrella debe mover el promedio
 * con la misma rapidez que crearla—.</p>
 *
 * <p>No lleva el promedio nuevo, solo el aviso de que hay que recalcular. Quien
 * lo consume decide cuando hacerlo; ver {@code RefrescoEstadisticasListener}.</p>
 *
 * @param lugarId lugar afectado
 * @param slug    para poder revalidar tambien su pagina cacheada
 */
public record ContenidoCalificadoEvent(UUID lugarId, String slug) {
}
