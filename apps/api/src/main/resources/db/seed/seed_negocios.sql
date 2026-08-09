-- ============================================================
--  Directorio de negocios e historia visual — Bloque 11
-- ============================================================
--  DATOS DE DEMOSTRACION. Los tres negocios NO son reales: llevan
--  «DATO DE DEMOSTRACION» en su descripcion y un numero de WhatsApp de
--  la franja reservada para documentacion. Existen para que el
--  directorio, el boton de contacto y la analitica por negocio se puedan
--  ensenar; los negocios de verdad se dan de alta desde la aplicacion y
--  los aprueba un administrador.
--
--  Uno de los tres queda PENDIENTE a proposito: es la forma de
--  comprobar de un vistazo que lo pendiente no se cuela en el listado
--  publico (RF-105).
--
--  Idempotente: se puede ejecutar N veces sin duplicar nada.
-- ============================================================

BEGIN;

-- ------------------------------------------------------------
--  1. Un dueno de negocio de demostracion
-- ------------------------------------------------------------
--  Contrasena INUTILIZABLE, como el resto de cuentas sembradas: es una
--  cadena que BCrypt nunca puede producir, asi que ninguna contrasena
--  valida contra ella.
INSERT INTO usuario (id, email, password_hash, nombre, rol_id, estado)
SELECT uuid_generar_v7(), 'negocio.demo@yachay-ayacucho.pe',
       'SIN-ACCESO-cuenta-de-demostracion', 'Comercio de demostracion', r.id, 'ACTIVO'
FROM rol r
WHERE r.nombre = 'NEGOCIO'
  AND NOT EXISTS (SELECT 1 FROM usuario WHERE email = 'negocio.demo@yachay-ayacucho.pe');

-- ------------------------------------------------------------
--  2. Tres negocios: dos aprobados y uno pendiente
-- ------------------------------------------------------------
INSERT INTO negocio (id, usuario_id, categoria_negocio_id, distrito_id, nombre,
                     telefono, whatsapp, direccion, ubicacion, horario_texto, estado)
SELECT uuid_generar_v7(),
       (SELECT id FROM usuario WHERE email = 'negocio.demo@yachay-ayacucho.pe'),
       c.id, d.id, s.nombre, s.telefono, s.whatsapp, s.direccion,
       ST_SetSRID(ST_MakePoint(s.lon, s.lat), 4326), s.horario, s.estado
FROM (VALUES
    ('Restaurante El Portal de Huamanga', 'RESTAURANTES', '050101',
     '066 312 456', '51966000001', 'Portal Union 123, Plaza Mayor',
     -74.2243, -13.1595, 'Lunes a domingo, 8:00 a 22:00', 'APROBADO'),
    ('Taller de Retablos Familia Jimenez', 'ARTESANOS', '050104',
     '066 318 990', '51966000002', 'Jiron Paris 210, Santa Ana',
     -74.2279, -13.1701, 'Lunes a sabado, 9:00 a 18:00', 'APROBADO'),
    ('Hospedaje Wari (pendiente de revision)', 'HOSPEDAJES', '050101',
     '066 314 007', '51966000003', 'Jiron Libertad 560',
     -74.2251, -13.1560, 'Recepcion 24 horas', 'PENDIENTE')
) AS s(nombre, cat_codigo, dist_codigo, telefono, whatsapp, direccion, lon, lat, horario, estado)
JOIN categoria_negocio c ON c.codigo = s.cat_codigo
JOIN distrito d ON d.codigo = s.dist_codigo
WHERE NOT EXISTS (SELECT 1 FROM negocio n WHERE n.nombre = s.nombre);

INSERT INTO negocio_traduccion (negocio_id, idioma, descripcion)
SELECT n.id, t.idioma, t.descripcion
FROM (VALUES
    ('Restaurante El Portal de Huamanga', 'es',
     'DATO DE DEMOSTRACION. Cocina ayacuchana en la Plaza Mayor: puca picante, chicharron y mondongo los domingos.'),
    ('Restaurante El Portal de Huamanga', 'en',
     'DEMO DATA. Ayacucho cuisine on the Plaza Mayor: puca picante, chicharron and Sunday mondongo.'),
    ('Taller de Retablos Familia Jimenez', 'es',
     'DATO DE DEMOSTRACION. Taller familiar de retablos ayacuchanos. Se puede ver trabajar a los artesanos y comprar directamente.'),
    ('Taller de Retablos Familia Jimenez', 'en',
     'DEMO DATA. Family workshop of Ayacucho retablos. Visitors can watch the artisans at work and buy directly.'),
    ('Hospedaje Wari (pendiente de revision)', 'es',
     'DATO DE DEMOSTRACION. Este negocio esta PENDIENTE a proposito: sirve para comprobar que lo no aprobado no aparece en el directorio publico.')
) AS t(nombre, idioma, descripcion)
JOIN negocio n ON n.nombre = t.nombre
WHERE NOT EXISTS (
    SELECT 1 FROM negocio_traduccion nt WHERE nt.negocio_id = n.id AND nt.idioma = t.idioma
);

-- ------------------------------------------------------------
--  3. Historia visual: se completa la foto ACTUAL (RF-11)
-- ------------------------------------------------------------
--  El seed del Bloque 1 dejo la foto historica de la Catedral con su
--  punto de captura pero sin contraparte moderna, asi que el slider no
--  tenia dos lados que comparar. Se completa esa y se anaden dos mas.
--
--  Las imagenes son de la galeria publica de demostracion de Cloudinary:
--  no son fotos reales de Huamanga y estan aqui para que el mecanismo se
--  pueda ver funcionando. >>> SUSTITUIR POR FOTOGRAFIAS REALES <<< antes
--  de la sustentacion, con su credito de archivo correspondiente.
UPDATE lugar_imagen_historica
SET url_actual = 'https://res.cloudinary.com/demo/image/upload/w_1200/sample.jpg',
    public_id_actual = 'demo/sample'
WHERE url_actual IS NULL;

INSERT INTO lugar_imagen_historica (id, lugar_id, titulo, url_historica, public_id_historica,
                                    anio_historico, url_actual, public_id_actual,
                                    credito_historico, punto_captura, orden)
SELECT uuid_generar_v7(), l.id, s.titulo,
       'https://res.cloudinary.com/demo/image/upload/e_sepia,w_1200/sample.jpg', 'demo/sample-sepia',
       s.anio,
       'https://res.cloudinary.com/demo/image/upload/w_1200/sample.jpg', 'demo/sample',
       s.credito,
       ST_SetSRID(ST_MakePoint(s.lon, s.lat), 4326)::geography,
       s.orden
FROM (VALUES
    ('templo-de-san-francisco', 'El atrio de San Francisco', 1935::smallint,
     'DATO DE DEMOSTRACION - sustituir por archivo real', -74.2229, -13.1604, 1::smallint),
    ('plaza-mayor-de-huamanga', 'La Plaza Mayor antes del arbolado', 1928::smallint,
     'DATO DE DEMOSTRACION - sustituir por archivo real', -74.2240, -13.1592, 2::smallint)
) AS s(slug, titulo, anio, credito, lon, lat, orden)
JOIN lugar l ON l.slug = s.slug
WHERE NOT EXISTS (
    SELECT 1 FROM lugar_imagen_historica i WHERE i.titulo = s.titulo
);

-- ------------------------------------------------------------
--  4. Un video de festividad (RF-12)
-- ------------------------------------------------------------
--  Se guarda el IDENTIFICADOR, nunca la URL. Este es un video publico de
--  demostracion; >>> SUSTITUIR por uno real de la festividad <<<.
UPDATE evento
SET youtube_video_id = 'aqz-KE-bpKQ'
WHERE youtube_video_id IS NULL
  AND id IN (
      SELECT e.id FROM evento e
      JOIN evento_traduccion t ON t.evento_id = e.id AND t.idioma = 'es'
      WHERE t.nombre = 'Semana Santa de Ayacucho'
  );

COMMIT;
