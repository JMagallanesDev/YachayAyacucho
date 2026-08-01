-- ============================================================
--  V2 - Grupo 0: jerarquia geografica (2 entidades)
-- ============================================================
--  Provincia y Distrito existen desde el principio aunque el contenido
--  sea solo de Huamanga: escalar a las 11 provincias sera insertar
--  filas, no un ALTER TABLE arriesgado (seccion 6.6 del plan).
--
--  Los nombres son oficiales y no se traducen, por eso ninguna de las
--  dos tiene tabla de traduccion.
-- ============================================================

CREATE TABLE provincia (
    id          UUID         PRIMARY KEY,
    codigo      VARCHAR(10)  NOT NULL UNIQUE,
    nombre      VARCHAR(100) NOT NULL,
    orden       SMALLINT     NOT NULL,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

COMMENT ON TABLE provincia IS 'Las 11 provincias de la region Ayacucho';

CREATE TABLE distrito (
    id            UUID         PRIMARY KEY,
    provincia_id  UUID         NOT NULL REFERENCES provincia (id),
    codigo        VARCHAR(10)  NOT NULL UNIQUE,
    nombre        VARCHAR(100) NOT NULL,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

COMMENT ON TABLE distrito IS 'Distritos; la provincia se deriva del distrito, no al reves';
