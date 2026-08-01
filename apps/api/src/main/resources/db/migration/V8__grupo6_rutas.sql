-- ============================================================
--  V8 - Grupo 6: rutas tematicas (3 entidades)
-- ============================================================

CREATE TABLE ruta_tematica (
    id          UUID         PRIMARY KEY,
    slug        VARCHAR(150) NOT NULL UNIQUE,
    color_hex   VARCHAR(7)   NOT NULL,
    icono       VARCHAR(50)  NOT NULL,
    activa      BOOLEAN      NOT NULL DEFAULT TRUE,
    orden       SMALLINT     NOT NULL DEFAULT 0,
    created_by  UUID         REFERENCES usuario (id) ON DELETE SET NULL,
    updated_by  UUID         REFERENCES usuario (id) ON DELETE SET NULL,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    deleted_at  TIMESTAMPTZ,
    CONSTRAINT ck_ruta_color CHECK (color_hex ~ '^#[0-9A-Fa-f]{6}$')
);

CREATE TABLE ruta_traduccion (
    ruta_tematica_id  UUID         NOT NULL REFERENCES ruta_tematica (id) ON DELETE CASCADE,
    idioma            VARCHAR(5)   NOT NULL,
    nombre            VARCHAR(200) NOT NULL,
    descripcion       TEXT,
    created_at        TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at        TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    PRIMARY KEY (ruta_tematica_id, idioma),
    CONSTRAINT ck_ruta_trad_idioma CHECK (idioma IN ('es', 'en'))
);

-- Pivote N:M con atributo propio: el orden de visita dentro de la ruta.
CREATE TABLE lugar_ruta (
    ruta_tematica_id  UUID     NOT NULL REFERENCES ruta_tematica (id) ON DELETE CASCADE,
    lugar_id          UUID     NOT NULL REFERENCES lugar (id) ON DELETE CASCADE,
    orden             SMALLINT NOT NULL,
    PRIMARY KEY (ruta_tematica_id, lugar_id),
    CONSTRAINT ck_lugar_ruta_orden CHECK (orden >= 0)
);
