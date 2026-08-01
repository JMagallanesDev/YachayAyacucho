-- ============================================================
--  DATOS DE DEMOSTRACION - desechables, NUNCA en produccion
-- ============================================================
--  Se ejecuta con:  pnpm db:seed
--
--  Deliberadamente FUERA de las migraciones Flyway. Los catalogos de la
--  V14 (roles, provincias, categorias, insignias) son parte del contrato
--  del esquema y deben existir tambien en produccion; esto de aqui es
--  material de prueba y se reconstruye cuando haga falta.
--
--  Es idempotente: se puede lanzar varias veces sin duplicar nada.
-- ============================================================

BEGIN;

-- ------------------------------------------------------------
--  Usuario administrador
-- ------------------------------------------------------------
--  ATENCION: password_hash NO es un hash valido y por tanto NO permite
--  iniciar sesion. Es deliberado: BCrypt llega con Spring Security en el
--  Bloque 2, que sustituira este valor por un hash real de coste 12.
--  Hasta entonces no existe login, asi que la cuenta no hace falta que
--  sea usable, y dejar aqui una contrasena de verdad seria una mala
--  costumbre que acabaria copiandose a produccion.
-- ------------------------------------------------------------
INSERT INTO usuario (id, email, password_hash, nombre, rol_id, estado)
SELECT uuid_generar_v7(),
       'admin@yachay-ayacucho.pe',
       'SIN_HASH_VALIDO_HASTA_BLOQUE_2',
       'Administrador',
       r.id,
       'ACTIVO'
FROM rol r
WHERE r.nombre = 'ADMIN'
  AND NOT EXISTS (SELECT 1 FROM usuario WHERE email = 'admin@yachay-ayacucho.pe');

-- ------------------------------------------------------------
--  5 lugares patrimoniales de Huamanga
-- ------------------------------------------------------------
--  Coordenadas aproximadas dentro de los bounds de Ayacucho que valida
--  el CHECK de la tabla. Deben verificarse en campo antes de la carga
--  real del Bloque 14.
-- ------------------------------------------------------------
INSERT INTO lugar (id, slug, categoria_lugar_id, distrito_id, ubicacion, direccion,
                   precio_entrada_pen, duracion_visita_min, tiene_banos,
                   accesible_silla_ruedas, apto_ninos, estado, created_by)
SELECT uuid_generar_v7(),
       s.slug,
       c.id,
       d.id,
       ST_SetSRID(ST_MakePoint(s.lon, s.lat), 4326),
       s.direccion,
       s.precio,
       s.duracion,
       s.banos,
       s.accesible,
       TRUE,
       'PUBLICADO',
       (SELECT id FROM usuario WHERE email = 'admin@yachay-ayacucho.pe')
FROM (VALUES
    ('catedral-de-ayacucho',        'IGLESIAS',              '050101', -74.2236, -13.1588,
     'Plaza Mayor de Huamanga',                  0.00,  45::smallint, TRUE,  TRUE),
    ('templo-de-santo-domingo',     'IGLESIAS',              '050101', -74.2246, -13.1570,
     'Jiron 9 de Diciembre, Huamanga',           0.00,  30::smallint, FALSE, FALSE),
    ('mirador-de-acuchimay',        'MIRADORES',             '050101', -74.2264, -13.1691,
     'Cerro Acuchimay, Carmen Alto',             0.00,  40::smallint, TRUE,  FALSE),
    ('complejo-arqueologico-wari',  'SITIOS_ARQUEOLOGICOS',  '050108', -74.1861, -13.0470,
     'Carretera Ayacucho-Quinua km 22',          5.00, 120::smallint, TRUE,  FALSE),
    ('santuario-pampa-de-quinua',   'SITIOS_ARQUEOLOGICOS',  '050108', -74.1372, -13.0473,
     'Pampa de la Quinua, distrito de Quinua',   3.00,  90::smallint, TRUE,  TRUE)
) AS s(slug, cat_codigo, dist_codigo, lon, lat, direccion, precio, duracion, banos, accesible)
JOIN categoria_lugar c ON c.codigo = s.cat_codigo
JOIN distrito d ON d.codigo = s.dist_codigo
WHERE NOT EXISTS (SELECT 1 FROM lugar WHERE slug = s.slug);

-- ------------------------------------------------------------
--  Traducciones es/en de los 5 lugares
-- ------------------------------------------------------------
INSERT INTO lugar_traduccion (lugar_id, idioma, nombre, descripcion, historia)
SELECT l.id, t.idioma, t.nombre, t.descripcion, t.historia
FROM (VALUES
    ('catedral-de-ayacucho', 'es', 'Catedral Basilica de Santa Maria',
     'Templo principal de Huamanga, frente a la Plaza Mayor.',
     'Construida entre 1632 y 1672 en sillar y piedra volcanica, es el centro de las procesiones de Semana Santa, la festividad mas importante de la ciudad.'),
    ('catedral-de-ayacucho', 'en', 'Saint Mary Basilica Cathedral',
     'Main temple of Huamanga, facing the Main Square.',
     'Built between 1632 and 1672 in volcanic stone, it is the heart of the Holy Week processions, the city''s most important festivity.'),
    ('templo-de-santo-domingo', 'es', 'Templo de Santo Domingo',
     'Iglesia dominica del siglo XVI con fachada de tres arcos.',
     'Levantada en 1548, su espadana de tres arcos servia para las ejecuciones de la Inquisicion. Hoy es uno de los conjuntos coloniales mejor conservados de la ciudad.'),
    ('templo-de-santo-domingo', 'en', 'Santo Domingo Temple',
     'Sixteenth-century Dominican church with a three-arch facade.',
     'Raised in 1548, its three-arch belfry was once used for Inquisition executions. Today it is one of the best preserved colonial complexes in the city.'),
    ('mirador-de-acuchimay', 'es', 'Mirador de Acuchimay',
     'Balcon natural sobre Huamanga, coronado por el Cristo Blanco.',
     'Desde este cerro se domina el valle completo de Huamanga. El Cristo Redentor que lo corona fue erigido en 1990 y es punto de peregrinacion local.'),
    ('mirador-de-acuchimay', 'en', 'Acuchimay Viewpoint',
     'Natural balcony over Huamanga, crowned by the White Christ.',
     'This hill overlooks the entire Huamanga valley. The Christ the Redeemer statue was erected in 1990 and is a local pilgrimage site.'),
    ('complejo-arqueologico-wari', 'es', 'Complejo Arqueologico Wari',
     'Capital del primer imperio andino, anterior a los incas.',
     'Centro urbano de la cultura Wari entre los siglos VII y XII. Sus murallas de hasta 12 metros y su trazado ortogonal la convierten en la primera ciudad planificada de los Andes.'),
    ('complejo-arqueologico-wari', 'en', 'Wari Archaeological Complex',
     'Capital of the first Andean empire, predating the Incas.',
     'Urban centre of the Wari culture between the 7th and 12th centuries. Its walls of up to 12 metres and orthogonal layout make it the first planned city in the Andes.'),
    ('santuario-pampa-de-quinua', 'es', 'Santuario Historico Pampa de Ayacucho',
     'Escenario de la batalla que sello la independencia americana.',
     'El 9 de diciembre de 1824 se libro aqui la batalla de Ayacucho, que puso fin al dominio espanol en Sudamerica. El obelisco de 44 metros conmemora los anios de lucha.'),
    ('santuario-pampa-de-quinua', 'en', 'Pampa de Ayacucho Historic Sanctuary',
     'Site of the battle that sealed American independence.',
     'On 9 December 1824 the Battle of Ayacucho was fought here, ending Spanish rule in South America. The 44-metre obelisk commemorates the years of struggle.')
) AS t(slug, idioma, nombre, descripcion, historia)
JOIN lugar l ON l.slug = t.slug
WHERE NOT EXISTS (
    SELECT 1 FROM lugar_traduccion lt WHERE lt.lugar_id = l.id AND lt.idioma = t.idioma
);

-- ------------------------------------------------------------
--  Horarios: lunes a sabado, con cierre al mediodia en los templos
-- ------------------------------------------------------------
INSERT INTO horario_lugar (id, lugar_id, dia_semana, hora_apertura, hora_cierre, cerrado)
SELECT uuid_generar_v7(), l.id, d.dia, h.apertura, h.cierre, FALSE
FROM lugar l
CROSS JOIN generate_series(1, 6) AS d(dia)
CROSS JOIN (VALUES ('09:00'::time, '13:00'::time), ('15:00'::time, '18:00'::time)) AS h(apertura, cierre)
WHERE l.slug IN ('catedral-de-ayacucho', 'templo-de-santo-domingo')
  AND NOT EXISTS (SELECT 1 FROM horario_lugar hl WHERE hl.lugar_id = l.id);

INSERT INTO horario_lugar (id, lugar_id, dia_semana, hora_apertura, hora_cierre, cerrado)
SELECT uuid_generar_v7(), l.id, d.dia, '08:00'::time, '17:00'::time, FALSE
FROM lugar l
CROSS JOIN generate_series(0, 6) AS d(dia)
WHERE l.slug IN ('mirador-de-acuchimay', 'complejo-arqueologico-wari', 'santuario-pampa-de-quinua')
  AND NOT EXISTS (SELECT 1 FROM horario_lugar hl WHERE hl.lugar_id = l.id);

-- ------------------------------------------------------------
--  Una imagen historica con punto de captura ("Parate aqui", RF-11b)
-- ------------------------------------------------------------
INSERT INTO lugar_imagen_historica (id, lugar_id, titulo, url_historica, public_id_historica,
                                    anio_historico, credito_historico, punto_captura, orden)
SELECT uuid_generar_v7(), l.id,
       'La Catedral desde la Plaza Mayor',
       'https://res.cloudinary.com/demo/image/upload/v1/yachay/catedral-1920.jpg',
       'yachay/catedral-1920',
       1920::smallint,
       'Archivo historico regional (imagen de demostracion)',
       ST_SetSRID(ST_MakePoint(-74.2239, -13.1592), 4326)::geography,
       0::smallint
FROM lugar l
WHERE l.slug = 'catedral-de-ayacucho'
  AND NOT EXISTS (SELECT 1 FROM lugar_imagen_historica WHERE lugar_id = l.id);

-- ------------------------------------------------------------
--  Una ruta tematica que enlaza tres de los lugares
-- ------------------------------------------------------------
INSERT INTO ruta_tematica (id, slug, color_hex, icono, activa, orden)
SELECT uuid_generar_v7(), 'ruta-colonial-huamanga', '#B3202B', 'church', TRUE, 1::smallint
WHERE NOT EXISTS (SELECT 1 FROM ruta_tematica WHERE slug = 'ruta-colonial-huamanga');

INSERT INTO ruta_traduccion (ruta_tematica_id, idioma, nombre, descripcion)
SELECT r.id, t.idioma, t.nombre, t.descripcion
FROM (VALUES
    ('es', 'Ruta colonial de Huamanga', 'Recorrido a pie por los templos y miradores del centro historico.'),
    ('en', 'Colonial route of Huamanga', 'Walking tour through the temples and viewpoints of the historic centre.')
) AS t(idioma, nombre, descripcion)
JOIN ruta_tematica r ON r.slug = 'ruta-colonial-huamanga'
WHERE NOT EXISTS (
    SELECT 1 FROM ruta_traduccion rt WHERE rt.ruta_tematica_id = r.id AND rt.idioma = t.idioma
);

INSERT INTO lugar_ruta (ruta_tematica_id, lugar_id, orden)
SELECT r.id, l.id, s.orden
FROM (VALUES
    ('catedral-de-ayacucho', 1::smallint),
    ('templo-de-santo-domingo', 2::smallint),
    ('mirador-de-acuchimay', 3::smallint)
) AS s(slug, orden)
JOIN lugar l ON l.slug = s.slug
JOIN ruta_tematica r ON r.slug = 'ruta-colonial-huamanga'
WHERE NOT EXISTS (
    SELECT 1 FROM lugar_ruta lr WHERE lr.ruta_tematica_id = r.id AND lr.lugar_id = l.id
);

-- Deja los agregados coherentes con los datos recien insertados.
REFRESH MATERIALIZED VIEW estadistica_lugar;

COMMIT;

\echo 'Seed de demostracion aplicado.'
