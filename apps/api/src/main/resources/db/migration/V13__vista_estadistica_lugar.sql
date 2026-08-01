-- ============================================================
--  V13 - Vista materializada EstadisticaLugar (seccion 6.3)
-- ============================================================
--  Por que una vista materializada y no columnas en lugar:
--  guardar calificacion_promedio o total_visitas dentro de lugar seria
--  un atributo derivado y violaria 3FN (dependencia transitiva). La
--  vista es un objeto independiente que se recalcula, no un dato
--  duplicado que haya que mantener sincronizado con triggers.
--
--  La calificacion promedio de un lugar se lee SIEMPRE de aqui. No
--  existe trigger ni columna de promedio en lugar.
--
--  Se refresca cada 5 minutos con REFRESH MATERIALIZED VIEW CONCURRENTLY
--  desde Spring Scheduler con lock ShedLock (EstadisticaLugarJob).
-- ============================================================

CREATE MATERIALIZED VIEW estadistica_lugar AS
SELECT
    l.id                                                    AS lugar_id,
    COALESCE(r.calificacion_promedio, 0)::NUMERIC(3, 2)     AS calificacion_promedio,
    COALESCE(r.total_resenas, 0)                            AS total_resenas,
    COALESCE(c.total_visitas, 0)                            AS total_visitas,
    COALESCE(f.total_favoritos, 0)                          AS total_favoritos,
    NOW()                                                   AS actualizado_en
FROM lugar l
LEFT JOIN (
    -- Solo las resenas publicadas cuentan: una resena oculta por
    -- moderacion no debe mover el promedio publico.
    SELECT lugar_id,
           AVG(calificacion)  AS calificacion_promedio,
           COUNT(*)           AS total_resenas
    FROM resena
    WHERE estado = 'PUBLICADA'
    GROUP BY lugar_id
) r ON r.lugar_id = l.id
LEFT JOIN (
    SELECT lugar_id, COUNT(*) AS total_visitas
    FROM check_in
    GROUP BY lugar_id
) c ON c.lugar_id = l.id
LEFT JOIN (
    SELECT lugar_id, COUNT(*) AS total_favoritos
    FROM favorito
    GROUP BY lugar_id
) f ON f.lugar_id = l.id
WHERE l.deleted_at IS NULL;

-- OBLIGATORIO: sin un indice unico, PostgreSQL rechaza el REFRESH
-- ... CONCURRENTLY y habria que bloquear la vista entera en cada
-- refresco, dejando el ranking inaccesible durante la operacion.
CREATE UNIQUE INDEX idx_estadistica_lugar_pk ON estadistica_lugar (lugar_id);

-- Rankings "mejor valorados" y "mas visitados" (RF-06).
CREATE INDEX idx_estadistica_calificacion ON estadistica_lugar (calificacion_promedio DESC);
CREATE INDEX idx_estadistica_visitas ON estadistica_lugar (total_visitas DESC);

COMMENT ON MATERIALIZED VIEW estadistica_lugar IS
    'Agregados de resenas, check-ins y favoritos por lugar. Fuente unica de la calificacion promedio (RF-06).';
