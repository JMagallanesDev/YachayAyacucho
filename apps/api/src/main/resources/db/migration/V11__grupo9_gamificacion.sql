-- ============================================================
--  V11 - Grupo 9: gamificacion (3 entidades)
-- ============================================================
--  El progreso por ruta NO se almacena: seria un atributo derivado y
--  violaria 3FN. Se calcula con COUNT sobre check_in x lugar_ruta. Solo
--  se persiste el hecho inmutable: la insignia obtenida y su fecha.
-- ============================================================

CREATE TABLE insignia (
    id          UUID        PRIMARY KEY,
    codigo      VARCHAR(30) NOT NULL UNIQUE,
    icono       VARCHAR(50) NOT NULL,
    -- Regla de obtencion, opaca al modelo: la BD nunca consulta dentro
    -- del JSON, lo interpreta el service. Es configuracion, no datos
    -- relacionales, por lo que no compromete 1FN (seccion 6.6).
    criterio    JSONB       NOT NULL,
    orden       SMALLINT    NOT NULL DEFAULT 0,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE insignia_traduccion (
    insignia_id  UUID         NOT NULL REFERENCES insignia (id) ON DELETE CASCADE,
    idioma       VARCHAR(5)   NOT NULL,
    nombre       VARCHAR(100) NOT NULL,
    descripcion  VARCHAR(300),
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at   TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    PRIMARY KEY (insignia_id, idioma),
    CONSTRAINT ck_insignia_trad_idioma CHECK (idioma IN ('es', 'en'))
);

-- La PK compuesta garantiza una insignia por usuario sin indice extra.
CREATE TABLE insignia_usuario (
    usuario_id   UUID        NOT NULL REFERENCES usuario (id) ON DELETE CASCADE,
    insignia_id  UUID        NOT NULL REFERENCES insignia (id) ON DELETE CASCADE,
    obtenida_en  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    PRIMARY KEY (usuario_id, insignia_id)
);
