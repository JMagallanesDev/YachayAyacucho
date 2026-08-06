-- ============================================================
--  Resenas de demostracion (RF-37) — Bloque 6
-- ============================================================
--  Los rankings del Bloque 4 quedaron "planos" por una razon simple:
--  no habia ni una sola resena, asi que los 15 lugares compartian
--  promedio 0.00 y "mejor valorados" caia en el desempate alfabetico.
--  Este seed les da datos dispares para que el orden signifique algo.
--
--  SEGURIDAD: los usuarios de demostracion llevan un password_hash
--  INUTILIZABLE, igual que el admin del Bloque 1. No es un hash de una
--  contrasena conocida: es una cadena que BCrypt nunca puede producir,
--  de modo que ninguna contrasena valida contra ella. Estas cuentas
--  existen para poblar resenas, no para iniciar sesion.
--
--  Idempotente: se puede ejecutar N veces sin duplicar nada.
-- ============================================================

BEGIN;

-- ------------------------------------------------------------
--  1. Seis visitantes de demostracion
-- ------------------------------------------------------------
INSERT INTO usuario (id, email, password_hash, nombre, rol_id, estado)
SELECT uuid_generar_v7(), v.email, 'SIN-ACCESO-cuenta-de-demostracion', v.nombre, r.id, 'ACTIVO'
FROM (VALUES
    ('rosa.demo@yachay-ayacucho.pe',    'Rosa Quispe'),
    ('julio.demo@yachay-ayacucho.pe',   'Julio Palomino'),
    ('marina.demo@yachay-ayacucho.pe',  'Marina Ccahuana'),
    ('tomas.demo@yachay-ayacucho.pe',   'Tomas Berrocal'),
    ('elena.demo@yachay-ayacucho.pe',   'Elena Prado'),
    ('victor.demo@yachay-ayacucho.pe',  'Victor Huaman')
) AS v(email, nombre)
JOIN rol r ON r.nombre = 'USUARIO'
WHERE NOT EXISTS (SELECT 1 FROM usuario u WHERE u.email = v.email);

-- ------------------------------------------------------------
--  2. Resenas
-- ------------------------------------------------------------
--  Las notas estan repartidas a proposito para que el ranking
--  diferencie: la Catedral y San Francisco arriba, el mercado y la
--  plaza en la media, Vilcashuaman abajo por lo lejos que queda.
--
--  El UNIQUE (usuario_id, lugar_id) impide duplicados aunque este
--  script se repita; el WHERE NOT EXISTS evita que salte el error.
INSERT INTO resena (id, usuario_id, lugar_id, calificacion, comentario, estado)
SELECT uuid_generar_v7(), u.id, l.id, s.calificacion, s.comentario, 'PUBLICADA'
FROM (VALUES
    ('rosa.demo@yachay-ayacucho.pe',   'catedral-de-ayacucho',        5::smallint, 'El retablo mayor en pan de oro justifica el viaje por si solo.'),
    ('julio.demo@yachay-ayacucho.pe',  'catedral-de-ayacucho',        5::smallint, 'Fui en Semana Santa y no se me olvida.'),
    ('marina.demo@yachay-ayacucho.pe', 'catedral-de-ayacucho',        4::smallint, 'Preciosa, aunque conviene ir temprano para verla con calma.'),
    ('tomas.demo@yachay-ayacucho.pe',  'catedral-de-ayacucho',        5::smallint, NULL),

    ('rosa.demo@yachay-ayacucho.pe',   'templo-de-san-francisco',     5::smallint, 'Los retablos del siglo XVI estan mejor conservados de lo que esperaba.'),
    ('elena.demo@yachay-ayacucho.pe',  'templo-de-san-francisco',     4::smallint, 'Muy bonito. Cierra al mediodia, tenlo en cuenta.'),
    ('victor.demo@yachay-ayacucho.pe', 'templo-de-san-francisco',     5::smallint, NULL),

    ('julio.demo@yachay-ayacucho.pe',  'museo-memoria-anfasep',       5::smallint, 'Duro y necesario. Se sale en silencio.'),
    ('marina.demo@yachay-ayacucho.pe', 'museo-memoria-anfasep',       5::smallint, 'Imprescindible para entender Ayacucho.'),
    ('elena.demo@yachay-ayacucho.pe',  'museo-memoria-anfasep',       4::smallint, 'Pequeno pero muy bien explicado.'),

    ('rosa.demo@yachay-ayacucho.pe',   'mirador-carmen-alto',         4::smallint, 'La vista al atardecer merece la subida.'),
    ('tomas.demo@yachay-ayacucho.pe',  'mirador-carmen-alto',         4::smallint, NULL),
    ('victor.demo@yachay-ayacucho.pe', 'mirador-carmen-alto',         5::smallint, 'El mejor sitio para ver Huamanga entera.'),

    ('julio.demo@yachay-ayacucho.pe',  'barrio-artesanal-santa-ana',  4::smallint, 'Se puede ver a los artesanos trabajando el retablo.'),
    ('elena.demo@yachay-ayacucho.pe',  'barrio-artesanal-santa-ana',  5::smallint, 'Compre un retablo directamente al taller. Trato inmejorable.'),

    ('marina.demo@yachay-ayacucho.pe', 'museo-hipolito-unanue',       4::smallint, 'Buena coleccion Wari.'),
    ('tomas.demo@yachay-ayacucho.pe',  'museo-hipolito-unanue',       3::smallint, 'Interesante, aunque las cartelas se quedan cortas.'),

    ('rosa.demo@yachay-ayacucho.pe',   'plaza-mayor-de-huamanga',     4::smallint, 'El corazon de la ciudad. Siempre hay algo pasando.'),
    ('victor.demo@yachay-ayacucho.pe', 'plaza-mayor-de-huamanga',     4::smallint, NULL),
    ('julio.demo@yachay-ayacucho.pe',  'plaza-mayor-de-huamanga',     3::smallint, 'Bonita, pero con mucho trafico alrededor.'),

    ('elena.demo@yachay-ayacucho.pe',  'templo-de-santo-domingo',     4::smallint, 'La fachada es una joya.'),
    ('marina.demo@yachay-ayacucho.pe', 'templo-de-santo-domingo',     4::smallint, NULL),

    ('tomas.demo@yachay-ayacucho.pe',  'complejo-arqueologico-wari',  4::smallint, 'Enorme. Lleva agua y gorra, hay poca sombra.'),
    ('victor.demo@yachay-ayacucho.pe', 'complejo-arqueologico-wari',  3::smallint, 'Impresiona, pero falta senalizacion.'),

    ('rosa.demo@yachay-ayacucho.pe',   'mercado-12-de-abril',         3::smallint, 'Autentico. No es para todo el mundo.'),
    ('julio.demo@yachay-ayacucho.pe',  'mercado-12-de-abril',         4::smallint, 'El mejor sitio para probar el puca picante.'),

    ('marina.demo@yachay-ayacucho.pe', 'templo-de-la-merced',         3::smallint, 'Correcto, se visita en veinte minutos.'),
    ('elena.demo@yachay-ayacucho.pe',  'mirador-de-acuchimay',        4::smallint, 'Vistas espectaculares, subida empinada.'),
    ('tomas.demo@yachay-ayacucho.pe',  'santuario-pampa-de-quinua',   4::smallint, 'Vale la pena por la historia que hay detras.'),

    ('victor.demo@yachay-ayacucho.pe', 'intihuatana-de-vilcashuaman', 3::smallint, 'Impresionante, pero queda muy lejos para ir y volver en el dia.'),
    ('rosa.demo@yachay-ayacucho.pe',   'bosque-de-puyas-titankayocc', 4::smallint, 'Un paisaje que no se parece a nada.')
) AS s(email, slug, calificacion, comentario)
JOIN usuario u ON u.email = s.email
JOIN lugar l ON l.slug = s.slug AND l.deleted_at IS NULL
WHERE NOT EXISTS (
    SELECT 1 FROM resena r WHERE r.usuario_id = u.id AND r.lugar_id = l.id
);

-- Sin este refresco los promedios seguirian a cero hasta el siguiente
-- ciclo del job: la vista no se entera de lo que entra por SQL.
REFRESH MATERIALIZED VIEW estadistica_lugar;

COMMIT;

\echo 'Seed de resenas aplicado.'
