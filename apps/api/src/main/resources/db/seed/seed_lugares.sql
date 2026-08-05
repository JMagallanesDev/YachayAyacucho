-- ============================================================
--  DATOS DE DEMOSTRACION - 10 lugares adicionales
-- ============================================================
--  Complementa a seed_demo.sql hasta llegar a 15 lugares, que es lo
--  minimo para que la paginacion, los filtros por categoria y los
--  rankings se puedan ver funcionando de verdad: con 5 registros todo
--  cabe en una pagina y ningun filtro cambia nada visible.
--
--  Coordenadas aproximadas dentro de los bounds de Ayacucho. Deben
--  verificarse en campo antes de la carga real del Bloque 14.
--
--  Idempotente: se puede lanzar las veces que haga falta.
-- ============================================================

BEGIN;

INSERT INTO lugar (id, slug, categoria_lugar_id, distrito_id, ubicacion, direccion,
                   precio_entrada_pen, duracion_visita_min, acepta_tarjeta, tiene_banos,
                   accesible_silla_ruedas, apto_ninos, costo_taxi_desde_plaza_pen,
                   requiere_guia, estado, created_by)
SELECT uuid_generar_v7(), s.slug, c.id, d.id,
       ST_SetSRID(ST_MakePoint(s.lon, s.lat), 4326), s.direccion,
       s.precio, s.duracion, s.tarjeta, s.banos, s.accesible, s.ninos, s.taxi, s.guia,
       'PUBLICADO',
       (SELECT id FROM usuario WHERE email = 'admin@yachay-ayacucho.pe')
FROM (VALUES
    ('templo-de-la-merced',        'IGLESIAS',             '050101', -74.2251, -13.1601,
     'Jiron 2 de Mayo, Huamanga',              0.00,  30::smallint, FALSE, FALSE, FALSE, TRUE,  5.00,  FALSE),
    ('templo-de-san-francisco',    'IGLESIAS',             '050101', -74.2229, -13.1604,
     'Jiron 28 de Julio, Huamanga',            0.00,  35::smallint, FALSE, TRUE,  TRUE,  TRUE,  5.00,  FALSE),
    ('museo-memoria-anfasep',      'MUSEOS',               '050101', -74.2189, -13.1520,
     'Prolongacion Libertad, Huamanga',        5.00,  60::smallint, FALSE, TRUE,  TRUE,  FALSE, 8.00,  TRUE),
    ('museo-hipolito-unanue',      'MUSEOS',               '050101', -74.2205, -13.1547,
     'Independencia 502, Huamanga',            4.00,  50::smallint, TRUE,  TRUE,  TRUE,  TRUE,  7.00,  FALSE),
    ('mirador-carmen-alto',        'MIRADORES',            '050104', -74.2298, -13.1725,
     'Carmen Alto, Huamanga',                  0.00,  30::smallint, FALSE, FALSE, FALSE, TRUE,  9.00,  FALSE),
    ('plaza-mayor-de-huamanga',    'PLAZAS',               '050101', -74.2240, -13.1592,
     'Plaza Mayor, Huamanga',                  0.00,  25::smallint, FALSE, FALSE, TRUE,  TRUE,  0.00,  FALSE),
    ('barrio-artesanal-santa-ana', 'ARTESANIA',            '050104', -74.2276, -13.1698,
     'Santa Ana, Carmen Alto',                 0.00,  90::smallint, TRUE,  TRUE,  FALSE, TRUE,  8.00,  FALSE),
    ('mercado-12-de-abril',        'GASTRONOMIA',          '050101', -74.2262, -13.1571,
     'Jiron Chorro, Huamanga',                 0.00,  60::smallint, FALSE, TRUE,  FALSE, TRUE,  5.00,  FALSE),
    ('intihuatana-de-vilcashuaman','SITIOS_ARQUEOLOGICOS', '051101', -73.9500, -13.6500,
     'Vilcas Huaman',                          8.00, 120::smallint, FALSE, TRUE,  FALSE, TRUE, 60.00,  TRUE),
    ('bosque-de-puyas-titankayocc','NATURALEZA',           '050114', -74.4200, -13.3100,
     'Vinchos, carretera a Huancavelica',     10.00, 180::smallint, FALSE, FALSE, FALSE, FALSE, 90.00, TRUE)
) AS s(slug, cat_codigo, dist_codigo, lon, lat, direccion, precio, duracion,
       tarjeta, banos, accesible, ninos, taxi, guia)
JOIN categoria_lugar c ON c.codigo = s.cat_codigo
JOIN distrito d ON d.codigo = s.dist_codigo
WHERE NOT EXISTS (SELECT 1 FROM lugar WHERE slug = s.slug);

-- ------------------------------------------------------------
--  Traducciones es/en
-- ------------------------------------------------------------
INSERT INTO lugar_traduccion (lugar_id, idioma, nombre, descripcion, historia)
SELECT l.id, t.idioma, t.nombre, t.descripcion, t.historia
FROM (VALUES
    ('templo-de-la-merced', 'es', 'Templo de La Merced',
     'El segundo templo mas antiguo de la ciudad.',
     'Levantado hacia 1540 por la orden mercedaria, conserva un retablo mayor de estilo barroco y una portada de piedra labrada.'),
    ('templo-de-la-merced', 'en', 'La Merced Temple',
     'The second oldest temple in the city.',
     'Raised around 1540 by the Mercedarian order, it preserves a baroque main altarpiece and a carved stone doorway.'),
    ('templo-de-san-francisco', 'es', 'Templo de San Francisco de Asis',
     'Iglesia franciscana con retablos dorados.',
     'Construida en el siglo XVI, alberga uno de los conjuntos de retablos en pan de oro mejor conservados de la ciudad.'),
    ('templo-de-san-francisco', 'en', 'San Francisco de Asis Temple',
     'Franciscan church with gilded altarpieces.',
     'Built in the sixteenth century, it houses one of the best preserved sets of gold-leaf altarpieces in the city.'),
    ('museo-memoria-anfasep', 'es', 'Museo de la Memoria ANFASEP',
     'Memoria de las victimas del conflicto armado interno.',
     'Creado por la Asociacion Nacional de Familiares de Secuestrados, Detenidos y Desaparecidos del Peru, reune testimonios y objetos de las familias afectadas entre 1980 y 2000.'),
    ('museo-memoria-anfasep', 'en', 'ANFASEP Memory Museum',
     'Memory of the victims of the internal armed conflict.',
     'Created by the National Association of Relatives of the Kidnapped, Detained and Disappeared of Peru, it gathers testimonies and belongings of the families affected between 1980 and 2000.'),
    ('museo-hipolito-unanue', 'es', 'Museo Arqueologico Hipolito Unanue',
     'Coleccion arqueologica de las culturas Wari y Chanka.',
     'Reune ceramica, textiles y liticos de las culturas que ocuparon el valle antes de la llegada de los incas.'),
    ('museo-hipolito-unanue', 'en', 'Hipolito Unanue Archaeological Museum',
     'Archaeological collection of the Wari and Chanka cultures.',
     'It gathers pottery, textiles and stonework from the cultures that occupied the valley before the arrival of the Incas.'),
    ('mirador-carmen-alto', 'es', 'Mirador de Carmen Alto',
     'Vista panoramica del valle y de los tejados coloniales.',
     'Desde este balcon natural se aprecia el trazado completo del centro historico y, en dias despejados, la cordillera al fondo.'),
    ('mirador-carmen-alto', 'en', 'Carmen Alto Viewpoint',
     'Panoramic view of the valley and the colonial rooftops.',
     'From this natural balcony you can see the full layout of the historic centre and, on clear days, the mountain range beyond.'),
    ('plaza-mayor-de-huamanga', 'es', 'Plaza Mayor de Huamanga',
     'El corazon de la ciudad, rodeado de portales coloniales.',
     'Trazada en 1540 siguiendo el damero espanol, sus cuatro portales albergan la catedral, la municipalidad y las casonas de las familias fundadoras.'),
    ('plaza-mayor-de-huamanga', 'en', 'Huamanga Main Square',
     'The heart of the city, surrounded by colonial arcades.',
     'Laid out in 1540 following the Spanish grid, its four arcades house the cathedral, the town hall and the mansions of the founding families.'),
    ('barrio-artesanal-santa-ana', 'es', 'Barrio artesanal de Santa Ana',
     'Talleres de retablos, tallado en piedra de Huamanga y textiles.',
     'Aqui trabajan las familias que mantienen vivas las tecnicas del retablo ayacuchano y el tallado en piedra de Huamanga, reconocidas como patrimonio cultural de la nacion.'),
    ('barrio-artesanal-santa-ana', 'en', 'Santa Ana Artisan Quarter',
     'Workshops of retablos, Huamanga stone carving and textiles.',
     'Here work the families who keep alive the techniques of the Ayacucho retablo and Huamanga stone carving, recognised as cultural heritage of the nation.'),
    ('mercado-12-de-abril', 'es', 'Mercado 12 de Abril',
     'Mercado tradicional y puestos de comida ayacuchana.',
     'Punto de encuentro de la ciudad desde mediados del siglo XX, es donde se prueban el puca picante, el qapchi y el mondongo ayacuchano.'),
    ('mercado-12-de-abril', 'en', '12 de Abril Market',
     'Traditional market and Ayacucho food stalls.',
     'A meeting point for the city since the mid twentieth century, this is where you try puca picante, qapchi and Ayacucho mondongo.'),
    ('intihuatana-de-vilcashuaman', 'es', 'Intihuatana de Vilcashuaman',
     'Complejo inca con piramide ceremonial y bano del inca.',
     'Vilcashuaman fue centro administrativo inca en el cruce del Camino Real. Conserva el ushnu piramidal y el llamado bano del inca, tallado en un solo bloque de granito.'),
    ('intihuatana-de-vilcashuaman', 'en', 'Vilcashuaman Intihuatana',
     'Inca complex with ceremonial pyramid and the Inca bath.',
     'Vilcashuaman was an Inca administrative centre at the crossing of the Royal Road. It preserves the pyramidal ushnu and the so-called Inca bath, carved from a single block of granite.'),
    ('bosque-de-puyas-titankayocc', 'es', 'Bosque de Puyas de Titankayocc',
     'El mayor bosque de Puya Raimondi del mundo.',
     'Mas de 250 000 ejemplares de Puya raimondii, la bromelia mas grande del planeta, que florece una sola vez tras decadas de vida y despues muere.'),
    ('bosque-de-puyas-titankayocc', 'en', 'Titankayocc Puya Forest',
     'The largest Puya Raimondi forest in the world.',
     'More than 250,000 specimens of Puya raimondii, the largest bromeliad on the planet, which flowers only once after decades of life and then dies.')
) AS t(slug, idioma, nombre, descripcion, historia)
JOIN lugar l ON l.slug = t.slug
WHERE NOT EXISTS (
    SELECT 1 FROM lugar_traduccion lt WHERE lt.lugar_id = l.id AND lt.idioma = t.idioma
);

-- ------------------------------------------------------------
--  Horarios variados, para que el badge abierto/cerrado tenga algo
--  que decir a distintas horas del dia
-- ------------------------------------------------------------

-- Templos: manana y tarde, con cierre al mediodia. Domingo cerrado.
INSERT INTO horario_lugar (id, lugar_id, dia_semana, hora_apertura, hora_cierre, cerrado)
SELECT uuid_generar_v7(), l.id, d.dia, h.apertura, h.cierre, FALSE
FROM lugar l
CROSS JOIN generate_series(1, 6) AS d(dia)
CROSS JOIN (VALUES ('09:00'::time, '12:30'::time), ('15:00'::time, '18:00'::time)) AS h(apertura, cierre)
WHERE l.slug IN ('templo-de-la-merced', 'templo-de-san-francisco')
  AND NOT EXISTS (SELECT 1 FROM horario_lugar hl WHERE hl.lugar_id = l.id);

INSERT INTO horario_lugar (id, lugar_id, dia_semana, cerrado)
SELECT uuid_generar_v7(), l.id, 0, TRUE
FROM lugar l
WHERE l.slug IN ('templo-de-la-merced', 'templo-de-san-francisco')
  AND NOT EXISTS (SELECT 1 FROM horario_lugar hl WHERE hl.lugar_id = l.id AND hl.dia_semana = 0);

-- Museos: martes a domingo, jornada continua. Lunes cerrado.
INSERT INTO horario_lugar (id, lugar_id, dia_semana, hora_apertura, hora_cierre, cerrado)
SELECT uuid_generar_v7(), l.id, d.dia, '09:00'::time, '17:00'::time, FALSE
FROM lugar l
CROSS JOIN generate_series(2, 6) AS d(dia)
WHERE l.slug IN ('museo-memoria-anfasep', 'museo-hipolito-unanue')
  AND NOT EXISTS (SELECT 1 FROM horario_lugar hl WHERE hl.lugar_id = l.id);

INSERT INTO horario_lugar (id, lugar_id, dia_semana, hora_apertura, hora_cierre, cerrado)
SELECT uuid_generar_v7(), l.id, 0, '09:00'::time, '13:00'::time, FALSE
FROM lugar l
WHERE l.slug IN ('museo-memoria-anfasep', 'museo-hipolito-unanue')
  AND NOT EXISTS (SELECT 1 FROM horario_lugar hl WHERE hl.lugar_id = l.id AND hl.dia_semana = 0);

INSERT INTO horario_lugar (id, lugar_id, dia_semana, cerrado)
SELECT uuid_generar_v7(), l.id, 1, TRUE
FROM lugar l
WHERE l.slug IN ('museo-memoria-anfasep', 'museo-hipolito-unanue')
  AND NOT EXISTS (SELECT 1 FROM horario_lugar hl WHERE hl.lugar_id = l.id AND hl.dia_semana = 1);

-- Espacios abiertos: todos los dias, horario amplio.
INSERT INTO horario_lugar (id, lugar_id, dia_semana, hora_apertura, hora_cierre, cerrado)
SELECT uuid_generar_v7(), l.id, d.dia, '06:00'::time, '20:00'::time, FALSE
FROM lugar l
CROSS JOIN generate_series(0, 6) AS d(dia)
WHERE l.slug IN ('mirador-carmen-alto', 'plaza-mayor-de-huamanga', 'mercado-12-de-abril',
                 'barrio-artesanal-santa-ana', 'intihuatana-de-vilcashuaman',
                 'bosque-de-puyas-titankayocc')
  AND NOT EXISTS (SELECT 1 FROM horario_lugar hl WHERE hl.lugar_id = l.id);

-- Los rankings leen de la vista materializada; sin refrescar saldrian a cero.
REFRESH MATERIALIZED VIEW estadistica_lugar;

COMMIT;

\echo 'Seed de lugares aplicado.'
