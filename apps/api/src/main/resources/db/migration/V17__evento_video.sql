-- ============================================================
--  V17 - Video de festividades en la agenda (RF-12)
-- ============================================================
--  Se guarda el IDENTIFICADOR de YouTube, no la URL completa, y la
--  diferencia es de seguridad: con el identificador el servidor construye
--  la direccion del embed y nadie puede colar una URL arbitraria dentro
--  de un iframe de nuestra pagina. Con una URL libre, un administrador
--  comprometido —o un error de validacion— podria incrustar cualquier
--  cosa en el sitio.
--
--  El CHECK fija el formato real de YouTube: 11 caracteres de un alfabeto
--  base64url. Es la misma idea que el CHECK del RUC en `negocio`.
--
--  Va en `evento` y no en `lugar` porque el requisito habla de videos de
--  FESTIVIDADES. Si algun dia un lugar necesita el suyo, sera otra
--  migracion y otra decision.
-- ============================================================

ALTER TABLE evento
    ADD COLUMN youtube_video_id VARCHAR(20);

ALTER TABLE evento
    ADD CONSTRAINT ck_evento_youtube_id CHECK (
        youtube_video_id IS NULL OR youtube_video_id ~ '^[A-Za-z0-9_-]{11}$'
    );

COMMENT ON COLUMN evento.youtube_video_id IS
    'Identificador de YouTube (11 caracteres), no la URL. El embed lo compone la aplicacion (RF-12).';
