-- ============================================================
--  Rutas tematicas (RF-20) — Bloque 5
-- ============================================================
--  El seed de demostracion trae una sola ruta con tres paradas, que no
--  basta para ver el requisito funcionando. Aqui se anaden dos mas y se
--  amplia la colonial, usando los 15 lugares ya cargados.
--
--  Idempotente, como los demas seeds: se puede ejecutar N veces.
--
--  Las paradas van EN ORDEN DE RECORRIDO. Ese orden es el dato con
--  valor: una ruta cultural es una secuencia, no un conjunto, y unir los
--  puntos en otro orden dibujaria un garabato sobre la ciudad.
-- ============================================================

BEGIN;

-- ------------------------------------------------------------
--  1. Cabeceras de las dos rutas nuevas
-- ------------------------------------------------------------
INSERT INTO ruta_tematica (id, slug, color_hex, icono, activa, orden)
SELECT uuid_generar_v7(), s.slug, s.color, s.icono, TRUE, s.orden
FROM (VALUES
    ('ruta-memoria-y-museos', '#24406E', 'landmark',  2::smallint),
    ('ruta-miradores-y-naturaleza', '#46704F', 'mountain', 3::smallint)
) AS s(slug, color, icono, orden)
WHERE NOT EXISTS (SELECT 1 FROM ruta_tematica r WHERE r.slug = s.slug);

INSERT INTO ruta_traduccion (ruta_tematica_id, idioma, nombre, descripcion)
SELECT r.id, t.idioma, t.nombre, t.descripcion
FROM (VALUES
    ('ruta-memoria-y-museos', 'es', 'Ruta de la memoria y los museos',
     'Del museo arqueologico a la memoria del conflicto armado interno, un recorrido por como Ayacucho guarda y cuenta su historia.'),
    ('ruta-memoria-y-museos', 'en', 'Memory and museums route',
     'From the archaeological museum to the memory of the internal armed conflict: how Ayacucho keeps and tells its history.'),
    ('ruta-miradores-y-naturaleza', 'es', 'Ruta de miradores y naturaleza',
     'Las alturas que rodean Huamanga, desde los miradores del centro hasta el bosque de puyas de Titankayocc.'),
    ('ruta-miradores-y-naturaleza', 'en', 'Viewpoints and nature route',
     'The heights around Huamanga, from the viewpoints above the centre to the Titankayocc puya forest.')
) AS t(slug, idioma, nombre, descripcion)
JOIN ruta_tematica r ON r.slug = t.slug
WHERE NOT EXISTS (
    SELECT 1 FROM ruta_traduccion rt
    WHERE rt.ruta_tematica_id = r.id AND rt.idioma = t.idioma
);

-- ------------------------------------------------------------
--  2. Paradas
-- ------------------------------------------------------------
--  La ruta colonial ya existia con 3 paradas; se completa hasta 6.
--  El orden sigue el recorrido natural a pie desde la Plaza Mayor y
--  TERMINA en el mirador: se sube al final, para el atardecer.
INSERT INTO lugar_ruta (ruta_tematica_id, lugar_id, orden)
SELECT r.id, l.id, s.orden
FROM (VALUES
    ('ruta-colonial-huamanga', 'plaza-mayor-de-huamanga', 0::smallint),
    ('ruta-colonial-huamanga', 'templo-de-san-francisco', 2::smallint),
    ('ruta-colonial-huamanga', 'templo-de-la-merced',     3::smallint),

    -- Memoria y museos
    ('ruta-memoria-y-museos', 'museo-hipolito-unanue',      1::smallint),
    ('ruta-memoria-y-museos', 'museo-memoria-anfasep',      2::smallint),
    ('ruta-memoria-y-museos', 'complejo-arqueologico-wari', 3::smallint),
    ('ruta-memoria-y-museos', 'santuario-pampa-de-quinua',  4::smallint),

    -- Miradores y naturaleza
    ('ruta-miradores-y-naturaleza', 'mirador-carmen-alto',          1::smallint),
    ('ruta-miradores-y-naturaleza', 'mirador-de-acuchimay',         2::smallint),
    ('ruta-miradores-y-naturaleza', 'bosque-de-puyas-titankayocc',  3::smallint)
) AS s(ruta, slug, orden)
JOIN ruta_tematica r ON r.slug = s.ruta
JOIN lugar l ON l.slug = s.slug AND l.deleted_at IS NULL
WHERE NOT EXISTS (
    SELECT 1 FROM lugar_ruta lr WHERE lr.ruta_tematica_id = r.id AND lr.lugar_id = l.id
);

-- ------------------------------------------------------------
--  3. Reordenar las paradas heredadas del seed de demostracion
-- ------------------------------------------------------------
--  El INSERT de arriba es idempotente y por tanto NO toca las filas que
--  ya existian, que traian el mirador de Acuchimay en mitad del
--  recorrido. Sobre el mapa eso dibuja una polilinea que sube al cerro y
--  vuelve al centro para ver dos templos mas. Estas dos lineas colocan a
--  Santo Domingo y al mirador donde toca.
UPDATE lugar_ruta lr SET orden = v.orden
FROM (VALUES
    ('templo-de-santo-domingo', 4::smallint),
    ('mirador-de-acuchimay',    5::smallint)
) AS v(slug, orden)
WHERE lr.lugar_id = (SELECT id FROM lugar WHERE slug = v.slug)
  AND lr.ruta_tematica_id = (SELECT id FROM ruta_tematica WHERE slug = 'ruta-colonial-huamanga')
  AND lr.orden <> v.orden;

COMMIT;

\echo 'Seed de rutas tematicas aplicado.'
