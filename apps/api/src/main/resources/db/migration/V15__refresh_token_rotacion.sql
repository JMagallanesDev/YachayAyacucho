-- ============================================================
--  V15 - Rotacion y deteccion de reutilizacion de refresh tokens
-- ============================================================
--  Hasta aqui, rotar un refresh token significaba borrar la fila: si
--  alguien reutilizaba el viejo, simplemente no lo encontraba y recibia
--  un 401. Correcto, pero ciego: el sistema no distinguia entre "token
--  caducado" y "token robado y reproducido".
--
--  Con estas dos columnas la fila sobrevive a la rotacion, marcada como
--  usada. Presentar un token ya usado solo puede significar una cosa:
--  hay dos actores con la misma credencial, es decir, robo. La respuesta
--  correcta no es rechazar esa peticion, sino revocar TODAS las sesiones
--  de ese usuario y obligarle a autenticarse de nuevo.
-- ============================================================

ALTER TABLE refresh_token
    ADD COLUMN usado_en    TIMESTAMPTZ,
    ADD COLUMN revocado_en TIMESTAMPTZ;

COMMENT ON COLUMN refresh_token.usado_en IS
    'Momento en que se rotó. Un token con este valor ya no es válido; si alguien lo presenta, es reutilización.';
COMMENT ON COLUMN refresh_token.revocado_en IS
    'Momento en que se invalidó por logout o por revocación en cascada tras detectar una reutilización.';

-- La validacion de cada refresh busca por hash y comprueba que siga
-- vivo. El indice parcial mantiene pequeno el conjunto de tokens
-- activos, que es el unico que se consulta en el camino caliente.
CREATE INDEX idx_refresh_token_activos ON refresh_token (usuario_id)
    WHERE usado_en IS NULL AND revocado_en IS NULL;
