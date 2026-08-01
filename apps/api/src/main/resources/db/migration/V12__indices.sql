-- ============================================================
--  V12 - Indices criticos (RNF-30)
-- ============================================================
--  Cada indice de la seccion 6.4 del plan, mas los indices de clave
--  foranea que PostgreSQL NO crea solo (a diferencia de las PK y las
--  UNIQUE) y que hacen falta para los JOIN y los borrados en cascada.
--
--  La comprobacion de que el planificador los usa de verdad esta en
--  PlanEjecucionIndicesIT, que carga volumen sintetico antes de medir:
--  con pocas filas, un Seq Scan es la decision correcta del planificador
--  y una prueba sin volumen no probaria nada.
-- ============================================================

-- ---------- Lugar: el nucleo de las consultas del sistema ----------
CREATE INDEX idx_lugar_categoria ON lugar (categoria_lugar_id);
CREATE INDEX idx_lugar_distrito ON lugar (distrito_id);

-- GIST sobre la geometria: sirve a las consultas en el plano, como el
-- filtrado por el area visible del mapa (ST_Within sobre un envelope).
CREATE INDEX idx_lugar_ubicacion ON lugar USING GIST (ubicacion);

-- Indice funcional sobre la conversion a geography.
--
-- No es redundante con el anterior, y el test PlanEjecucionIndicesTest lo
-- demostro: las busquedas por cercania miden METROS reales, y para eso hay
-- que convertir la columna con ubicacion::geography. Esa conversion cambia
-- la expresion indexable, de modo que el indice sobre la geometria pura
-- queda inservible y PostgreSQL recurria a Seq Scan. Indexar la expresion
-- exacta que usa la consulta es lo que hace util al indice
-- (RF-07 explorar cerca, RF-09c distancia a pie).
CREATE INDEX idx_lugar_ubicacion_geog ON lugar USING GIST ((ubicacion::geography));

-- Parcial: casi todas las consultas publicas filtran por lugares vivos y
-- publicados. Al excluir lo borrado, el indice es mas pequeno y rapido.
CREATE INDEX idx_lugar_estado_partial ON lugar (estado) WHERE deleted_at IS NULL;

-- ---------- Horarios: calculo de "abierto ahora" (RF-09b) ----------
CREATE INDEX idx_horario_lugar_dia ON horario_lugar (lugar_id, dia_semana);

-- ---------- Geografia ----------
CREATE INDEX idx_distrito_provincia ON distrito (provincia_id);

-- ---------- Busqueda de texto completo (RF-02) ----------
--  Indice de expresion sobre el vector de texto en espanol. to_tsvector
--  con la configuracion escrita literalmente es IMMUTABLE, requisito
--  para poder indexarla.
CREATE INDEX idx_lugartrad_fulltext ON lugar_traduccion
    USING GIN (to_tsvector('spanish',
        COALESCE(nombre, '') || ' ' || COALESCE(descripcion, '') || ' ' || COALESCE(historia, '')));

-- ---------- Contenido de usuarios ----------
CREATE INDEX idx_resena_lugar ON resena (lugar_id);
CREATE INDEX idx_resena_usuario ON resena (usuario_id);

-- Parcial: la bandeja de moderacion (RF-49) solo mira las pendientes.
CREATE INDEX idx_foto_pendientes ON foto (created_at) WHERE estado = 'PENDIENTE';
CREATE INDEX idx_foto_lugar ON foto (lugar_id);

CREATE INDEX idx_favorito_lugar ON favorito (lugar_id);
CREATE INDEX idx_checkin_usuario_lugar ON check_in (usuario_id, lugar_id);
CREATE INDEX idx_checkin_lugar ON check_in (lugar_id);

-- UNIQUE parciales: un unico reporte por usuario y contenido. Deben ser
-- parciales porque una de las dos FK siempre es NULL, y en un UNIQUE
-- normal los NULL no colisionan entre si.
CREATE UNIQUE INDEX idx_reporte_contenido_foto_unique
    ON reporte_contenido (usuario_id, foto_id) WHERE foto_id IS NOT NULL;
CREATE UNIQUE INDEX idx_reporte_contenido_resena_unique
    ON reporte_contenido (usuario_id, resena_id) WHERE resena_id IS NOT NULL;

-- ---------- Preservacion ciudadana ----------
CREATE INDEX idx_reporte_ubicacion ON reporte USING GIST (ubicacion);
CREATE INDEX idx_reporte_estado ON reporte (estado) WHERE deleted_at IS NULL;
CREATE INDEX idx_reporte_tipo ON reporte (tipo_incidente_id);
CREATE INDEX idx_foto_reporte_reporte ON foto_reporte (reporte_id);

-- ---------- Agenda cultural ----------
CREATE INDEX idx_evento_fecha ON evento (fecha_inicio, fecha_fin) WHERE deleted_at IS NULL;
CREATE INDEX idx_evento_distrito ON evento (distrito_id);
CREATE INDEX idx_evento_lugar ON evento (lugar_id);

-- ---------- Rutas ----------
CREATE INDEX idx_lugar_ruta_lugar ON lugar_ruta (lugar_id);

-- ---------- Negocios ----------
CREATE INDEX idx_negocio_categoria ON negocio (categoria_negocio_id);
CREATE INDEX idx_negocio_distrito ON negocio (distrito_id);
CREATE INDEX idx_negocio_usuario ON negocio (usuario_id);
CREATE INDEX idx_negocio_ubicacion ON negocio USING GIST (ubicacion);

-- ---------- Sesiones y auditoria ----------
-- Barrido periodico de tokens caducados (job del Bloque 2).
CREATE INDEX idx_refresh_expira ON refresh_token (expira_en);
CREATE INDEX idx_refresh_usuario ON refresh_token (usuario_id);
CREATE INDEX idx_actividad_created ON registro_actividad (created_at DESC);
CREATE INDEX idx_actividad_usuario ON registro_actividad (usuario_id);

-- ---------- Analitica ----------
CREATE INDEX idx_visita_negocio_negocio ON visita_negocio_diario (negocio_id);
