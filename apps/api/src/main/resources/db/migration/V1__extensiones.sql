-- ============================================================
--  V1 - Extensiones de PostgreSQL
-- ============================================================
--  PostGIS habilita los tipos geometry/geography, los indices GIST y
--  las funciones ST_* sobre las que se apoyan la busqueda por cercania
--  (RF-07), la distancia a pie (RF-09c) y el mapa de incidentes (RF-74).
-- ============================================================

CREATE EXTENSION IF NOT EXISTS postgis;


-- ============================================================
--  Generador de UUID v7 en SQL
-- ============================================================
--  La aplicacion genera sus UUID v7 con uuid-creator (Java). Esta
--  funcion existe para lo que se inserta por SQL: los catalogos de la
--  V14 y los datos de demostracion. Sin ella habria que elegir entre
--  incrustar cientos de UUID literales o usar gen_random_uuid(), que
--  produce v4 y rompe la convencion de PK ordenable por tiempo.
--
--  PostgreSQL 16 no trae uuidv7() nativo (llega en la 18).
--
--  Estructura RFC 9562: 48 bits de timestamp Unix en milisegundos,
--  4 bits de version (7), 2 bits de variante y el resto aleatorio.
-- ============================================================
CREATE OR REPLACE FUNCTION uuid_generar_v7()
    RETURNS uuid
    LANGUAGE plpgsql
    VOLATILE
AS $$
DECLARE
    v_bytes bytea;
BEGIN
    -- int8send da 8 bytes big-endian; se descartan los 2 primeros para
    -- quedarse con los 48 bits de milisegundos. Los 10 bytes restantes
    -- se toman de un UUID aleatorio, evitando depender de pgcrypto.
    v_bytes := substring(int8send((EXTRACT(EPOCH FROM clock_timestamp()) * 1000)::bigint) FROM 3)
               || substring(uuid_send(gen_random_uuid()) FROM 7 FOR 10);

    -- Byte 6: los 4 bits altos pasan a valer 7 (version).
    v_bytes := set_byte(v_bytes, 6, (get_byte(v_bytes, 6) & 15) | 112);
    -- Byte 8: los 2 bits altos pasan a valer 10 (variante RFC 4122).
    v_bytes := set_byte(v_bytes, 8, (get_byte(v_bytes, 8) & 63) | 128);

    RETURN encode(v_bytes, 'hex')::uuid;
END;
$$;

COMMENT ON FUNCTION uuid_generar_v7() IS
    'UUID v7 (RFC 9562) para inserciones por SQL. La aplicacion usa uuid-creator.';
