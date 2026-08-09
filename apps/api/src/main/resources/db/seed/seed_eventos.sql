-- ============================================================
--  Agenda cultural (RF-79, RF-84, RF-86) — Bloque 9
-- ============================================================
--  Dos grupos de eventos, y conviene no confundirlos:
--
--  A) FESTIVIDADES REALES de Ayacucho, con fecha fija en el archivo.
--     >>> LAS FECHAS DEBEN VERIFICARSE CONTRA FUENTES OFICIALES <<<
--     antes de la sustentacion. La Semana Santa y el Carnaval son
--     festividades MOVILES —van atadas a la Pascua— y cambian cada anio;
--     las de aqui son las que corresponden segun el calendario liturgico,
--     pero el programa concreto lo publica cada anio el Arzobispado y la
--     Municipalidad, y puede empezar uno o dos dias antes.
--
--  B) DOS EVENTOS ANCLADOS a CURRENT_DATE, marcados como tales en su
--     descripcion. No son festividades reales: existen para que la
--     aplicacion siempre tenga un evento dentro de la ventana de
--     pronostico (5 dias) y otro de varios dias en curso, de modo que
--     ambas ramas del RF-88 se puedan demostrar cualquier dia del anio y
--     no solo la semana en que se escribio este archivo.
--
--  Idempotente: se puede ejecutar N veces sin duplicar nada.
-- ============================================================

BEGIN;

-- ------------------------------------------------------------
--  A. Festividades reales, con fecha fija
-- ------------------------------------------------------------
--  La Semana Santa se siembra en su edicion de 2026 (ya pasada) a
--  proposito: es el material con el que se demuestra el clonado anual
--  (RF-86), que crea la edicion siguiente sin copiar la fecha vieja.
INSERT INTO evento (id, lugar_id, distrito_id, tipo, fecha_inicio, fecha_fin,
                    recurrente_anual, estado, created_by)
SELECT uuid_generar_v7(),
       (SELECT id FROM lugar WHERE slug = s.lugar_slug),
       d.id, s.tipo, s.inicio::date, s.fin::date, s.recurrente, 'PUBLICADO',
       (SELECT id FROM usuario WHERE email = 'admin@yachay-ayacucho.pe')
FROM (VALUES
    ('semana-santa-ayacucho-2026', 'catedral-de-ayacucho', '050101', 'RELIGIOSO',
     '2026-03-27', '2026-04-05', TRUE),
    ('carnaval-ayacuchano-2027',   'plaza-mayor-de-huamanga', '050101', 'CULTURAL',
     '2027-02-06', '2027-02-09', TRUE),
    ('batalla-de-ayacucho-2026',   'plaza-mayor-de-huamanga', '050101', 'CIVICO',
     '2026-12-09', '2026-12-09', TRUE)
) AS s(clave, lugar_slug, dist_codigo, tipo, inicio, fin, recurrente)
JOIN distrito d ON d.codigo = s.dist_codigo
WHERE NOT EXISTS (
    SELECT 1 FROM evento_traduccion et
    WHERE et.idioma = 'es' AND et.nombre = CASE s.clave
        WHEN 'semana-santa-ayacucho-2026' THEN 'Semana Santa de Ayacucho'
        WHEN 'carnaval-ayacuchano-2027'   THEN 'Carnaval Ayacuchano'
        ELSE 'Aniversario de la Batalla de Ayacucho'
    END
);

-- ------------------------------------------------------------
--  B. Eventos anclados al dia de hoy (datos de demostracion)
-- ------------------------------------------------------------
--  El primero cae dentro de la ventana de pronostico; el segundo dura
--  cinco dias y sirve para ver una fiesta repartida por el calendario.
--  Ninguno es recurrente: clonarlos debe fallar, y esa es justamente la
--  regla que protege el calendario de fantasmas.
INSERT INTO evento (id, lugar_id, distrito_id, tipo, fecha_inicio, fecha_fin,
                    recurrente_anual, estado, created_by)
SELECT uuid_generar_v7(),
       (SELECT id FROM lugar WHERE slug = s.lugar_slug),
       d.id, s.tipo, CURRENT_DATE + s.desde, CURRENT_DATE + s.hasta, FALSE, 'PUBLICADO',
       (SELECT id FROM usuario WHERE email = 'admin@yachay-ayacucho.pe')
FROM (VALUES
    ('feria-santa-ana',    'barrio-artesanal-santa-ana', '050104', 'ARTESANAL', 2, 2),
    ('festival-retablo',   'plaza-mayor-de-huamanga',    '050101', 'CULTURAL',  3, 7)
) AS s(clave, lugar_slug, dist_codigo, tipo, desde, hasta)
JOIN distrito d ON d.codigo = s.dist_codigo
WHERE NOT EXISTS (
    SELECT 1 FROM evento_traduccion et
    WHERE et.idioma = 'es' AND et.nombre = CASE s.clave
        WHEN 'feria-santa-ana' THEN 'Feria de artesania de Santa Ana'
        ELSE 'Festival del retablo ayacuchano'
    END
);

-- ------------------------------------------------------------
--  Traducciones es/en
-- ------------------------------------------------------------
--  Se enganchan por la fecha de inicio y el tipo, que juntos identifican
--  a cada evento sembrado sin necesidad de guardar su UUID.
INSERT INTO evento_traduccion (evento_id, idioma, nombre, descripcion, organizador)
SELECT e.id, t.idioma, t.nombre, t.descripcion, t.organizador
FROM (VALUES
    ('2026-03-27', 'RELIGIOSO', 'es', 'Semana Santa de Ayacucho',
     'La celebracion religiosa mas importante del pais despues de la de Sevilla. Diez dias de procesiones que recorren el centro historico, con alfombras de flores, la salida del Senor de la Resurreccion al amanecer del domingo y la ciudad entera volcada en la calle. Las fechas cambian cada anio porque dependen de la Pascua.',
     'Arzobispado de Ayacucho'),
    ('2026-03-27', 'RELIGIOSO', 'en', 'Holy Week in Ayacucho',
     'The most important religious celebration in Peru. Ten days of processions through the historic centre, with flower carpets, the dawn procession of the Risen Christ on Sunday and the whole city out on the streets. Dates change every year because they follow Easter.',
     'Archdiocese of Ayacucho'),

    ('2027-02-06', 'CULTURAL', 'es', 'Carnaval Ayacuchano',
     'Comparsas, pandillas y contrapunto de coplas en quechua por las calles de Huamanga. Se celebra en los dias previos al miercoles de ceniza, asi que la fecha se mueve cada anio.',
     'Municipalidad Provincial de Huamanga'),
    ('2027-02-06', 'CULTURAL', 'en', 'Ayacucho Carnival',
     'Troupes, parades and Quechua verse duels through the streets of Huamanga. Held in the days before Ash Wednesday, so the date shifts every year.',
     'Provincial Municipality of Huamanga'),

    ('2026-12-09', 'CIVICO', 'es', 'Aniversario de la Batalla de Ayacucho',
     'Conmemoracion de la batalla del 9 de diciembre de 1824, que sello la independencia del Peru y de America del Sur. Desfile civico militar y romeria al santuario historico de la Pampa de Quinua.',
     'Gobierno Regional de Ayacucho'),
    ('2026-12-09', 'CIVICO', 'en', 'Anniversary of the Battle of Ayacucho',
     'Commemoration of the 9 December 1824 battle that sealed the independence of Peru and South America. Civic and military parade, and pilgrimage to the historic sanctuary at Pampa de Quinua.',
     'Regional Government of Ayacucho')
) AS t(inicio, tipo, idioma, nombre, descripcion, organizador)
JOIN evento e ON e.fecha_inicio = t.inicio::date AND e.tipo = t.tipo
WHERE NOT EXISTS (
    SELECT 1 FROM evento_traduccion et
    WHERE et.evento_id = e.id AND et.idioma = t.idioma
);

-- Traducciones de los dos eventos anclados. Se enganchan por el desfase
-- respecto a hoy, que es lo unico estable en ellos.
INSERT INTO evento_traduccion (evento_id, idioma, nombre, descripcion, organizador)
SELECT e.id, t.idioma, t.nombre, t.descripcion, t.organizador
FROM (VALUES
    (2, 'es', 'Feria de artesania de Santa Ana',
     'DATO DE DEMOSTRACION. Feria de retablos, ceramica de Quinua y tejidos en el barrio artesanal de Santa Ana.',
     'Asociacion de Artesanos de Santa Ana'),
    (2, 'en', 'Santa Ana craft fair',
     'DEMO DATA. Fair of retablos, Quinua pottery and textiles in the Santa Ana craft quarter.',
     'Santa Ana Artisans Association'),
    (3, 'es', 'Festival del retablo ayacuchano',
     'DATO DE DEMOSTRACION. Cinco dias de talleres abiertos, exhibicion y venta directa de retablos en la Plaza Mayor.',
     'Direccion Desconcentrada de Cultura'),
    (3, 'en', 'Ayacucho retablo festival',
     'DEMO DATA. Five days of open workshops, exhibition and direct sale of retablos in the Plaza Mayor.',
     'Regional Directorate of Culture')
) AS t(desfase, idioma, nombre, descripcion, organizador)
JOIN evento e ON e.fecha_inicio = CURRENT_DATE + t.desfase
WHERE NOT EXISTS (
    SELECT 1 FROM evento_traduccion et
    WHERE et.evento_id = e.id AND et.idioma = t.idioma
);

COMMIT;
