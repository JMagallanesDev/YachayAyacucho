-- ============================================================
--  V9 - Grupo 7: directorio de negocios y auditoria (5 entidades)
-- ============================================================

-- Separada de categoria_lugar a proposito: son entidades semanticamente
-- distintas con valores disjuntos (seccion 6.6).
CREATE TABLE categoria_negocio (
    id          UUID        PRIMARY KEY,
    codigo      VARCHAR(30) NOT NULL UNIQUE,
    icono       VARCHAR(50) NOT NULL,
    orden       SMALLINT    NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE categoria_negocio_traduccion (
    categoria_negocio_id  UUID         NOT NULL REFERENCES categoria_negocio (id) ON DELETE CASCADE,
    idioma                VARCHAR(5)   NOT NULL,
    nombre                VARCHAR(100) NOT NULL,
    created_at            TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at            TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    PRIMARY KEY (categoria_negocio_id, idioma),
    CONSTRAINT ck_categoria_negocio_trad_idioma CHECK (idioma IN ('es', 'en'))
);

CREATE TABLE negocio (
    id                    UUID                  PRIMARY KEY,
    usuario_id            UUID                  NOT NULL REFERENCES usuario (id),
    categoria_negocio_id  UUID                  NOT NULL REFERENCES categoria_negocio (id),
    distrito_id           UUID                  NOT NULL REFERENCES distrito (id),
    nombre                VARCHAR(200)          NOT NULL,
    ruc                   VARCHAR(11),
    telefono              VARCHAR(30),
    whatsapp              VARCHAR(30),
    direccion             VARCHAR(255),
    ubicacion             geometry(Point, 4326),
    -- Texto libre a proposito: el sistema no calcula nada con el horario
    -- del negocio (no hay "abierto ahora" en el directorio). Se normaliza
    -- lo computable, se deja como texto lo meramente informativo (6.6).
    horario_texto         VARCHAR(255),
    estado                VARCHAR(20)           NOT NULL DEFAULT 'PENDIENTE',
    created_by            UUID                  REFERENCES usuario (id) ON DELETE SET NULL,
    updated_by            UUID                  REFERENCES usuario (id) ON DELETE SET NULL,
    created_at            TIMESTAMPTZ           NOT NULL DEFAULT NOW(),
    updated_at            TIMESTAMPTZ           NOT NULL DEFAULT NOW(),
    deleted_at            TIMESTAMPTZ,
    CONSTRAINT ck_negocio_estado CHECK (estado IN ('PENDIENTE', 'APROBADO', 'RECHAZADO', 'SUSPENDIDO')),
    CONSTRAINT ck_negocio_ruc CHECK (ruc IS NULL OR ruc ~ '^[0-9]{11}$'),
    CONSTRAINT ck_negocio_bounds_ayacucho CHECK (
        ubicacion IS NULL
        OR (ST_X(ubicacion) BETWEEN -75.5 AND -73.0 AND ST_Y(ubicacion) BETWEEN -15.5 AND -12.5)
    )
);

CREATE TABLE negocio_traduccion (
    negocio_id   UUID        NOT NULL REFERENCES negocio (id) ON DELETE CASCADE,
    idioma       VARCHAR(5)  NOT NULL,
    descripcion  TEXT,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at   TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    PRIMARY KEY (negocio_id, idioma),
    CONSTRAINT ck_negocio_trad_idioma CHECK (idioma IN ('es', 'en'))
);

-- Log inmutable de acciones del admin (RF-56). Aqui la IP SI se guarda:
-- es auditoria interna de un usuario identificado, no un reporte anonimo.
-- detalles es JSONB porque cada accion registra una estructura distinta;
-- es un log, no datos relacionales de dominio (seccion 6.6).
CREATE TABLE registro_actividad (
    id          UUID        PRIMARY KEY,
    usuario_id  UUID        NOT NULL REFERENCES usuario (id),
    accion      VARCHAR(50) NOT NULL,
    entidad     VARCHAR(50) NOT NULL,
    entidad_id  UUID,
    detalles    JSONB,
    ip          VARCHAR(45),
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
