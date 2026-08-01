-- ============================================================
--  V10 - Grupo 8: analitica de trafico (2 entidades)
-- ============================================================
--  Tablas de hechos agregadas por dia (patron data warehouse). Sus
--  contadores no derivan de ninguna otra tabla porque los eventos crudos
--  NO se persisten, por diseno: privacidad y volumen. La tabla de hechos
--  ES la fuente primaria, y por eso cumple 3FN (secciones 6.1 y 10.3).
-- ============================================================

CREATE TABLE visita_resumen_diario (
    id              UUID        PRIMARY KEY,
    tipo_pagina     VARCHAR(30) NOT NULL,
    fecha           DATE        NOT NULL,
    total_visitas   INTEGER     NOT NULL DEFAULT 0,
    visitas_unicas  INTEGER     NOT NULL DEFAULT 0,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT ck_visita_tipo_pagina CHECK (
        tipo_pagina IN ('HOME', 'LUGAR', 'MAPA', 'EVENTO', 'RUTA', 'DIRECTORIO', 'REPORTAR')
    ),
    CONSTRAINT ck_visita_totales CHECK (total_visitas >= 0 AND visitas_unicas >= 0),
    CONSTRAINT uk_visita_resumen_dia UNIQUE (tipo_pagina, fecha)
);

CREATE TABLE visita_negocio_diario (
    id                 UUID        PRIMARY KEY,
    negocio_id         UUID        NOT NULL REFERENCES negocio (id) ON DELETE CASCADE,
    fecha              DATE        NOT NULL,
    total_visitas      INTEGER     NOT NULL DEFAULT 0,
    clics_whatsapp     INTEGER     NOT NULL DEFAULT 0,
    clics_como_llegar  INTEGER     NOT NULL DEFAULT 0,
    created_at         TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at         TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT ck_visita_negocio_totales CHECK (
        total_visitas >= 0 AND clics_whatsapp >= 0 AND clics_como_llegar >= 0
    ),
    CONSTRAINT uk_visita_negocio_dia UNIQUE (negocio_id, fecha)
);
