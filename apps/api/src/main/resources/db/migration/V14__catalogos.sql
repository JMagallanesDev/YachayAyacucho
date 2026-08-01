-- ============================================================
--  V14 - Datos de referencia (catalogos)
-- ============================================================
--  Estos datos NO son datos de prueba: son parte del contrato del
--  esquema. Sin roles, categorias o tipos de incidente la aplicacion no
--  funciona, ni en desarrollo ni en produccion. Por eso viajan en una
--  migracion y no en el seed de demostracion (db/seed/seed_demo.sql),
--  que si es desechable.
--
--  Nota sobre los codigos de distrito: los nombres corresponden a la
--  division politica oficial de la region Ayacucho (11 provincias, 119
--  distritos) y los codigos siguen la estructura UBIGEO del INEI
--  (region 05 + provincia + distrito). Conviene cotejarlos contra el
--  padron vigente del INEI antes de la sustentacion.
-- ============================================================

-- ------------------------------------------------------------
--  Roles (RF-31, RNF-16)
-- ------------------------------------------------------------
INSERT INTO rol (id, nombre, descripcion) VALUES
    (uuid_generar_v7(), 'VISITANTE', 'Turista sin cuenta: explora, busca y reporta incidentes de forma anonima'),
    (uuid_generar_v7(), 'USUARIO',   'Turista registrado: favoritos, resenas, fotos, check-in y pasaporte'),
    (uuid_generar_v7(), 'NEGOCIO',   'Dueno de negocio local con panel propio en el directorio'),
    (uuid_generar_v7(), 'ADMIN',     'Gestor interno: CRUD de contenido, moderacion, metricas y auditoria');

-- ------------------------------------------------------------
--  Provincias de Ayacucho (11)
-- ------------------------------------------------------------
INSERT INTO provincia (id, codigo, nombre, orden) VALUES
    (uuid_generar_v7(), '0501', 'Huamanga', 1),
    (uuid_generar_v7(), '0502', 'Cangallo', 2),
    (uuid_generar_v7(), '0503', 'Huanca Sancos', 3),
    (uuid_generar_v7(), '0504', 'Huanta', 4),
    (uuid_generar_v7(), '0505', 'La Mar', 5),
    (uuid_generar_v7(), '0506', 'Lucanas', 6),
    (uuid_generar_v7(), '0507', 'Parinacochas', 7),
    (uuid_generar_v7(), '0508', 'Paucar del Sara Sara', 8),
    (uuid_generar_v7(), '0509', 'Sucre', 9),
    (uuid_generar_v7(), '0510', 'Victor Fajardo', 10),
    (uuid_generar_v7(), '0511', 'Vilcas Huaman', 11);

-- ------------------------------------------------------------
--  Distritos (119). Se resuelven contra provincia por su codigo.
-- ------------------------------------------------------------
INSERT INTO distrito (id, provincia_id, codigo, nombre)
SELECT uuid_generar_v7(), p.id, d.codigo, d.nombre
FROM (VALUES
    -- Huamanga (16)
    ('050101', 'Ayacucho', '0501'),
    ('050102', 'Acocro', '0501'),
    ('050103', 'Acos Vinchos', '0501'),
    ('050104', 'Carmen Alto', '0501'),
    ('050105', 'Chiara', '0501'),
    ('050106', 'Ocros', '0501'),
    ('050107', 'Pacaycasa', '0501'),
    ('050108', 'Quinua', '0501'),
    ('050109', 'San Jose de Ticllas', '0501'),
    ('050110', 'San Juan Bautista', '0501'),
    ('050111', 'Santiago de Pischa', '0501'),
    ('050112', 'Socos', '0501'),
    ('050113', 'Tambillo', '0501'),
    ('050114', 'Vinchos', '0501'),
    ('050115', 'Jesus Nazareno', '0501'),
    ('050116', 'Andres Avelino Caceres Dorregaray', '0501'),
    -- Cangallo (6)
    ('050201', 'Cangallo', '0502'),
    ('050202', 'Chuschi', '0502'),
    ('050203', 'Los Morochucos', '0502'),
    ('050204', 'Maria Parado de Bellido', '0502'),
    ('050205', 'Paras', '0502'),
    ('050206', 'Totos', '0502'),
    -- Huanca Sancos (4)
    ('050301', 'Sancos', '0503'),
    ('050302', 'Carapo', '0503'),
    ('050303', 'Sacsamarca', '0503'),
    ('050304', 'Santiago de Lucanamarca', '0503'),
    -- Huanta (12)
    ('050401', 'Huanta', '0504'),
    ('050402', 'Ayahuanco', '0504'),
    ('050403', 'Huamanguilla', '0504'),
    ('050404', 'Iguain', '0504'),
    ('050405', 'Luricocha', '0504'),
    ('050406', 'Santillana', '0504'),
    ('050407', 'Sivia', '0504'),
    ('050408', 'Llochegua', '0504'),
    ('050409', 'Canayre', '0504'),
    ('050410', 'Uchuraccay', '0504'),
    ('050411', 'Pucacolpa', '0504'),
    ('050412', 'Chaca', '0504'),
    -- La Mar (11)
    ('050501', 'San Miguel', '0505'),
    ('050502', 'Anco', '0505'),
    ('050503', 'Ayna', '0505'),
    ('050504', 'Chilcas', '0505'),
    ('050505', 'Chungui', '0505'),
    ('050506', 'Luis Carranza', '0505'),
    ('050507', 'Santa Rosa', '0505'),
    ('050508', 'Tambo', '0505'),
    ('050509', 'Samugari', '0505'),
    ('050510', 'Anchihuay', '0505'),
    ('050511', 'Oronccoy', '0505'),
    -- Lucanas (21)
    ('050601', 'Puquio', '0506'),
    ('050602', 'Aucara', '0506'),
    ('050603', 'Cabana', '0506'),
    ('050604', 'Carmen Salcedo', '0506'),
    ('050605', 'Chavina', '0506'),
    ('050606', 'Chipao', '0506'),
    ('050607', 'Huac-Huas', '0506'),
    ('050608', 'Laramate', '0506'),
    ('050609', 'Leoncio Prado', '0506'),
    ('050610', 'Llauta', '0506'),
    ('050611', 'Lucanas', '0506'),
    ('050612', 'Ocana', '0506'),
    ('050613', 'Otoca', '0506'),
    ('050614', 'Saisa', '0506'),
    ('050615', 'San Cristobal', '0506'),
    ('050616', 'San Juan', '0506'),
    ('050617', 'San Pedro', '0506'),
    ('050618', 'San Pedro de Palco', '0506'),
    ('050619', 'Sancos', '0506'),
    ('050620', 'Santa Ana de Huaycahuacho', '0506'),
    ('050621', 'Santa Lucia', '0506'),
    -- Parinacochas (8)
    ('050701', 'Coracora', '0507'),
    ('050702', 'Chumpi', '0507'),
    ('050703', 'Coronel Castaneda', '0507'),
    ('050704', 'Pacapausa', '0507'),
    ('050705', 'Pullo', '0507'),
    ('050706', 'Puyusca', '0507'),
    ('050707', 'San Francisco de Ravacayco', '0507'),
    ('050708', 'Upahuacho', '0507'),
    -- Paucar del Sara Sara (10)
    ('050801', 'Pausa', '0508'),
    ('050802', 'Colta', '0508'),
    ('050803', 'Corculla', '0508'),
    ('050804', 'Lampa', '0508'),
    ('050805', 'Marcabamba', '0508'),
    ('050806', 'Oyolo', '0508'),
    ('050807', 'Pararca', '0508'),
    ('050808', 'San Javier de Alpabamba', '0508'),
    ('050809', 'San Jose de Ushua', '0508'),
    ('050810', 'Sara Sara', '0508'),
    -- Sucre (11)
    ('050901', 'Querobamba', '0509'),
    ('050902', 'Belen', '0509'),
    ('050903', 'Chalcos', '0509'),
    ('050904', 'Chilcayoc', '0509'),
    ('050905', 'Huacana', '0509'),
    ('050906', 'Morcolla', '0509'),
    ('050907', 'Paico', '0509'),
    ('050908', 'San Pedro de Larcay', '0509'),
    ('050909', 'San Salvador de Quije', '0509'),
    ('050910', 'Santiago de Paucaray', '0509'),
    ('050911', 'Soras', '0509'),
    -- Victor Fajardo (12)
    ('051001', 'Huancapi', '0510'),
    ('051002', 'Alcamenca', '0510'),
    ('051003', 'Apongo', '0510'),
    ('051004', 'Asquipata', '0510'),
    ('051005', 'Canaria', '0510'),
    ('051006', 'Cayara', '0510'),
    ('051007', 'Colca', '0510'),
    ('051008', 'Huamanquiquia', '0510'),
    ('051009', 'Huancaraylla', '0510'),
    ('051010', 'Huaya', '0510'),
    ('051011', 'Sarhua', '0510'),
    ('051012', 'Vilcanchos', '0510'),
    -- Vilcas Huaman (8)
    ('051101', 'Vilcas Huaman', '0511'),
    ('051102', 'Accomarca', '0511'),
    ('051103', 'Carhuanca', '0511'),
    ('051104', 'Concepcion', '0511'),
    ('051105', 'Huambalpa', '0511'),
    ('051106', 'Independencia', '0511'),
    ('051107', 'Saurama', '0511'),
    ('051108', 'Vischongo', '0511')
) AS d(codigo, nombre, prov_codigo)
JOIN provincia p ON p.codigo = d.prov_codigo;

-- ------------------------------------------------------------
--  Categorias de lugar (8) con traduccion es/en
-- ------------------------------------------------------------
INSERT INTO categoria_lugar (id, codigo, icono, color_hex, orden) VALUES
    (uuid_generar_v7(), 'IGLESIAS',      'church',      '#B3202B', 1),
    (uuid_generar_v7(), 'MUSEOS',        'landmark',    '#24406E', 2),
    (uuid_generar_v7(), 'MIRADORES',     'mountain',    '#46704F', 3),
    (uuid_generar_v7(), 'SITIOS_ARQUEOLOGICOS', 'ruins', '#C0703A', 4),
    (uuid_generar_v7(), 'PLAZAS',        'trees',       '#46704F', 5),
    (uuid_generar_v7(), 'ARTESANIA',     'palette',     '#C0703A', 6),
    (uuid_generar_v7(), 'GASTRONOMIA',   'utensils',    '#B3202B', 7),
    (uuid_generar_v7(), 'NATURALEZA',    'leaf',        '#46704F', 8);

INSERT INTO categoria_lugar_traduccion (categoria_lugar_id, idioma, nombre)
SELECT c.id, t.idioma, t.nombre
FROM (VALUES
    ('IGLESIAS', 'es', 'Iglesias y templos'),            ('IGLESIAS', 'en', 'Churches and temples'),
    ('MUSEOS', 'es', 'Museos'),                          ('MUSEOS', 'en', 'Museums'),
    ('MIRADORES', 'es', 'Miradores'),                    ('MIRADORES', 'en', 'Viewpoints'),
    ('SITIOS_ARQUEOLOGICOS', 'es', 'Sitios arqueologicos'), ('SITIOS_ARQUEOLOGICOS', 'en', 'Archaeological sites'),
    ('PLAZAS', 'es', 'Plazas y parques'),                ('PLAZAS', 'en', 'Squares and parks'),
    ('ARTESANIA', 'es', 'Artesania'),                    ('ARTESANIA', 'en', 'Handicrafts'),
    ('GASTRONOMIA', 'es', 'Gastronomia'),                ('GASTRONOMIA', 'en', 'Gastronomy'),
    ('NATURALEZA', 'es', 'Naturaleza'),                  ('NATURALEZA', 'en', 'Nature')
) AS t(codigo, idioma, nombre)
JOIN categoria_lugar c ON c.codigo = t.codigo;

-- ------------------------------------------------------------
--  Categorias de negocio (7) con traduccion es/en
-- ------------------------------------------------------------
INSERT INTO categoria_negocio (id, codigo, icono, orden) VALUES
    (uuid_generar_v7(), 'RESTAURANTES',  'utensils',  1),
    (uuid_generar_v7(), 'HOSPEDAJES',    'bed',       2),
    (uuid_generar_v7(), 'ARTESANOS',     'hammer',    3),
    (uuid_generar_v7(), 'AGENCIAS',      'map',       4),
    (uuid_generar_v7(), 'TRANSPORTE',    'bus',       5),
    (uuid_generar_v7(), 'CAFETERIAS',    'coffee',    6),
    (uuid_generar_v7(), 'GUIAS',         'user-check',7);

INSERT INTO categoria_negocio_traduccion (categoria_negocio_id, idioma, nombre)
SELECT c.id, t.idioma, t.nombre
FROM (VALUES
    ('RESTAURANTES', 'es', 'Restaurantes'),  ('RESTAURANTES', 'en', 'Restaurants'),
    ('HOSPEDAJES', 'es', 'Hospedajes'),      ('HOSPEDAJES', 'en', 'Accommodation'),
    ('ARTESANOS', 'es', 'Artesanos'),        ('ARTESANOS', 'en', 'Artisans'),
    ('AGENCIAS', 'es', 'Agencias de viaje'), ('AGENCIAS', 'en', 'Travel agencies'),
    ('TRANSPORTE', 'es', 'Transporte'),      ('TRANSPORTE', 'en', 'Transport'),
    ('CAFETERIAS', 'es', 'Cafeterias'),      ('CAFETERIAS', 'en', 'Coffee shops'),
    ('GUIAS', 'es', 'Guias turisticos'),     ('GUIAS', 'en', 'Tour guides')
) AS t(codigo, idioma, nombre)
JOIN categoria_negocio c ON c.codigo = t.codigo;

-- ------------------------------------------------------------
--  Tipos de incidente (7) con traduccion es/en - RF-70
-- ------------------------------------------------------------
INSERT INTO tipo_incidente (id, codigo, icono, color_hex) VALUES
    (uuid_generar_v7(), 'VANDALISMO',        'spray-can',   '#B3202B'),
    (uuid_generar_v7(), 'DETERIORO',         'crack',       '#C0703A'),
    (uuid_generar_v7(), 'CONSTRUCCION_ILEGAL','hard-hat',   '#B3202B'),
    (uuid_generar_v7(), 'BASURA',            'trash',       '#C0703A'),
    (uuid_generar_v7(), 'GRAFITI',           'brush',       '#24406E'),
    (uuid_generar_v7(), 'ROBO_PATRIMONIO',   'shield-alert','#B3202B'),
    (uuid_generar_v7(), 'OTRO',              'alert-circle','#24406E');

INSERT INTO tipo_incidente_traduccion (tipo_incidente_id, idioma, nombre)
SELECT ti.id, t.idioma, t.nombre
FROM (VALUES
    ('VANDALISMO', 'es', 'Vandalismo'),                       ('VANDALISMO', 'en', 'Vandalism'),
    ('DETERIORO', 'es', 'Deterioro estructural'),             ('DETERIORO', 'en', 'Structural decay'),
    ('CONSTRUCCION_ILEGAL', 'es', 'Construccion ilegal'),     ('CONSTRUCCION_ILEGAL', 'en', 'Illegal construction'),
    ('BASURA', 'es', 'Acumulacion de basura'),                ('BASURA', 'en', 'Waste accumulation'),
    ('GRAFITI', 'es', 'Grafiti sobre patrimonio'),            ('GRAFITI', 'en', 'Graffiti on heritage'),
    ('ROBO_PATRIMONIO', 'es', 'Robo de bien patrimonial'),    ('ROBO_PATRIMONIO', 'en', 'Heritage theft'),
    ('OTRO', 'es', 'Otro'),                                   ('OTRO', 'en', 'Other')
) AS t(codigo, idioma, nombre)
JOIN tipo_incidente ti ON ti.codigo = t.codigo;

-- ------------------------------------------------------------
--  Insignias del pasaporte patrimonial (8) - RF-39b
-- ------------------------------------------------------------
--  criterio es JSONB porque cada insignia se gana de una forma
--  distinta. La BD no consulta dentro del JSON: lo interpreta el
--  service de gamificacion (Bloque 7).
-- ------------------------------------------------------------
INSERT INTO insignia (id, codigo, icono, criterio, orden) VALUES
    (uuid_generar_v7(), 'PRIMER_PASO',      'footprints', '{"tipo":"CHECKINS_TOTAL","cantidad":1}', 1),
    (uuid_generar_v7(), 'EXPLORADOR',       'compass',    '{"tipo":"CHECKINS_TOTAL","cantidad":5}', 2),
    (uuid_generar_v7(), 'PEREGRINO',        'church',     '{"tipo":"CHECKINS_CATEGORIA","categoria":"IGLESIAS","cantidad":5}', 3),
    (uuid_generar_v7(), 'HISTORIADOR',      'scroll',     '{"tipo":"CHECKINS_CATEGORIA","categoria":"MUSEOS","cantidad":3}', 4),
    (uuid_generar_v7(), 'RUTA_COMPLETA',    'flag',       '{"tipo":"RUTA_COMPLETADA","cantidad":1}', 5),
    (uuid_generar_v7(), 'CRONISTA',         'pen',        '{"tipo":"RESENAS_PUBLICADAS","cantidad":5}', 6),
    (uuid_generar_v7(), 'FOTOGRAFO',        'camera',     '{"tipo":"FOTOS_APROBADAS","cantidad":5}', 7),
    (uuid_generar_v7(), 'GUARDIAN',         'shield',     '{"tipo":"REPORTES_APROBADOS","cantidad":1}', 8);

INSERT INTO insignia_traduccion (insignia_id, idioma, nombre, descripcion)
SELECT i.id, t.idioma, t.nombre, t.descripcion
FROM (VALUES
    ('PRIMER_PASO', 'es', 'Primer paso', 'Tu primer check-in en un lugar patrimonial'),
    ('PRIMER_PASO', 'en', 'First step', 'Your first check-in at a heritage site'),
    ('EXPLORADOR', 'es', 'Explorador', 'Cinco lugares patrimoniales visitados'),
    ('EXPLORADOR', 'en', 'Explorer', 'Five heritage sites visited'),
    ('PEREGRINO', 'es', 'Peregrino', 'Cinco iglesias o templos visitados'),
    ('PEREGRINO', 'en', 'Pilgrim', 'Five churches or temples visited'),
    ('HISTORIADOR', 'es', 'Historiador', 'Tres museos visitados'),
    ('HISTORIADOR', 'en', 'Historian', 'Three museums visited'),
    ('RUTA_COMPLETA', 'es', 'Ruta completa', 'Una ruta tematica completada de principio a fin'),
    ('RUTA_COMPLETA', 'en', 'Route complete', 'A thematic route completed end to end'),
    ('CRONISTA', 'es', 'Cronista', 'Cinco resenas publicadas'),
    ('CRONISTA', 'en', 'Chronicler', 'Five published reviews'),
    ('FOTOGRAFO', 'es', 'Fotografo', 'Cinco fotos aprobadas por moderacion'),
    ('FOTOGRAFO', 'en', 'Photographer', 'Five photos approved by moderation'),
    ('GUARDIAN', 'es', 'Guardian del patrimonio', 'Un reporte ciudadano aprobado'),
    ('GUARDIAN', 'en', 'Heritage guardian', 'One approved citizen report')
) AS t(codigo, idioma, nombre, descripcion)
JOIN insignia i ON i.codigo = t.codigo;
