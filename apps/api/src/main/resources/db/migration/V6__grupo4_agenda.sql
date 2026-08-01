-- ============================================================
--  V6 - Grupo 4: agenda cultural (2 entidades)
-- ============================================================

CREATE TABLE evento (
    id                      UUID         PRIMARY KEY,
    -- Opcional: hay festividades de distrito que no ocurren en un lugar
    -- patrimonial concreto.
    lugar_id                UUID         REFERENCES lugar (id) ON DELETE SET NULL,
    distrito_id             UUID         NOT NULL REFERENCES distrito (id),
    tipo                    VARCHAR(30)  NOT NULL,
    fecha_inicio            DATE         NOT NULL,
    fecha_fin               DATE         NOT NULL,
    cloudinary_url_portada  VARCHAR(500),
    recurrente_anual        BOOLEAN      NOT NULL DEFAULT FALSE,
    estado                  VARCHAR(20)  NOT NULL DEFAULT 'BORRADOR',
    created_by              UUID         REFERENCES usuario (id) ON DELETE SET NULL,
    updated_by              UUID         REFERENCES usuario (id) ON DELETE SET NULL,
    created_at              TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at              TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    deleted_at              TIMESTAMPTZ,
    CONSTRAINT ck_evento_tipo CHECK (
        tipo IN ('RELIGIOSO', 'CIVICO', 'CULTURAL', 'GASTRONOMICO', 'ARTESANAL', 'MUSICAL', 'OTRO')
    ),
    CONSTRAINT ck_evento_estado CHECK (estado IN ('BORRADOR', 'PUBLICADO', 'CANCELADO', 'ARCHIVADO')),
    CONSTRAINT ck_evento_fechas CHECK (fecha_fin >= fecha_inicio)
);

CREATE TABLE evento_traduccion (
    evento_id     UUID         NOT NULL REFERENCES evento (id) ON DELETE CASCADE,
    idioma        VARCHAR(5)   NOT NULL,
    nombre        VARCHAR(200) NOT NULL,
    descripcion   TEXT,
    organizador   VARCHAR(200),
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    PRIMARY KEY (evento_id, idioma),
    CONSTRAINT ck_evento_trad_idioma CHECK (idioma IN ('es', 'en'))
);
