-- ============================================================
--  V16 - Trazabilidad del clonado anual de eventos (RF-86)
-- ============================================================
--  Muchas festividades se repiten cada anio, asi que el panel permite
--  clonar un evento al anio siguiente. Sin saber de donde salio cada
--  clon, clonar dos veces produce dos borradores gemelos que nadie
--  distingue.
--
--  Esta columna guarda ese origen. NO es un dato derivable de ningun
--  otro (el nombre puede repetirse entre eventos distintos), asi que no
--  rompe la 3FN.
--
--  ON DELETE SET NULL y no CASCADE: si se borra la edicion de 2026, la
--  de 2027 debe sobrevivir. Ya ocurrio, y borrar el historico se llevaria
--  por delante el evento que esta a punto de celebrarse.
-- ============================================================

ALTER TABLE evento
    ADD COLUMN evento_origen_id UUID REFERENCES evento (id) ON DELETE SET NULL;

COMMENT ON COLUMN evento.evento_origen_id IS
    'Evento del que se clono este (RF-86). NULL si se creo desde cero.';

-- Sostiene la comprobacion "ya existe un clon de este evento para el anio
-- N", que es la unica consulta que usa la columna.
CREATE INDEX idx_evento_origen ON evento (evento_origen_id)
    WHERE evento_origen_id IS NOT NULL;
