-- ============================================================
--  V7 - Grupo 5: preservacion ciudadana (4 entidades)
-- ============================================================
--  El modulo diferenciador de la tesis: el ciudadano reporta atentados
--  al patrimonio. Invierte la direccion del dato respecto a GeoPeru.
-- ============================================================

CREATE TABLE tipo_incidente (
    id          UUID        PRIMARY KEY,
    codigo      VARCHAR(30) NOT NULL UNIQUE,
    icono       VARCHAR(50) NOT NULL,
    color_hex   VARCHAR(7)  NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT ck_tipo_incidente_color CHECK (color_hex ~ '^#[0-9A-Fa-f]{6}$')
);

CREATE TABLE tipo_incidente_traduccion (
    tipo_incidente_id  UUID         NOT NULL REFERENCES tipo_incidente (id) ON DELETE CASCADE,
    idioma             VARCHAR(5)   NOT NULL,
    nombre             VARCHAR(100) NOT NULL,
    created_at         TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at         TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    PRIMARY KEY (tipo_incidente_id, idioma),
    CONSTRAINT ck_tipo_incidente_trad_idioma CHECK (idioma IN ('es', 'en'))
);

-- IMPORTANTE: esta tabla NO almacena IP ni hash de IP. Una IP, incluso
-- hasheada, es reversible por fuerza bruta sobre el espacio IPv4. El
-- anti-spam es un contador volatil en Redis con TTL de 24 h. Anonimato
-- por diseno, no por promesa (seccion 6.6).
CREATE TABLE reporte (
    id                     UUID                  PRIMARY KEY,
    tipo_incidente_id      UUID                  NOT NULL REFERENCES tipo_incidente (id),
    -- Nulo cuando el reporte es anonimo.
    usuario_id             UUID                  REFERENCES usuario (id) ON DELETE SET NULL,
    nombre_reportante      VARCHAR(120),
    descripcion            TEXT                  NOT NULL,
    ubicacion              geometry(Point, 4326) NOT NULL,
    direccion_referencial  VARCHAR(255),
    estado                 VARCHAR(20)           NOT NULL DEFAULT 'RECIBIDO',
    notas_admin            TEXT,
    es_anonimo             BOOLEAN               NOT NULL DEFAULT TRUE,
    created_by             UUID                  REFERENCES usuario (id) ON DELETE SET NULL,
    updated_by             UUID                  REFERENCES usuario (id) ON DELETE SET NULL,
    created_at             TIMESTAMPTZ           NOT NULL DEFAULT NOW(),
    updated_at             TIMESTAMPTZ           NOT NULL DEFAULT NOW(),
    deleted_at             TIMESTAMPTZ,
    CONSTRAINT ck_reporte_estado CHECK (
        estado IN ('RECIBIDO', 'EN_REVISION', 'APROBADO', 'DESCARTADO', 'RESUELTO')
    ),
    CONSTRAINT ck_reporte_bounds_ayacucho CHECK (
        ST_X(ubicacion) BETWEEN -75.5 AND -73.0
        AND ST_Y(ubicacion) BETWEEN -15.5 AND -12.5
    )
);

CREATE TABLE foto_reporte (
    id                    UUID         PRIMARY KEY,
    reporte_id            UUID         NOT NULL REFERENCES reporte (id) ON DELETE CASCADE,
    cloudinary_url        VARCHAR(500) NOT NULL,
    cloudinary_public_id  VARCHAR(200) NOT NULL,
    orden                 SMALLINT     NOT NULL DEFAULT 0,
    created_at            TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);
