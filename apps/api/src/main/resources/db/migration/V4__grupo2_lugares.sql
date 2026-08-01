-- ============================================================
--  V4 - Grupo 2: lugares patrimoniales y su categorizacion (6 entidades)
-- ============================================================
--  Es el nucleo del sistema y del titulo de la tesis.
-- ============================================================

CREATE TABLE categoria_lugar (
    id          UUID        PRIMARY KEY,
    codigo      VARCHAR(30) NOT NULL UNIQUE,
    icono       VARCHAR(50) NOT NULL,
    -- VARCHAR y no CHAR: CHAR rellena con espacios hasta la longitud fija,
    -- de modo que '#B3202B' volveria con padding invisible.
    color_hex   VARCHAR(7)  NOT NULL,
    orden       SMALLINT    NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT ck_categoria_lugar_color CHECK (color_hex ~ '^#[0-9A-Fa-f]{6}$')
);

-- Patron i18n: anadir un idioma es un INSERT, no un ALTER TABLE.
CREATE TABLE categoria_lugar_traduccion (
    categoria_lugar_id  UUID         NOT NULL REFERENCES categoria_lugar (id) ON DELETE CASCADE,
    idioma              VARCHAR(5)   NOT NULL,
    nombre              VARCHAR(100) NOT NULL,
    created_at          TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    PRIMARY KEY (categoria_lugar_id, idioma),
    CONSTRAINT ck_categoria_lugar_trad_idioma CHECK (idioma IN ('es', 'en'))
);

CREATE TABLE lugar (
    id                          UUID                    PRIMARY KEY,
    slug                        VARCHAR(150)            NOT NULL UNIQUE,
    categoria_lugar_id          UUID                    NOT NULL REFERENCES categoria_lugar (id),
    distrito_id                 UUID                    NOT NULL REFERENCES distrito (id),
    ubicacion                   geometry(Point, 4326)   NOT NULL,
    direccion                   VARCHAR(255),
    telefono                    VARCHAR(30),
    -- Datos practicos del bloque "Antes de ir" (RF-09d). Nullables a
    -- proposito: se desconocen hasta que el admin los verifica.
    precio_entrada_pen          NUMERIC(8, 2),
    duracion_visita_min         SMALLINT,
    acepta_tarjeta              BOOLEAN,
    tiene_banos                 BOOLEAN,
    accesible_silla_ruedas      BOOLEAN,
    apto_ninos                  BOOLEAN,
    costo_taxi_desde_plaza_pen  NUMERIC(8, 2),
    requiere_guia               BOOLEAN,
    estado                      VARCHAR(20)             NOT NULL DEFAULT 'BORRADOR',
    created_by                  UUID                    REFERENCES usuario (id) ON DELETE SET NULL,
    updated_by                  UUID                    REFERENCES usuario (id) ON DELETE SET NULL,
    created_at                  TIMESTAMPTZ             NOT NULL DEFAULT NOW(),
    updated_at                  TIMESTAMPTZ             NOT NULL DEFAULT NOW(),
    deleted_at                  TIMESTAMPTZ,
    CONSTRAINT ck_lugar_estado CHECK (estado IN ('BORRADOR', 'PUBLICADO', 'ARCHIVADO')),
    -- Restriccion geografica a Ayacucho en la propia BD (RF-22b). El
    -- frontend tambien la aplica, pero la BD es la ultima linea.
    CONSTRAINT ck_lugar_bounds_ayacucho CHECK (
        ST_X(ubicacion) BETWEEN -75.5 AND -73.0
        AND ST_Y(ubicacion) BETWEEN -15.5 AND -12.5
    ),
    CONSTRAINT ck_lugar_precio CHECK (precio_entrada_pen IS NULL OR precio_entrada_pen >= 0),
    CONSTRAINT ck_lugar_duracion CHECK (duracion_visita_min IS NULL OR duracion_visita_min > 0)
);

COMMENT ON TABLE lugar IS 'Lugar patrimonial. Sin horario en texto libre: eso vive en horario_lugar';

-- 1:N con el lugar. Filas por dia y turno, no columnas por dia: es lo
-- que hace computable el "abierto ahora" (RF-09b), las recomendaciones
-- (RF-08) y el planificador (RF-29).
CREATE TABLE horario_lugar (
    id             UUID        PRIMARY KEY,
    lugar_id       UUID        NOT NULL REFERENCES lugar (id) ON DELETE CASCADE,
    dia_semana     SMALLINT    NOT NULL,
    hora_apertura  TIME,
    hora_cierre    TIME,
    cerrado        BOOLEAN     NOT NULL DEFAULT FALSE,
    created_at     TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at     TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_by     UUID        REFERENCES usuario (id) ON DELETE SET NULL,
    CONSTRAINT ck_horario_dia CHECK (dia_semana BETWEEN 0 AND 6),
    -- O el dia esta cerrado, o hay un rango horario coherente.
    CONSTRAINT ck_horario_rango CHECK (
        cerrado = TRUE
        OR (hora_apertura IS NOT NULL AND hora_cierre IS NOT NULL AND hora_apertura < hora_cierre)
    )
);

CREATE TABLE lugar_traduccion (
    lugar_id     UUID         NOT NULL REFERENCES lugar (id) ON DELETE CASCADE,
    idioma       VARCHAR(5)   NOT NULL,
    nombre       VARCHAR(200) NOT NULL,
    descripcion  TEXT,
    historia     TEXT,
    consejos     TEXT,
    updated_by   UUID         REFERENCES usuario (id) ON DELETE SET NULL,
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at   TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    PRIMARY KEY (lugar_id, idioma),
    CONSTRAINT ck_lugar_trad_idioma CHECK (idioma IN ('es', 'en'))
);

-- Rephotography (RF-11, RF-11b). punto_captura es GEOGRAPHY porque se
-- usa para medir cercania real en metros ("Parate aqui", radio 50 m).
CREATE TABLE lugar_imagen_historica (
    id                   UUID                    PRIMARY KEY,
    lugar_id             UUID                    NOT NULL REFERENCES lugar (id) ON DELETE CASCADE,
    titulo               VARCHAR(200)            NOT NULL,
    url_historica        VARCHAR(500)            NOT NULL,
    public_id_historica  VARCHAR(200)            NOT NULL,
    anio_historico       SMALLINT                NOT NULL,
    url_actual           VARCHAR(500),
    public_id_actual     VARCHAR(200),
    credito_historico    VARCHAR(255),
    punto_captura        geography(Point, 4326),
    orden                SMALLINT                NOT NULL DEFAULT 0,
    created_at           TIMESTAMPTZ             NOT NULL DEFAULT NOW(),
    updated_at           TIMESTAMPTZ             NOT NULL DEFAULT NOW(),
    -- Limite fijo e inmutable en BD. La regla dinamica "al menos 50 anios"
    -- se valida en Bean Validation: CURRENT_DATE no es inmutable y
    -- PostgreSQL no la admite dentro de un CHECK.
    CONSTRAINT ck_imagen_anio CHECK (anio_historico BETWEEN 1500 AND 1990)
);
