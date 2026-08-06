-- ============================================================
--  Check-ins de demostracion (RF-39) — Bloque 7
-- ============================================================
--  El ranking "mas visitados" quedo plano desde el Bloque 4 por la
--  misma razon que el de "mejor valorados": no habia datos. Las
--  visitas se registran con GPS desde la aplicacion, asi que un
--  entorno recien montado tiene cero check-ins y los 15 lugares
--  comparten 0 visitas.
--
--  Este seed reparte visitas de forma desigual entre los usuarios de
--  demostracion que creo el Bloque 6, para que el orden signifique
--  algo: la Plaza y la Catedral por encima, Vilcashuaman al fondo.
--
--  La ubicacion registrada es la del propio lugar. En la aplicacion
--  real se guarda la posicion que envio el navegador —para poder
--  auditarla—, pero aqui no hay navegador que la envie.
--
--  Idempotente: solo inserta si ese usuario no visito ya ese lugar.
-- ============================================================

BEGIN;

INSERT INTO check_in (id, usuario_id, lugar_id, ubicacion_gps, created_at)
SELECT uuid_generar_v7(), u.id, l.id, l.ubicacion, NOW() - (s.dias || ' days')::INTERVAL
FROM (VALUES
    ('rosa.demo@yachay-ayacucho.pe',   'plaza-mayor-de-huamanga',     1),
    ('julio.demo@yachay-ayacucho.pe',  'plaza-mayor-de-huamanga',     2),
    ('marina.demo@yachay-ayacucho.pe', 'plaza-mayor-de-huamanga',     3),
    ('tomas.demo@yachay-ayacucho.pe',  'plaza-mayor-de-huamanga',     4),
    ('elena.demo@yachay-ayacucho.pe',  'plaza-mayor-de-huamanga',     5),
    ('victor.demo@yachay-ayacucho.pe', 'plaza-mayor-de-huamanga',     6),

    ('rosa.demo@yachay-ayacucho.pe',   'catedral-de-ayacucho',        1),
    ('julio.demo@yachay-ayacucho.pe',  'catedral-de-ayacucho',        2),
    ('marina.demo@yachay-ayacucho.pe', 'catedral-de-ayacucho',        3),
    ('tomas.demo@yachay-ayacucho.pe',  'catedral-de-ayacucho',        4),
    ('elena.demo@yachay-ayacucho.pe',  'catedral-de-ayacucho',        7),

    ('rosa.demo@yachay-ayacucho.pe',   'templo-de-san-francisco',     2),
    ('julio.demo@yachay-ayacucho.pe',  'templo-de-san-francisco',     3),
    ('elena.demo@yachay-ayacucho.pe',  'templo-de-san-francisco',     8),
    ('victor.demo@yachay-ayacucho.pe', 'templo-de-san-francisco',     9),

    ('julio.demo@yachay-ayacucho.pe',  'museo-memoria-anfasep',       3),
    ('marina.demo@yachay-ayacucho.pe', 'museo-memoria-anfasep',       4),
    ('elena.demo@yachay-ayacucho.pe',  'museo-memoria-anfasep',       6),

    ('rosa.demo@yachay-ayacucho.pe',   'mirador-carmen-alto',         5),
    ('tomas.demo@yachay-ayacucho.pe',  'mirador-carmen-alto',         6),
    ('victor.demo@yachay-ayacucho.pe', 'mirador-carmen-alto',         7),

    ('julio.demo@yachay-ayacucho.pe',  'barrio-artesanal-santa-ana',  4),
    ('elena.demo@yachay-ayacucho.pe',  'barrio-artesanal-santa-ana',  9),

    ('marina.demo@yachay-ayacucho.pe', 'museo-hipolito-unanue',       5),
    ('tomas.demo@yachay-ayacucho.pe',  'museo-hipolito-unanue',      10),

    ('rosa.demo@yachay-ayacucho.pe',   'mercado-12-de-abril',         6),
    ('elena.demo@yachay-ayacucho.pe',  'templo-de-santo-domingo',    11),
    ('tomas.demo@yachay-ayacucho.pe',  'complejo-arqueologico-wari', 12),
    ('victor.demo@yachay-ayacucho.pe', 'intihuatana-de-vilcashuaman',14)
) AS s(email, slug, dias)
JOIN usuario u ON u.email = s.email
JOIN lugar l ON l.slug = s.slug AND l.deleted_at IS NULL
WHERE NOT EXISTS (
    SELECT 1 FROM check_in c WHERE c.usuario_id = u.id AND c.lugar_id = l.id
);

-- Sin esto el ranking seguiria plano hasta el siguiente ciclo del job:
-- la vista materializada no se entera de lo que entra por SQL.
REFRESH MATERIALIZED VIEW estadistica_lugar;

COMMIT;

\echo 'Seed de check-ins aplicado.'
