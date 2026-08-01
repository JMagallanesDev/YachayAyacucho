-- ============================================================
--  V5 - Grupo 3: contenido generado por usuarios (5 entidades)
-- ============================================================

CREATE TABLE resena (
    id            UUID         PRIMARY KEY,
    usuario_id    UUID         NOT NULL REFERENCES usuario (id) ON DELETE CASCADE,
    lugar_id      UUID         NOT NULL REFERENCES lugar (id) ON DELETE CASCADE,
    calificacion  SMALLINT     NOT NULL,
    comentario    VARCHAR(500),
    estado        VARCHAR(20)  NOT NULL DEFAULT 'PUBLICADA',
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT ck_resena_calificacion CHECK (calificacion BETWEEN 1 AND 5),
    CONSTRAINT ck_resena_comentario CHECK (comentario IS NULL OR LENGTH(comentario) <= 500),
    CONSTRAINT ck_resena_estado CHECK (estado IN ('PUBLICADA', 'OCULTA', 'ELIMINADA', 'EN_REVISION')),
    -- Una unica resena por usuario y lugar: sin esto, un usuario podria
    -- inflar la calificacion de un lugar repitiendo resenas.
    CONSTRAINT uk_resena_usuario_lugar UNIQUE (usuario_id, lugar_id)
);

CREATE TABLE foto (
    id                    UUID         PRIMARY KEY,
    usuario_id            UUID         NOT NULL REFERENCES usuario (id) ON DELETE CASCADE,
    lugar_id              UUID         NOT NULL REFERENCES lugar (id) ON DELETE CASCADE,
    cloudinary_url        VARCHAR(500) NOT NULL,
    cloudinary_public_id  VARCHAR(200) NOT NULL,
    estado                VARCHAR(20)  NOT NULL DEFAULT 'PENDIENTE',
    motivo_rechazo        VARCHAR(255),
    created_at            TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at            TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT ck_foto_estado CHECK (estado IN ('PENDIENTE', 'APROBADA', 'RECHAZADA', 'EN_REVISION'))
);

-- Pivote N:M puro. La PK compuesta es la que impide duplicados: no hace
-- falta ninguna columna id adicional.
CREATE TABLE favorito (
    usuario_id  UUID        NOT NULL REFERENCES usuario (id) ON DELETE CASCADE,
    lugar_id    UUID        NOT NULL REFERENCES lugar (id) ON DELETE CASCADE,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    PRIMARY KEY (usuario_id, lugar_id)
);

-- Hecho inmutable que alimenta el pasaporte patrimonial (RF-39b).
CREATE TABLE check_in (
    id             UUID                  PRIMARY KEY,
    usuario_id     UUID                  NOT NULL REFERENCES usuario (id) ON DELETE CASCADE,
    lugar_id       UUID                  NOT NULL REFERENCES lugar (id) ON DELETE CASCADE,
    ubicacion_gps  geometry(Point, 4326) NOT NULL,
    created_at     TIMESTAMPTZ           NOT NULL DEFAULT NOW()
);

-- Dos FK nullables con CHECK en vez de una FK polimorfica: PostgreSQL no
-- puede validar integridad de una columna hacia dos tablas, y asi se
-- conserva el ON DELETE CASCADE nativo (seccion 6.6).
CREATE TABLE reporte_contenido (
    id          UUID        PRIMARY KEY,
    usuario_id  UUID        NOT NULL REFERENCES usuario (id) ON DELETE CASCADE,
    foto_id     UUID        REFERENCES foto (id) ON DELETE CASCADE,
    resena_id   UUID        REFERENCES resena (id) ON DELETE CASCADE,
    motivo      VARCHAR(30) NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT ck_reporte_contenido_motivo CHECK (
        motivo IN ('SPAM', 'OFENSIVO', 'FALSO', 'IRRELEVANTE', 'DERECHOS_AUTOR', 'OTRO')
    ),
    -- Exactamente una de las dos FK, nunca ambas ni ninguna.
    CONSTRAINT ck_reporte_contenido_xor CHECK (
        (foto_id IS NOT NULL AND resena_id IS NULL)
        OR (foto_id IS NULL AND resena_id IS NOT NULL)
    )
);
