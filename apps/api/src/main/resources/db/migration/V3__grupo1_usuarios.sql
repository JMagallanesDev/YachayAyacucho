-- ============================================================
--  V3 - Grupo 1: nucleo de usuarios (3 entidades)
-- ============================================================

CREATE TABLE rol (
    id           UUID        PRIMARY KEY,
    nombre       VARCHAR(20) NOT NULL UNIQUE,
    descripcion  VARCHAR(200),
    created_at   TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at   TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT ck_rol_nombre CHECK (nombre IN ('VISITANTE', 'USUARIO', 'NEGOCIO', 'ADMIN'))
);

CREATE TABLE usuario (
    id             UUID         PRIMARY KEY,
    email          VARCHAR(255) NOT NULL UNIQUE,
    password_hash  VARCHAR(100) NOT NULL,
    nombre         VARCHAR(120) NOT NULL,
    rol_id         UUID         NOT NULL REFERENCES rol (id),
    estado         VARCHAR(20)  NOT NULL DEFAULT 'ACTIVO',
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at     TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    deleted_at     TIMESTAMPTZ,
    -- Validacion de email tambien en la BD: la capa de aplicacion puede
    -- cambiar, pero cualquier INSERT por SQL directo pasa igual por aqui.
    CONSTRAINT ck_usuario_email CHECK (email ~* '^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$'),
    CONSTRAINT ck_usuario_estado CHECK (estado IN ('ACTIVO', 'SUSPENDIDO', 'PENDIENTE'))
);

-- El refresh token se guarda hasheado, nunca en claro: si alguien lee la
-- tabla no puede reutilizarlo. Rotacion en cada uso (Bloque 2).
CREATE TABLE refresh_token (
    id          UUID         PRIMARY KEY,
    usuario_id  UUID         NOT NULL REFERENCES usuario (id) ON DELETE CASCADE,
    token_hash  VARCHAR(255) NOT NULL UNIQUE,
    expira_en   TIMESTAMPTZ  NOT NULL,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);
