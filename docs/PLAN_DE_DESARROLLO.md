**PLAN DE DESARROLLO**

Sistema Web de Turismo Inteligente y Patrimonio Cultural

*Huamanga · Ayacucho · Perú*

**Versión 9.1 — Documento de planificación técnica**

*Tesis: «Aplicación web para la publicación de información del
patrimonio cultural de Ayacucho, 2026»*

Escuela de Posgrado — Pontificia

Ingeniería de Sistemas

Año 2026

**Aplicación WEB (no PWA, sin modo offline)** Stack: Next.js 15 (SSR/SSG/ISR) + Spring Boot 3 + PostgreSQL/PostGIS Base de datos en 3FN estricta · 35 entidades + 1 vista materializada  
---


**Contenido**

*(Para actualizar la numeración de páginas: clic derecho sobre la tabla
→ Actualizar campos.)*

**Nota sobre esta versión (9.1)**

Esta versión revisa integralmente el plan bajo criterio de Ingeniería de
Sistemas, con tres cambios estructurales de fondo respecto a la versión
anterior.

**Cambios estructurales aplicados**

- **Aplicación web tradicional, no PWA.** Se elimina el service worker,
  el manifiesto de instalación, next-pwa y el modo offline (antiguo
  RNF-08). El producto es una web servida por navegador. Conserva diseño
  responsivo mobile-first (esto es solo CSS y no implica ser PWA). Esta
  decisión alinea el entregable de forma literal con el título de la
  tesis, que dice «aplicación web».

- **Se eliminan los cronogramas de sprints y las fases de despliegue por
  sprint.** En su lugar, la sección 8 describe un orden lógico de
  construcción por dependencias (qué debe existir antes de qué), sin
  fechas ni número de iteraciones.

- **Marco alineado al patrimonio.** El alcance de turismo se mantiene
  completo, pero se reencuadra: el patrimonio cultural es el núcleo del
  sistema y las demás capas (clima, agenda, directorio, preservación
  ciudadana, gamificación) son servicios de contextualización, difusión
  y preservación de ese patrimonio, en coherencia con el título de la
  tesis.

**1. Resumen ejecutivo**

El Sistema Web de Turismo Inteligente y Patrimonio Cultural de Huamanga
es una aplicación web mobile-first dirigida a turistas, ciudadanos y
negocios locales de la región Ayacucho. Su núcleo es la publicación
estructurada, multilingüe y auditable de información del patrimonio
cultural: lugares patrimoniales, su historia, imágenes históricas
comparables in situ, rutas temáticas y agenda cultural. Sobre ese núcleo
se construyen capas que contextualizan y preservan el patrimonio:
información climática para planificar la visita, recomendaciones según
hora y clima, estado abierto/cerrado calculado al momento, un directorio
de negocios locales y —como diferenciador central— un módulo ciudadano
de reporte de atentados al patrimonio.

El sistema soporta español e inglés desde su núcleo, con arquitectura
preparada para escalar a más idiomas sin modificar código. Es una
aplicación web tradicional (no PWA): se accede desde el navegador, sin
instalación ni modo offline, en coherencia con el título de la tesis.

A diferencia de los visores institucionales de datos georreferenciados
(como GeoPerú, orientado a funcionarios y planificadores), este sistema
es un producto de experiencia y difusión patrimonial: responde a la
pregunta «¿qué hago hoy, ahora, aquí?» mediante recomendaciones
contextuales, distancias a pie, guía por proximidad y un pasaporte
patrimonial gamificado, con contenido vivo alimentado por la propia
comunidad.

**Cifras clave**

| **Indicador**              | **Valor**                                                                                                       |
|----------------------------|-----------------------------------------------------------------------------------------------------------------|
| Naturaleza del producto    | Aplicación web tradicional (SSR/SSG/ISR), no PWA, sin offline                                                   |
| Requisitos Funcionales     | 76 RF (68 del alcance base + 8 RF de experiencia turística)                                                     |
| Requisitos No Funcionales  | 30 RNF con métricas objetivas (se retira el RNF de modo offline)                                                |
| Módulos activos            | 11 módulos del ERS                                                                                              |
| Roles del sistema          | 4 roles diferenciados                                                                                           |
| Idiomas en lanzamiento     | Español (es-PE) e Inglés                                                                                        |
| Costo mensual de operación | 0 USD objetivo (ver riesgo de hosting, sección 7)                                                               |
| Entidades de base de datos | 35 entidades + 1 vista materializada; 3FN estricta y BCNF (24 de dominio + 8 de traducción i18n + 3 pivote N:M) |

**Diferenciadores del sistema**

- **Módulo de preservación ciudadana:** reporte de atentados al
  patrimonio con geolocalización, anonimato real (sin persistencia de
  IP) y moderación. Único en el mercado peruano de aplicaciones
  turísticas y directamente ligado al objeto de la tesis.

- **Slider antes/después geolocalizado:** indica el punto exacto de
  captura de la foto histórica («Párate aquí») para comparar el
  patrimonio in situ (rephotography).

- **Recomendación contextual «¿Qué hago ahora?»:** cruza hora, clima y
  estado abierto/cerrado del lugar patrimonial.

- **Pasaporte patrimonial gamificado:** sellos por check-in verificado
  con GPS, insignias y progreso por rutas patrimoniales.

- **Mapa MapLibre GL JS con render vectorial WebGL,** vista 3D inclinada
  con edificios extruidos nativos y restricción geográfica a Ayacucho.

- **Tema oscuro completo** con paleta inspirada en retablos ayacuchanos
  e internacionalización nativa con SSR para indexación SEO.

- **Arquitectura empresarial Spring Boot,** diferenciada de la mayoría
  de tesis MERN/SPA puras.

**2. Stack tecnológico consolidado**

Tecnologías seleccionadas bajo tres criterios: capacidad de cumplir los
RNF, valor de mercado para el currículum del egresado y costo cero
durante el desarrollo académico. Respecto a versiones anteriores se
retira todo lo relativo a PWA (next-pwa, service worker, manifiesto de
instalación) por la decisión de construir una web tradicional.

**2.1 Frontend**

| **Capa**             | **Tecnología**                | **Función**                                               |
|----------------------|-------------------------------|-----------------------------------------------------------|
| Framework            | Next.js 15 (App Router)       | SSR/SSG/CSR híbrido, ISR con revalidación bajo demanda    |
| Lenguaje             | TypeScript 5                  | Tipado estricto end-to-end                                |
| Estilos              | Tailwind CSS v4 + tokens.css  | Utility-first, design tokens centralizados                |
| Componentes UI       | shadcn/ui + Radix UI          | Componentes accesibles y personalizables                  |
| Mapas                | MapLibre GL JS + react-map-gl | Mapas vectoriales WebGL, vista 3D                         |
| Tiles del mapa       | MapTiler (free tier)          | Tiles vectoriales gratuitos hasta 100K/mes                |
| Internacionalización | next-intl                     | i18n nativo con soporte SSR                               |
| Estado del servidor  | TanStack Query                | Cache, refetch, sincronización automática                 |
| Estado del cliente   | Zustand                       | Estado global minimalista (usuario, tema, idioma, fechas) |
| Formularios          | React Hook Form + Zod         | Validación tipada, performance optimizada                 |

**Retirado del frontend** Se elimina next-pwa y toda la capa de Progressive Web App: sin service worker, sin caché offline, sin manifest instalable. La web se sirve directamente desde el navegador. Se conserva únicamente el diseño responsivo mobile-first, que es CSS puro.  
---


**2.2 Backend**

| **Capa**            | **Tecnología**                     | **Función**                                                            |
|---------------------|------------------------------------|------------------------------------------------------------------------|
| Framework           | Spring Boot 3.x                    | API REST, IoC, autoconfiguración                                       |
| Lenguaje            | Java 21 LTS                        | Records, pattern matching, virtual threads                             |
| Seguridad           | Spring Security + JWT              | Autenticación stateless; access en memoria, refresh en cookie httpOnly |
| Hash de contraseñas | BCrypt (cost 12)                   | Cumple RNF-12                                                          |
| ORM                 | Spring Data JPA + Hibernate        | Repositorios, queries derivadas, transacciones                         |
| Validación          | Bean Validation (Jakarta)          | Validación de DTOs con anotaciones                                     |
| Mapeo Entity/DTO    | MapStruct                          | Mappers generados en compilación                                       |
| Jobs programados    | Spring Scheduler + ShedLock        | Tareas periódicas con lock distribuido                                 |
| Resiliencia         | Resilience4j                       | Circuit breaker y fallback para APIs externas (clima)                  |
| Generación UUID v7  | uuid-creator                       | Java 21 no genera UUIDv7 nativo; se genera en backend                  |
| Documentación API   | SpringDoc OpenAPI                  | Swagger UI automática                                                  |
| Migraciones BD      | Flyway                             | Versionado de esquema en migraciones SQL                               |
| Testing             | JUnit 5 + Mockito + Testcontainers | Pruebas unitarias e integración                                        |
| Cobertura           | JaCoCo                             | Cumple RNF-33 (≥ 70%)                                                  |

**2.3 Datos y servicios externos**

| **Servicio**   | **Proveedor**                      | **Función**                                                  |
|----------------|------------------------------------|--------------------------------------------------------------|
| BD producción  | Supabase (PostgreSQL 16 + PostGIS) | BD gestionada, backups, dashboard                            |
| BD local       | PostgreSQL 16 + PostGIS en Docker  | Desarrollo offline, queries en ms                            |
| Caché          | Upstash Redis                      | Caché de clima, rate limiting, anti-spam TTL, locks ShedLock |
| Multimedia     | Cloudinary                         | Imágenes/videos optimizados a WebP                           |
| API de clima   | OpenWeatherMap                     | Clima actual y pronóstico 7 días (con fallback Resilience4j) |
| Tiles del mapa | MapTiler                           | Renderizado vectorial del mapa                               |
| Correos        | SMTP (JavaMail)                    | Notificaciones y alertas                                     |

**Rol de Supabase en la arquitectura** Supabase se emplea estrictamente como PostgreSQL 16 gestionado con PostGIS y backups automáticos. La autenticación se resuelve con Spring Security + JWT, el almacenamiento de medios con Cloudinary y la API con Spring Boot: la lógica de negocio, la seguridad y las transacciones viven en la capa empresarial Java. Esta separación es deliberada y mantiene el sistema portable (RNF-39): la base gestionada es intercambiable por Neon, Railway Postgres o RDS sin tocar el código de aplicación.  
---


**2.4 Hosting y DevOps**

| **Capa**            | **Proveedor**                    | **Costo**                 |
|---------------------|----------------------------------|---------------------------|
| Frontend Next.js    | Vercel (Hobby tier)              | 0 USD                     |
| Backend Spring Boot | Railway (plan B: Fly.io / Koyeb) | 0–5 USD (ver riesgo, 7.6) |
| Base de datos       | Supabase (Free tier)             | 0 USD                     |
| Redis               | Upstash (Free tier)              | 0 USD                     |
| Imágenes/videos     | Cloudinary (Free tier)           | 0 USD                     |
| Tiles del mapa      | MapTiler (Free tier)             | 0 USD                     |
| CI/CD               | GitHub Actions                   | 0 USD                     |
| Repositorio         | GitHub (privado)                 | 0 USD                     |

**3. Roles del sistema**

El sistema define cuatro roles diferenciados. La autorización se
gestiona en el backend mediante Spring Security y se verifica en cada
endpoint protegido conforme al RNF-16.

| **Rol**             | **Descripción**        | **Capacidades principales**                                                                                                                                                                  |
|---------------------|------------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Visitante anónimo   | Turista sin cuenta     | Explorar lugares patrimoniales, búsqueda, mapa, clima, recomendaciones contextuales, agenda cultural, «durante mi visita», directorio básico, reportar incidentes anónimamente               |
| Usuario registrado  | Turista con cuenta     | Todo lo anterior + favoritos, reseñas con calificación, subida de fotos, check-in, pasaporte patrimonial con insignias, reporte de contenido inapropiado                                     |
| Negocio / Proveedor | Dueño de negocio local | Panel propio con perfil editable (descripción traducible), contacto por WhatsApp, visualización en directorio                                                                                |
| Administrador       | Único gestor interno   | CRUD completo de lugares patrimoniales (descripciones, precios, horarios estructurados), eventos y rutas; moderación de fotos, reseñas, reportes ciudadanos y negocios; métricas y auditoría |

**Decisión de gestión de contenido** Todo el contenido de dominio (descripciones patrimoniales, historias, precios de entrada, horarios, datos prácticos) se gestiona exclusivamente desde el panel de administración y se persiste en la base de datos. No se hardcodea en el frontend. El rendimiento de contenido «estático» se logra en la capa de entrega (ISR de Next.js + caché Redis), no acoplando datos a la presentación. Ver secciones 5.5 y 10.  
---


**4. Alcance del sistema**

El sistema comprende 76 Requisitos Funcionales (68 del alcance base más
8 RF de experiencia turística) y 30 Requisitos No Funcionales,
distribuidos en 11 módulos del ERS. Respecto a la versión PWA se retira
el requisito de modo offline (antiguo RNF-08) por la decisión de web
tradicional. El alcance maximiza valor demostrable manteniendo los
diferenciadores patrimoniales del proyecto.

**4.1 Requisitos Funcionales por módulo**

**Módulo 1 — Descubrimiento y búsqueda (7 RF)**

| **ID** | **Nombre**                                                                                                      | **Prioridad** |
|--------|-----------------------------------------------------------------------------------------------------------------|---------------|
| RF-01  | Listado de lugares paginado                                                                                     | Alta          |
| RF-02  | Búsqueda por texto con debounce 300 ms                                                                          | Alta          |
| RF-04  | Filtrado por categoría                                                                                          | Alta          |
| RF-05  | Filtros combinados (categoría + calificación + distancia)                                                       | Media         |
| RF-06  | Ranking «Más visitados» y «Mejor valorados»                                                                     | Alta          |
| RF-07  | Modo explorar cercano con GPS                                                                                   | Media         |
| RF-08  | NUEVO — Recomendación contextual «¿Qué hago ahora?»: motor de reglas hora + clima + categoría + abierto/cerrado | Alta          |

**Módulo 2 — Detalle de lugar (9 RF)**

| **ID** | **Nombre**                                                                                    | **Prioridad** |
|--------|-----------------------------------------------------------------------------------------------|---------------|
| RF-09  | Ficha completa del lugar patrimonial                                                          | Alta          |
| RF-09b | NUEVO — Estado «abierto/cerrado ahora» calculado sobre HorarioLugar                           | Alta          |
| RF-09c | NUEVO — Duración de visita + distancia/tiempo a pie (ST_Distance PostGIS, ajuste 2 760 msnm)  | Alta          |
| RF-09d | NUEVO — Bloque «Antes de ir»: tarjeta, baños, accesibilidad, apto niños, costo taxi, consejos | Media         |
| RF-10  | Galería inmersiva con swipe y zoom                                                            | Alta          |
| RF-11  | Slider antes/después histórico                                                                | Alta          |
| RF-11b | NUEVO — Slider geolocalizado «Párate aquí» (radio 50 m abre pantalla completa)                | Media         |
| RF-12  | Videos de festividades                                                                        | Alta          |
| RF-15  | Compartir lugar (URL, QR, share nativo)                                                       | Media         |

**Módulo 3 — Mapa interactivo (8 RF)**

| **ID** | **Nombre**                                                                                              | **Prioridad** |
|--------|---------------------------------------------------------------------------------------------------------|---------------|
| RF-17  | Mapa MapLibre, vista 3D inclinada (pitch 55° escritorio / 45° móvil), edificios extruidos, toggle 2D/3D | Alta          |
| RF-18  | Clusters automáticos de marcadores                                                                      | Alta          |
| RF-19  | Ubicación GPS del usuario en tiempo real                                                                | Alta          |
| RF-19b | NUEVO — Modo «Estoy aquí»: auto-guía por proximidad, banner al entrar en radio 50 m                     | Media         |
| RF-20  | Rutas temáticas como polilíneas                                                                         | Alta          |
| RF-21  | Deep link a Google Maps/Waze                                                                            | Alta          |
| RF-22  | Toggles de categorías sobre el mapa                                                                     | Media         |
| RF-22b | Restricción geográfica a Ayacucho (front + back)                                                        | Alta          |

**Módulo 4 — Clima inteligente (4 RF)**

| **ID** | **Nombre**                                                  | **Prioridad** |
|--------|-------------------------------------------------------------|---------------|
| RF-25  | Clima actual con caché Redis 30 min y fallback Resilience4j | Alta          |
| RF-26  | Pronóstico 7 días                                           | Alta          |
| RF-27  | Consejos automáticos según clima                            | Alta          |
| RF-29  | Planificador de visitas por fecha                           | Media         |

**Módulo 5 — Usuarios y comunidad (9 RF)**

| **ID** | **Nombre**                                                                              | **Prioridad** |
|--------|-----------------------------------------------------------------------------------------|---------------|
| RF-31  | Registro con email + contraseña validada                                                | Alta          |
| RF-32  | Login JWT 24h + refresh token 7d                                                        | Alta          |
| RF-34  | Navegación anónima permitida                                                            | Alta          |
| RF-35  | Lista de favoritos persistente                                                          | Alta          |
| RF-37  | Reseñas (500 chars) + calificación 1-5                                                  | Alta          |
| RF-38  | Subida de fotos (5 max, estado pendiente)                                               | Alta          |
| RF-39  | Check-in en lugar con GPS                                                               | Media         |
| RF-39b | NUEVO — Pasaporte patrimonial gamificado: sellos, insignias, progreso por ruta, diploma | Media         |
| RF-45  | Reporte de contenido (3 reportes = revisión)                                            | Media         |

**Módulo 6 — Panel de administración (9 RF)**

| **ID** | **Nombre**                                                                                         | **Prioridad** |
|--------|----------------------------------------------------------------------------------------------------|---------------|
| RF-47  | CRUD de lugares: descripciones e historia (ES/EN), precio, horarios estructurados, datos prácticos | Alta          |
| RF-48  | Gestión de multimedia con Cloudinary (incluye punto de captura de imágenes históricas)             | Alta          |
| RF-49  | Moderación de fotos pendientes                                                                     | Alta          |
| RF-50  | Moderación de reseñas                                                                              | Alta          |
| RF-51  | Gestión de usuarios y roles                                                                        | Alta          |
| RF-52  | Dashboard de métricas con Chart.js                                                                 | Alta          |
| RF-52b | Analítica de tráfico: visitas por página y por negocio                                             | Media         |
| RF-53  | CRUD de rutas temáticas                                                                            | Media         |
| RF-56  | Registro de actividad / audit log                                                                  | Media         |

**Módulo 7 — Multilenguaje i18n (6 RF)**

| **ID** | **Nombre**                                                    | **Prioridad** |
|--------|---------------------------------------------------------------|---------------|
| RF-59  | Cambio de idioma inmediato sin recarga                        | Alta          |
| RF-60  | Español (es-PE) + Inglés en el alcance                        | Alta          |
| RF-63  | Persistencia de idioma en localStorage                        | Alta          |
| RF-64  | Detección automática del navegador (ES/EN/FR/DE con fallback) | Media         |
| RF-66  | UI 100% traducida (cero hardcoded)                            | Alta          |
| RF-67  | Formato local con Intl API                                    | Media         |

**Módulo 8 — Preservación ciudadana (6 RF) — DIFERENCIADOR**

| **ID** | **Nombre**                                   | **Prioridad** |
|--------|----------------------------------------------|---------------|
| RF-69  | Formulario de reporte completable en \< 60 s | Alta          |
| RF-70  | 7 tipos de incidente predefinidos            | Alta          |
| RF-71  | Geolocalización GPS + pin ajustable          | Alta          |
| RF-72  | Reporte anónimo opcional                     | Alta          |
| RF-74  | Mapa público de incidentes aprobados         | Alta          |
| RF-76  | Moderación de reportes por admin             | Alta          |

**Módulo 9 — Agenda cultural (7 RF)**

| **ID** | **Nombre**                                                                                       | **Prioridad** |
|--------|--------------------------------------------------------------------------------------------------|---------------|
| RF-79  | Calendario interactivo mensual                                                                   | Alta          |
| RF-80  | Ficha completa de evento                                                                         | Alta          |
| RF-84  | «Próximos eventos» en Home con countdown                                                         | Alta          |
| RF-84b | NUEVO — Vista «Durante mi visita»: fechas de viaje (localStorage) + eventos coincidentes + clima | Media         |
| RF-85  | Filtro de eventos por tipo                                                                       | Media         |
| RF-86  | CRUD de eventos con clonado anual                                                                | Alta          |
| RF-88  | Clima pronosticado del evento                                                                    | Media         |

**Módulo 10 — Identidad visual (6 RF)**

| **ID** | **Nombre**                                         | **Prioridad** |
|--------|----------------------------------------------------|---------------|
| RF-89  | Design tokens centralizados (cero hex hardcodeado) | Alta          |
| RF-90  | Paleta Ayacucho (5 colores oficiales)              | Alta          |
| RF-91  | Tipografía dual Inter + Playfair Display           | Alta          |
| RF-94  | Tema oscuro completo                               | Alta          |
| RF-95  | Microinteracciones (\< 16 ms feedback)             | Media         |
| RF-96  | Skeleton loaders (CLS \< 0.1)                      | Alta          |

**Módulo 13 — Directorio básico de negocios (4 RF)**

| **ID** | **Nombre**                                       | **Prioridad** |
|--------|--------------------------------------------------|---------------|
| RF-104 | Registro de negocio (pendiente hasta aprobación) | Alta          |
| RF-105 | Listado público de negocios aprobados            | Alta          |
| RF-107 | Panel propio del negocio (perfil básico)         | Media         |
| RF-110 | Botón de contacto WhatsApp                       | Alta          |

**Extra — Accesibilidad mínima (1 RF)**

| **ID** | **Nombre**                         | **Prioridad** |
|--------|------------------------------------|---------------|
| RF-100 | CSS lógico para soporte RTL futuro | Media         |

**RF retirado respecto a la versión PWA** Se elimina el antiguo requisito de instalación PWA / manifiesto. El modo offline (que dependía del service worker) desaparece del alcance. La galería, favoritos y demás funcionalidades siguen disponibles, pero requieren conexión, como cualquier web tradicional.  
---


**4.2 Gestión de alcance por prioridad (MoSCoW)**

Para proteger la fecha de sustentación, el alcance se clasifica
explícitamente. Un recorte ejecutado con este criterio documentado es
una decisión de gestión, no un incumplimiento. Al no haber sprints, la
revisión de prioridad se hace de forma continua contra el avance real.

**Categoría** | **Contenido** | **Regla**  
---|---|---  
MUST (núcleo intocable) | Módulos 1, 2 (sin RF-12), 3, 5 (sin RF-39/39b), 6 (CRUD + moderación), 7 (es/en), 8 completo (diferenciador), 10 (tokens + dark mode), RF-08, RF-09b, RF-09c | Nunca se recorta  
SHOULD (alto valor) | Módulo 4 (clima), Módulo 9 (agenda + RF-84b), Módulo 13 (negocios), RF-11, RF-52, RF-09d, RF-19b, RF-11b | Se recorta solo en riesgo crítico  
COULD (válvulas de escape) | RF-29, RF-39 + RF-39b, RF-52b, RF-64 (fr/de), RF-88, RF-12, RF-95, RF-53 (dejar como seed) | Primeros candidatos a recorte ante atraso


**4.3 Alcance excluido (documentado)**

- Sin pagos ni reservas: el directorio conecta por WhatsApp; la
  transacción ocurre fuera del sistema.

- Sin marketplace ni cupones geolocalizados (fases futuras del ERS).

- **Sin app nativa iOS/Android y sin PWA:** es una aplicación web
  tradicional accesible por navegador, por decisión deliberada y en
  coherencia con el título de la tesis.

- **Sin modo offline:** al no ser PWA, no hay service worker ni caché de
  contenido en el dispositivo.

- Sin chat interno ni notificaciones push. Sin login social (solo
  email + contraseña).

- Sin tracking en tiempo real ni navegación turn-by-turn: se delega a
  Google Maps/Waze.

- Francés y alemán se detectan pero no se traducen (fallback a inglés).

- Contenido solo de Huamanga, aunque el modelo soporte las 11
  provincias.

**Mapa título ↔ alcance (para la defensa)**

Frente a un jurado que exija coherencia estricta con el título
«publicación de información del patrimonio cultural», el alcance se
ordena así:

| **Relación con el título**             | **Módulos**                                                                                                                        |
|----------------------------------------|------------------------------------------------------------------------------------------------------------------------------------|
| Núcleo patrimonial directo             | M1 Descubrimiento, M2 Detalle de lugar patrimonial, M3 Mapa, M7 i18n, M8 Preservación ciudadana, M9 Agenda cultural, M10 Identidad |
| Capa de contextualización de la visita | M4 Clima, RF-08 recomendaciones, RF-09b/c/d datos prácticos                                                                        |
| Capa de comunidad y difusión           | M5 Usuarios/reseñas/fotos, RF-39b pasaporte                                                                                        |
| Capa de ecosistema local (extensión)   | M13 Directorio de negocios                                                                                                         |

**4.4 Requisitos No Funcionales (30 RNF)**

**Rendimiento (6 RNF)**

| **ID** | **Descripción y métrica**                          |
|--------|----------------------------------------------------|
| RNF-01 | First Contentful Paint \< 3 s en 4G (Lighthouse)   |
| RNF-02 | Tiempo de respuesta API: P95 \< 500 ms (JMeter)    |
| RNF-03 | Imágenes WebP \< 200 KB, LCP \< 2.5 s              |
| RNF-04 | Mapa \< 2 s y FPS \> 30 con 200 marcadores activos |
| RNF-05 | Cambio de idioma \< 100 ms (archivos pre-cargados) |
| RNF-06 | Bundle de traducción \< 50 KB por idioma           |

**Disponibilidad y confiabilidad (2 RNF)**

| **ID** | **Descripción y métrica**                                                           |
|--------|-------------------------------------------------------------------------------------|
| RNF-07 | Uptime mensual ≥ 99% (UptimeRobot), sujeto a condiciones del hosting gratuito (7.6) |
| RNF-09 | Backups diarios con retención 30 días (Supabase)                                    |

*Retirado: el antiguo RNF de modo offline vía Service Worker ya no
aplica (web tradicional).*

**Seguridad (7 RNF)**

| **ID** | **Descripción y métrica**                                                                         |
|--------|---------------------------------------------------------------------------------------------------|
| RNF-11 | HTTPS obligatorio con TLS válido, sin contenido mixto                                             |
| RNF-12 | Contraseñas con BCrypt cost 12, nunca en texto plano                                              |
| RNF-13 | Protección SQL injection vía JPA parametrizado                                                    |
| RNF-14 | Rate limiting 100 req/min por IP real (X-Forwarded-For tras proxy confiable; HTTP 429 al superar) |
| RNF-15 | Validación MIME real y tamaño \< 5 MB en fotos                                                    |
| RNF-16 | JWT + validación de rol en cada endpoint protegido                                                |
| RNF-17 | Secrets en .env, nunca en historial de Git                                                        |

**Usabilidad y accesibilidad (8 RNF)**

| **ID** | **Descripción y métrica**                                             |
|--------|-----------------------------------------------------------------------|
| RNF-19 | Tap targets ≥ 44×44 px (guías Apple/Google)                           |
| RNF-20 | Responsive 320 px (iPhone SE) a 1920 px (full HD)                     |
| RNF-21 | WCAG 2.1 AA: contraste 4.5:1, navegación por teclado, Lighthouse ≥ 90 |
| RNF-22 | Máximo 3 toques desde búsqueda hasta detalle de un lugar              |
| RNF-23 | Mensajes de error claros en español, sin códigos técnicos             |
| RNF-24 | CLS \< 0.1 (Core Web Vitals) con skeleton + font-display: swap        |
| RNF-25 | Transiciones entre pantallas \< 200 ms sin flash blanco               |
| RNF-26 | Cobertura i18n 100% (i18next-parser en CI)                            |

**Compatibilidad, escalabilidad y mantenibilidad (7 RNF)**

| **ID** | **Descripción y métrica**                                                                         |
|--------|---------------------------------------------------------------------------------------------------|
| RNF-27 | Últimas 2 versiones de Chrome, Firefox, Safari iOS y Edge                                         |
| RNF-28 | API versionada bajo /api/v1/                                                                      |
| RNF-30 | Índices JPA + GIST PostGIS, EXPLAIN sin Seq Scan en consultas principales                         |
| RNF-31 | Arquitectura por capas: controller/service/repository/entity/dto/mapper                           |
| RNF-32 | README.md completo: un dev nuevo corre el proyecto sin asistencia                                 |
| RNF-33 | Cobertura de tests unitarios ≥ 70% (JaCoCo)                                                       |
| RNF-39 | Arquitectura portable cloud-agnostic: contenedor Docker configurado 100% por variables de entorno |

**5. Arquitectura del sistema**

El sistema sigue una arquitectura de cinco capas con frontend y backend
desacoplados, comunicándose mediante API REST sobre HTTPS con
autenticación JWT. Cada componente puede desplegarse y escalarse de
forma independiente.

**5.1 Vista general**

| **Capa**           | **Componente principal**           | **Tecnologías**                          |
|--------------------|------------------------------------|------------------------------------------|
| Cliente            | Navegador (móvil/escritorio) + CDN | Web tradicional, Vercel Edge             |
| Presentación       | Aplicación Next.js                 | React Server Components, App Router, ISR |
| Aplicación         | API Spring Boot                    | REST /api/v1, Spring Security, JPA       |
| Datos              | PostgreSQL, Redis, Cloudinary      | PostGIS, caché, CDN de medios            |
| Servicios externos | OpenWeatherMap, MapTiler, SMTP     | APIs de terceros con circuit breaker     |

*Nota: la capa Cliente ya no incluye Service Worker (se retiró la PWA).*

**5.2 Arquitectura del backend Spring Boot**

Backend organizado en capas con responsabilidad única: Controller recibe
la petición, Service ejecuta lógica de negocio, Repository accede a
datos, Entity refleja la BD; la respuesta sube convertida en DTO por el
Mapper. Capas transversales: Config, Exception (@ControllerAdvice),
Security (filtros JWT, BCrypt), Validation (validadores custom, p. ej.
coordenadas dentro de Ayacucho), Integration (Cloudinary,
OpenWeatherMap, mail), Audit (@EntityListeners, RF-56) y Util.

Organización por feature, no por capa: paquetes por dominio funcional
bajo com.huamanga.tourism {common, lugar, horario, usuario, reporte,
evento, resena, foto, ruta, negocio, insignia, auth, admin, clima,
recomendacion}. Cada paquete contiene su Controller, Service,
Repository, Mapper, dominio y dto.

**5.3 Seguridad de sesión: patrón de tokens**

- **Access token JWT (24 h):** vive únicamente en memoria (Zustand, sin
  persistir). Se envía por header Authorization. Al recargar, se
  recupera silenciosamente con el refresh token.

- **Refresh token (7 d):** cookie httpOnly + Secure + SameSite=Lax,
  inaccesible desde JavaScript (mitiga XSS). Hasheado en BD con rotación
  en cada renovación.

- **Este reparto evita la contradicción clásica:** si la cookie fuera
  legible por Zustand sería vulnerable a XSS; si el access token se
  persistiera en localStorage, también. Cada token vive donde su riesgo
  es menor.

**5.4 Resiliencia y jobs distribuidos**

- **Resilience4j en clientes de APIs externas:** si OpenWeatherMap
  falla, el circuit breaker corta y el sistema sirve el último clima
  cacheado en Redis con aviso de antigüedad. El Home nunca se rompe por
  un tercero caído.

- **ShedLock (lock distribuido sobre Redis)** en todos los jobs de
  Spring Scheduler (refresh de la vista materializada, limpieza de
  tokens). Garantiza ejecución única aunque existan N instancias del
  backend (RNF-39).

**5.5 Gestión de contenido: panel admin + ISR**

Todo el contenido de dominio (descripciones patrimoniales, historias,
precios, horarios, datos prácticos, eventos, negocios) se crea y edita
desde el panel de administración y se persiste en la base de datos, con
traducción por registro y auditoría (RF-56). Se descarta hardcodear este
contenido en el frontend: obligaría a un despliegue por cada cambio
editorial, eliminaría el CRUD del admin (RF-47), rompería el audit log y
el cálculo de «abierto ahora» (RF-09b), y violaría la separación de
capas.

El rendimiento de contenido «estático» se obtiene en la capa de entrega:
ISR de Next.js (revalidate por tiempo + revalidación bajo demanda vía
webhook disparado por el backend al guardar) hace que la mayoría de
visitas se sirvan como HTML pre-generado desde el CDN de Vercel sin
tocar la BD, mientras el admin sigue editando en segundos sin
despliegues. Los textos de interfaz (botones, menús, mensajes) sí viven
en el frontend (es.json/en.json de next-intl), porque son parte del
software, no del contenido.

**5.6 Arquitectura del frontend Next.js**

| **Capa**          | **Carpeta**           | **Función**                                                            |
|-------------------|-----------------------|------------------------------------------------------------------------|
| Rutas             | src/app/\[locale\]/   | App Router con prefijo de idioma                                       |
| Componentes UI    | src/components/       | shadcn/ui + propios                                                    |
| Lógica de cliente | src/lib/              | Clientes HTTP, validación Zod, utilidades                              |
| Hooks custom      | src/hooks/            | useAuth, useGeolocation, useDebounce, useTheme, useProximidad (RF-19b) |
| Estado global     | src/stores/           | Zustand: usuario, tema, favoritos, fechas de viaje (RF-84b)            |
| Tipos             | src/types/            | TypeScript types compartidos                                           |
| Estilos           | src/styles/tokens.css | Design tokens centralizados (RF-89, RF-90)                             |
| Traducciones      | src/locales/          | es.json y en.json (RF-60, RF-66)                                       |
| Middleware        | src/middleware.ts     | Autenticación de rutas + routing por locale                            |

*Nota: se retira la carpeta/configuración de PWA (manifest, sw). Ya no
hay caché de favoritos offline en Zustand; los favoritos se leen del
backend.*

**6. Modelo de datos**

La base de datos PostgreSQL contiene 35 entidades organizadas en diez
grupos funcionales, más una vista materializada para agregados. El
diseño cumple estrictamente 3FN y BCNF. Convenciones globales: UUID v7
como PK (generado en backend con uuid-creator), auditoría explícita en
todas las tablas, soft delete, estados como enums con CHECK,
multi-idioma vía tablas de traducción, coordenadas PostGIS y jerarquía
geográfica Provincia/Distrito para escalar de Huamanga a toda la región
sin cambios estructurales.

**6.1 Cumplimiento de Formas Normales**

| **Forma** | **Estado** | **Justificación**                                                                                                                                                                                                                                                            |
|-----------|------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| 1FN       | Cumple     | Atributos de dominio atómicos; horarios en filas de HorarioLugar, no columnas por día; PK en todas las tablas. Los campos JSONB (criterio de insignia, detalles de auditoría) son valores de configuración/log opacos al modelo, no datos relacionales de dominio (ver 6.6). |
| 2FN       | Cumple     | Sin dependencias parciales: en PKs compuestas todo atributo no-clave depende de la PK completa.                                                                                                                                                                              |
| 3FN       | Cumple     | Sin dependencias transitivas; cero atributos derivados en tablas base (promedios y progreso se calculan).                                                                                                                                                                    |
| BCNF      | Cumple     | Toda dependencia funcional X→Y tiene X como superclave. Sin anomalías de actualización.                                                                                                                                                                                      |

Nota sobre el Grupo 8 (analítica): VisitaResumenDiario y
VisitaNegocioDiario son tablas de hechos agregadas por día (patrón data
warehouse). Sus contadores no derivan de otra tabla porque los eventos
crudos no se persisten por diseño (privacidad y volumen): la tabla de
hechos ES la fuente primaria, por lo que cumple 3FN. Defensa en 10.3.

**6.2 Entidades por grupo (35)**

**Desglose del conteo (35 entidades)** Las 35 entidades se componen de: 24 entidades de dominio propiamente dichas, 8 tablas de traducción (patrón i18n, una por cada entidad con contenido multilingüe) y 3 tablas pivote N:M puras (Favorito, LugarRuta, InsigniaUsuario). Este desglose es deliberado y defendible: cada tabla de traducción existe para que agregar un idioma sea un INSERT y no un ALTER TABLE, y cada pivote modela una relación muchos-a-muchos con integridad referencial nativa.  
---


**Grupo 0 — Jerarquía geográfica (2)**

- **Provincia:** id (PK), codigo (UK), nombre, orden. Las 11 provincias
  de Ayacucho.

- **Distrito:** id (PK), provincia_id (FK), codigo (UK), nombre. La
  provincia se deriva del distrito. Nombres oficiales: no requieren
  traducción.

**Grupo 1 — Núcleo de usuarios (3)**

- **Usuario:** id, email (UK), password_hash, nombre, rol_id (FK),
  estado, created_at, updated_at, deleted_at.

- **Rol:** id, nombre (UK), descripcion. Valores: VISITANTE, USUARIO,
  NEGOCIO, ADMIN.

- **RefreshToken:** id, usuario_id (FK), token_hash (UK), expira_en,
  created_at.

**Grupo 2 — Lugares y su categorización (6)**

- **Lugar:** id, slug (UK), categoria_lugar_id (FK), distrito_id (FK),
  ubicacion (POINT PostGIS), direccion, telefono, precio_entrada_pen,
  duracion_visita_min, acepta_tarjeta, tiene_banos,
  accesible_silla_ruedas, apto_ninos, costo_taxi_desde_plaza_pen,
  requiere_guia (nullables), estado, created_by, updated_by, created_at,
  updated_at, deleted_at. Sin horario en texto libre.

- **HorarioLugar:** id (PK), lugar_id (FK), dia_semana (0-6),
  hora_apertura, hora_cierre, cerrado, created_at, updated_at,
  updated_by. Relación 1:N; soporta múltiples turnos por día. Habilita
  RF-09b, RF-08 y RF-29.

- **LugarTraduccion:** lugar_id (PK_FK), idioma (PK), nombre,
  descripcion, historia, consejos, updated_by, created_at, updated_at.

- **LugarImagenHistorica:** id, lugar_id (FK), titulo, url_historica,
  public_id_historica, anio_historico, url_actual, public_id_actual,
  credito_historico, punto_captura (GEOGRAPHY POINT, nullable), orden,
  created_at, updated_at.

- **CategoriaLugar:** id, codigo (UK), icono, color_hex, orden. 8
  valores (IGLESIAS, MIRADORES, ...).

- **CategoriaLugarTraduccion:** categoria_lugar_id (PK_FK), idioma (PK),
  nombre, auditoría.

**Grupo 3 — Contenido generado por usuarios (5)**

- **Resena:** id, usuario_id (FK), lugar_id (FK), calificacion (1-5),
  comentario (≤500), estado, auditoría. UNIQUE(usuario_id, lugar_id).

- **Foto:** id, usuario_id, lugar_id, cloudinary_url,
  cloudinary_public_id, estado, motivo_rechazo, auditoría.

- **Favorito:** usuario_id (PK_FK), lugar_id (PK_FK), created_at. PK
  compuesta.

- **CheckIn:** id, usuario_id, lugar_id, ubicacion_gps (POINT),
  created_at. Alimenta el pasaporte (RF-39b).

- **ReporteContenido:** id, usuario_id (FK), foto_id (FK nullable),
  resena_id (FK nullable), motivo, created_at. CHECK exactamente una FK
  no nula; UNIQUE(usuario_id, foto_id) y UNIQUE(usuario_id, resena_id).
  ON DELETE CASCADE nativo, evitando FK polimórfica.

**Grupo 4 — Agenda cultural (2)**

- **Evento:** id, lugar_id (FK opcional), distrito_id (FK), tipo,
  fecha_inicio, fecha_fin, cloudinary_url_portada, recurrente_anual,
  estado, auditoría completa.

- **EventoTraduccion:** evento_id (PK_FK), idioma (PK), nombre,
  descripcion, organizador, auditoría.

**Grupo 5 — Preservación ciudadana (4)**

- **Reporte:** id, tipo_incidente_id (FK), usuario_id (FK nullable),
  nombre_reportante, descripcion, ubicacion (POINT),
  direccion_referencial, estado, notas_admin, es_anonimo, auditoría. NO
  almacena IP ni hash de IP; el anti-spam vive en Redis con TTL 24 h.

- **FotoReporte:** id, reporte_id (FK), cloudinary_url,
  cloudinary_public_id, orden, created_at.

- **TipoIncidente:** id, codigo (UK), icono, color_hex. 7 valores
  (VANDALISMO, ...).

- **TipoIncidenteTraduccion:** tipo_incidente_id (PK_FK), idioma (PK),
  nombre, auditoría.

**Grupo 6 — Rutas temáticas (3)**

- **RutaTematica:** id, slug (UK), color_hex, icono, activa, orden,
  auditoría completa.

- **RutaTraduccion:** ruta_tematica_id (PK_FK), idioma (PK), nombre,
  descripcion, auditoría.

- **LugarRuta:** ruta_tematica_id (PK_FK), lugar_id (PK_FK), orden.
  Pivot N:M con orden de visita.

**Grupo 7 — Directorio de negocios y auditoría (5)**

- **CategoriaNegocio:** id, codigo (UK), icono, orden. 7 valores,
  independiente de CategoriaLugar.

- **CategoriaNegocioTraduccion:** categoria_negocio_id (PK_FK), idioma
  (PK), nombre, auditoría.

- **Negocio:** id, usuario_id (FK gestor), categoria_negocio_id (FK),
  distrito_id (FK), nombre, ruc, telefono, whatsapp, direccion,
  ubicacion (POINT), horario_texto (informativo, ver 6.6), estado,
  auditoría. La descripción vive en NegocioTraduccion.

- **NegocioTraduccion:** negocio_id (PK_FK), idioma (PK), descripcion,
  auditoría. Español obligatorio, inglés opcional.

- **RegistroActividad:** id, usuario_id (FK), accion, entidad,
  entidad_id, detalles (JSONB), ip, created_at. Log inmutable del admin;
  aquí la IP sí se guarda por ser auditoría interna.

**Grupo 8 — Analítica de tráfico (2)**

- **VisitaResumenDiario:** id, tipo_pagina (ENUM), fecha (DATE),
  total_visitas, visitas_unicas, auditoría. UNIQUE(tipo_pagina, fecha).

- **VisitaNegocioDiario:** id, negocio_id (FK), fecha (DATE),
  total_visitas, clics_whatsapp, clics_como_llegar, auditoría.
  UNIQUE(negocio_id, fecha). UPSERT con throttling en frontend.

**Grupo 9 — Gamificación (3)**

- **Insignia:** id (PK), codigo (UK), icono, criterio (JSONB), orden,
  auditoría. El criterio declara la regla de obtención.

- **InsigniaTraduccion:** insignia_id (PK_FK), idioma (PK), nombre,
  descripcion, auditoría.

- **InsigniaUsuario:** usuario_id (PK_FK), insignia_id (PK_FK),
  obtenida_en. PK compuesta. El progreso por ruta NO se almacena: se
  calcula con COUNT sobre CheckIn × LugarRuta.

**6.3 Vista materializada para agregados**

Las consultas de ranking (RF-06) requieren agregados sobre Resena,
CheckIn y Favorito. Para mantener las tablas base en 3FN sin sacrificar
performance, se define la vista materializada EstadisticaLugar,
refrescada cada 5 minutos por Spring Scheduler con lock ShedLock, usando
REFRESH MATERIALIZED VIEW CONCURRENTLY (exige índice único sobre
lugar_id).

La calificación promedio de un lugar se lee SIEMPRE de esta vista: no
existe trigger ni columna de promedio en Lugar. Para dashboards en
tiempo real, un endpoint separado calcula en vivo con caché Redis de 30
segundos.

**Definición (resumen)** SELECT l.id, COALESCE(r.calificacion_promedio,0), COALESCE(r.total_resenas,0), COALESCE(c.total_visitas,0), COALESCE(f.total_favoritos,0), NOW() FROM lugar l LEFT JOIN (subquery reseñas publicadas) r LEFT JOIN (subquery checkins) c LEFT JOIN (subquery favoritos) f WHERE l.deleted_at IS NULL; CREATE UNIQUE INDEX idx_estadistica_lugar_pk ON estadistica_lugar(lugar_id);  
---


**6.4 Índices críticos (cumple RNF-30)**

| **Tabla**         | **Índice**                               | **Tipo**       | **Propósito**                            |
|-------------------|------------------------------------------|----------------|------------------------------------------|
| Lugar             | idx_lugar_categoria                      | BTREE          | Filtro por categoría (RF-04)             |
| Lugar             | idx_lugar_distrito                       | BTREE          | Filtro por distrito/provincia            |
| Lugar             | idx_lugar_ubicacion                      | GIST           | Búsqueda por distancia (RF-07, RF-09c)   |
| Lugar             | idx_lugar_estado_partial                 | BTREE          | WHERE deleted_at IS NULL                 |
| HorarioLugar      | idx_horario_lugar_dia                    | BTREE          | Cálculo «abierto ahora» (RF-09b)         |
| Distrito          | idx_distrito_provincia                   | BTREE          | Listar distritos de una provincia        |
| EstadisticaLugar  | idx_estadistica_lugar_pk                 | UNIQUE         | Obligatorio para REFRESH CONCURRENTLY    |
| EstadisticaLugar  | idx_estadistica_calificacion / \_visitas | BTREE          | Rankings (RF-06)                         |
| LugarTraduccion   | idx_lugartrad_fulltext                   | GIN            | Full-text search (RF-02)                 |
| Resena            | idx_resena_lugar / idx_resena_unique     | BTREE/UNIQUE   | Listado y una reseña por usuario+lugar   |
| Foto              | idx_foto_pendientes                      | BTREE parcial  | WHERE estado='PENDIENTE' (RF-49)         |
| ReporteContenido  | idx_reporte_foto/\_resena_unique         | UNIQUE parcial | Un reporte por usuario por contenido     |
| Reporte           | idx_reporte_ubicacion / \_estado         | GIST/BTREE     | Mapa de incidentes y moderación          |
| Evento            | idx_evento_fecha                         | BTREE          | Calendario y «durante mi visita»         |
| CheckIn           | idx_checkin_usuario_lugar                | BTREE          | Pasaporte y progreso de rutas (RF-39b)   |
| InsigniaUsuario   | PK compuesta                             | UNIQUE         | Una insignia por usuario                 |
| Visita\*Diario    | idx\_\*\_unique                          | UNIQUE         | Un registro por dimensión+fecha (RF-52b) |
| RefreshToken      | idx_refresh_expira                       | BTREE          | Limpieza programada                      |
| RegistroActividad | idx_actividad_created                    | BTREE          | Listado cronológico admin                |

**6.5 Constraints CHECK y UNIQUE aplicados**

- **Resena:** CHECK (calificacion BETWEEN 1 AND 5); UNIQUE (usuario_id,
  lugar_id); CHECK (LENGTH(comentario) ≤ 500); estado IN
  ('PUBLICADA','OCULTA','ELIMINADA','EN_REVISION').

- **Foto:** estado IN
  ('PENDIENTE','APROBADA','RECHAZADA','EN_REVISION').

- **Favorito e InsigniaUsuario:** PRIMARY KEY compuesta, previene
  duplicados.

- **ReporteContenido:** CHECK ((foto_id IS NOT NULL AND resena_id IS
  NULL) OR (foto_id IS NULL AND resena_id IS NOT NULL)); UNIQUE
  parciales por (usuario_id, foto_id) y (usuario_id, resena_id).

- **HorarioLugar:** CHECK (dia_semana BETWEEN 0 AND 6); CHECK (cerrado =
  TRUE OR hora_apertura \< hora_cierre).

- **Lugar:** CHECK bounds de Ayacucho ST_X entre -75.5 y -73.0, ST_Y
  entre -15.5 y -12.5 (RF-22b backend).

- **Reporte:** estado IN
  ('RECIBIDO','EN_REVISION','APROBADO','DESCARTADO','RESUELTO').

- **Usuario:** CHECK regex de email a nivel BD.

- **LugarImagenHistorica:** CHECK (anio_historico BETWEEN 1500 AND 1990)
  como límite fijo inmutable en BD; la regla dinámica «al menos 50 años»
  se valida en Bean Validation (evita CURRENT_DATE en CHECK, función no
  inmutable).

**6.6 Decisiones de diseño defendibles**

**¿Por qué CategoriaLugar y CategoriaNegocio separadas?**

Entidades semánticamente distintas con valores disjuntos; unificarlas
introduciría una dependencia funcional débil que viola la cohesión del
dominio.

**¿Por qué dos FKs nullables en ReporteContenido?**

La FK polimórfica es un anti-patrón: PostgreSQL no valida integridad
hacia dos tablas desde una columna. Dos FKs con CHECK preservan ON
DELETE CASCADE nativo.

**¿Por qué cero persistencia de IP en reportes?**

Incluso hasheada, una IP es reversible por fuerza bruta (espacio IPv4 de
~4.3 mil millones). El anonimato se cumple con privacidad por diseño:
contador volátil en Redis con TTL 24 h.

**¿Por qué EstadisticaLugar como vista materializada?**

Almacenar promedios en Lugar violaría 3FN (dependencia transitiva). La
vista es un objeto independiente, técnica reconocida (Stonebraker,
Date).

**¿Por qué HorarioLugar en vez de horario como texto?**

El texto libre no es computable: impide calcular «abierto ahora»
(RF-09b), recomendaciones (RF-08) y planificador (RF-29). Filas por
día/turno cumplen 1FN y habilitan tres features con una entidad.

**¿Por qué el contenido editorial vive en BD y no en el frontend?**

Es contenido de dominio gestionado por el admin, auditable y traducible
por registro. El rendimiento estático se logra con ISR + Redis en la
capa de entrega (5.5), no acoplando datos a la presentación.

**¿Por qué el progreso de rutas e insignias no se almacena como
contador?**

Sería un atributo derivado (viola 3FN); se calcula con COUNT sobre
CheckIn × LugarRuta con índices apropiados. Solo se persiste el hecho
inmutable: la insignia obtenida y su fecha.

**¿Por qué jerarquía Provincia/Distrito si el foco es solo Huamanga?**

Escalar a las 11 provincias solo requiere insertar registros; añadir la
jerarquía después obligaría a un ALTER TABLE riesgoso.

**¿Por qué el horario del lugar se normaliza (HorarioLugar) pero el del
negocio es texto libre?**

Es una decisión basada en el USO del dato, no una inconsistencia. El
horario del lugar patrimonial alimenta lógica computable: «abierto
ahora» (RF-09b), recomendaciones (RF-08) y planificador (RF-29); por eso
debe estar normalizado en filas por día y turno. El horario del negocio
es puramente informativo: se muestra al turista pero el sistema no
calcula nada con él (el directorio no tiene «abierto ahora» ni filtro
por horario). Normalizarlo sería sobre-ingeniería: añadir una tabla, sus
constraints y su CRUD para una funcionalidad que no existe en el
alcance. Se normaliza cuando el dato es computable; se deja como texto
cuando es solo informativo.

**¿Los campos JSONB (Insignia.criterio, RegistroActividad.detalles) no
violan 1FN?**

No. La normalización aplica a los datos relacionales del dominio, no a
un valor de configuración o a un log de auditoría. Insignia.criterio es
una regla de obtención opaca al modelo: la BD nunca hace JOIN ni
consulta lógica de negocio dentro del JSON; lo interpreta el service.
RegistroActividad.detalles registra estructuras heterogéneas de
auditoría por diseño flexible. Usar JSONB para configuración y logs es
una práctica reconocida en PostgreSQL y no compromete la 3FN de las
tablas de dominio.

**Lugar tiene distrito_id y coordenadas: ¿el distrito no se deriva de la
ubicación (dependencia transitiva)?**

No. Son datos capturados independientemente, no uno derivado del otro.
Las coordenadas son un punto geográfico exacto; el distrito es una
asignación administrativa que puede decidirse manualmente (un lugar en
un límite distrital, o una dirección postal que no coincide con el
polígono). No existe una dependencia funcional que obligue a calcular
uno desde el otro, por lo que no hay violación de 3FN.

**7. Hosting y estrategia de despliegue**

Esta sección conserva la topología de ambientes y la estrategia técnica
de despliegue, pero se retira toda referencia a un calendario por
sprints. El despliegue continuo (cada push a main despliega
automáticamente) se mantiene como práctica, no como fase datada.

**7.1 Topología de ambientes**

| **Componente** | **Local (desarrollo)**           | **Producción (cloud)**            |
|----------------|----------------------------------|-----------------------------------|
| PostgreSQL     | Docker (5432)                    | Supabase (gestionado)             |
| Redis          | Docker (6379)                    | Upstash (serverless)              |
| Backend        | IDE (mvnw spring-boot:run)       | Railway (auto-deploy git push)    |
| Frontend       | npm run dev (3000)               | Vercel (auto-deploy git push)     |
| Cloudinary     | Mismo CDN                        | Mismo CDN (free tier)             |
| Configuración  | application-dev.yml + .env.local | Variables en panel Railway/Vercel |

**7.2 Docker local + Supabase en producción**

- **Velocidad:** queries locales en ~5 ms vs 80-200 ms remotas. Trabajo
  offline del desarrollador en zonas con red limitada.

- **Conservación del free tier y aislamiento** (DROP DATABASE local sin
  riesgo). Mismo esquema vía Flyway.

**7.3 Despliegue temprano del esqueleto**

Desde el inicio se despliega un «Hello World» funcional en todos los
proveedores para descubrir problemas de configuración (CORS, variables,
cold starts, certificados) cuando son baratos de resolver. Cada push a
main despliega automáticamente frontend y backend. Esta es una práctica
permanente, sin asociarla a un número de iteración.

**7.4 Práctica de CI/CD (sin fases datadas)**

El pipeline se enriquece a medida que el proyecto madura, no en fechas
fijas. El orden recomendado de incorporación es:

1.  Build + auto-deploy en push (desde el primer día).

2.  Tests unitarios obligatorios en cada Pull Request, en cuanto exista
    la primera capa de servicios.

3.  Lint + format + reporte de cobertura en PR, cuando el código base
    justifique control de calidad.

4.  Ambiente staging separado, cuando se necesite validar antes de
    producción.

5.  Producción solo desde main; staging desde develop, como política
    estable.

**7.5 Estrategia de escalabilidad en dos fases**

Fase 1 (gratuita): Vercel (CDN + ISR), Railway con 1 instancia Docker
stateless, Supabase con Pgbouncer, Upstash Redis, Cloudinary. Toda la
ingeniería de rendimiento se implementa ya en esta fase. Fase 2
(elástica): por diseño cloud-agnostic (RNF-39), migrar consiste en
redirigir variables de entorno: backend a AWS Fargate o Google Cloud Run
con auto-escalado, BD a RDS con réplicas de lectura, caché a
ElastiCache. Se descarta AWS Lambda puro porque el arranque de Spring
Boot penaliza los cold starts. Gracias a ShedLock, los jobs siguen
ejecutándose una sola vez con N instancias.

Cuatro capas de rendimiento: (1) caché agresivo CDN/ISR + Redis: la
mayoría de peticiones nunca llega a la BD; (2) backend stateless con
virtual threads de Java 21; (3) connection pooling HikariCP + Pgbouncer;
(4) índices estratégicos + vista materializada (RNF-02, P95 \< 500 ms).

**7.6 Riesgo de hosting y plan B (transparencia)**

Las condiciones de los planes gratuitos cambian con frecuencia: el
crédito mensual de Railway puede no cubrir un contenedor Spring Boot
24/7 (consumo típico 300-500 MB RAM), y Supabase free puede pausar
proyectos inactivos. Mitigación: (a) verificar condiciones vigentes
periódicamente; (b) plan B documentado y probado en Fly.io o Koyeb
(mismo contenedor, solo cambian variables, gracias a RNF-39); (c)
presupuesto de contingencia de 5 USD/mes declarado honestamente; (d)
UptimeRobot con ping periódico para evitar pausas por inactividad. El
RNF-07 (uptime 99%) se declara sujeto a estas condiciones.

**7.7 Pruebas de carga: política**

Las pruebas JMeter (carga base de 50 usuarios y pico de Semana Santa con
rampa a 500) se ejecutan contra el ambiente Docker local o un staging
temporal, nunca contra los free tiers de producción, para no agotar
cuotas ni infringir términos de uso. Los resultados se documentan
evidenciando el efecto de la caché (RNF-39).

**8. Orden lógico de construcción**

A pedido, se retira el calendario de 15 sprints y las fases datadas. En
su lugar, esta sección ordena la construcción por dependencias técnicas:
qué debe existir antes de qué. No hay fechas ni número de iteraciones;
el ritmo lo define el avance real. La regla es simple: no se empieza un
bloque hasta que sus prerrequisitos estén terminados y demostrables.

**8.1 Cadena de dependencias (de la base a la superficie)**

**Bloque 0 — Cimientos del proyecto**

**Requiere:** *nada*

- Monorepo con apps/web (Next.js 15 + TS) y apps/api (Spring Boot 3 +
  Java 21).

- Docker Compose local (PostgreSQL 16 + PostGIS + Redis). Cuentas en los
  proveedores.

- Tailwind v4 + tokens.css con paleta Ayacucho. Endpoint /api/v1/health.

- Deploy automático a Vercel y Railway; verificación del plan B
  (Fly.io). README inicial.

**Bloque 1 — Modelo de datos y migraciones**

**Requiere:** *Bloque 0*

- 35 entidades JPA (incluye HorarioLugar, Insignia, InsigniaTraduccion,
  InsigniaUsuario) + vista materializada EstadisticaLugar con refresh
  cada 5 min (Scheduler + ShedLock).

- Migraciones Flyway (esquema, índices, vista, constraints, seeds de
  roles, categorías, tipos de incidente, provincias/distritos e
  insignias).

- UUID v7 generado en backend con uuid-creator. Tests de repositorio con
  Testcontainers.

- Seed inicial: 1 admin, 11 provincias con distritos, 8 categorías de
  lugar, 7 de negocio, 7 tipos de incidente, 8 insignias, 5 lugares demo
  con traducciones y horarios.

- Verificación EXPLAIN ANALYZE sin Seq Scan en filtros indexados.

**Bloque 2 — Autenticación y seguridad**

**Requiere:** *Bloque 1*

- Endpoints /api/v1/auth: register, login, refresh, me, logout. Spring
  Security + @PreAuthorize.

- BCrypt cost 12 (RNF-12). Refresh tokens hasheados con rotación.

- Access token solo en memoria (Zustand sin persistencia); refresh en
  cookie httpOnly + Secure + SameSite. Middleware Next.js protege
  /perfil y /admin.

- Rate limiting con lectura validada de X-Forwarded-For. Tests: 401
  token inválido, 403 rol insuficiente.

**Bloque 3 — i18n base + CRUD de lugares (backend)**

**Requiere:** *Bloque 2*

- next-intl con rutas \[locale\]/ y detección es/en/fr/de con fallback.
  Switcher con persistencia (RF-63).

- Endpoints /api/v1/lugares: CRUD completo con horarios estructurados,
  precio, duración y datos prácticos (RF-09d). DTOs + MapStruct + Bean
  Validation.

- Validación de coordenadas en bounds de Ayacucho (RF-22b). Swagger UI.
  Tests de servicio ≥ 70%.

- Webhook de revalidación ISR: al guardar un lugar, el backend dispara
  la revalidación de su página en Vercel (5.5).

**Bloque 4 — Listado, detalle y búsqueda (frontend)**

**Requiere:** *Bloque 3*

- /lugares con paginación, skeletons, ordenamiento (RF-01, RF-96).
  LugarCard con badge abierto/cerrado (RF-09b) y «a X min caminando»
  (RF-09c).

- Búsqueda con debounce (RF-02), filtros por categoría y combinados
  (RF-04, RF-05), rankings (RF-06).

- Ficha /lugares/\[slug\] con galería inmersiva (RF-09, RF-10) y bloque
  «Antes de ir» (RF-09d).

- Páginas servidas con ISR + revalidación bajo demanda. TanStack Query
  con caché 5 min.

**Bloque 5 — Mapa, clima, recomendaciones y proximidad**

**Requiere:** *Bloque 4*

- MapLibre + tiles MapTiler con vista 3D inclinada (RF-17): edificios
  extruidos con fill-extrusion nativo, toggle 2D/3D animado. Sin
  three.js. Preserva RNF-04.

- Marcadores por categoría, clusters (RF-18), bounds Ayacucho (RF-22b),
  GPS (RF-19), deep links (RF-21), toggles (RF-22), rutas como
  polilíneas (RF-20).

- OpenWeatherMap con caché Redis 30 min + circuit breaker Resilience4j
  (RF-25); pronóstico y consejos (RF-26, RF-27); planificador (RF-29).

- RF-08: RecomendacionService (hora + clima + abierto/cerrado +
  categoría). RF-19b: hook useProximidad con Haversine, banner a \< 50
  m, supresión de 2 h.

**Bloque 6 — Reseñas, calificaciones y fotos**

**Requiere:** *Bloque 4*

- CRUD de reseñas con UNIQUE (usuario_id, lugar_id) y CHECK 1-5 (RF-37).

- La calificación promedio se lee exclusivamente de EstadisticaLugar;
  sin trigger ni columna de promedio en Lugar (coherente con 3FN).

- Cloudinary con upload firmado desde backend; validación MIME y 5 MB
  (RNF-15); hasta 5 fotos PENDIENTE (RF-38); galería pública solo
  APROBADAS.

- Bandejas de moderación de fotos y reseñas en panel admin (RF-49,
  RF-50).

**Bloque 7 — Favoritos, check-in, pasaporte y reportes de contenido**

**Requiere:** *Bloque 6*

- Favoritos con animación (RF-35, RF-95) y /perfil/favoritos. Check-in
  GPS a \< 100 m (RF-39). (Sin caché offline: los favoritos se leen del
  backend.)

- RF-39b: /perfil/pasaporte con sellos, insignias (evaluación en service
  tras check-in, criterio JSONB), progreso por ruta con COUNT y diploma
  compartible.

- ReporteContenido con dos FKs nullables + CHECK + UNIQUE parciales.
  Endpoint POST /api/v1/reportes-contenido (RF-45); al tercer reporte
  único el estado pasa a EN_REVISION; duplicado devuelve 409.

**Bloque 8 — Preservación ciudadana (diferenciador)**

**Requiere:** *Bloque 5*

- Seed de 7 tipos de incidente con traducciones (RF-70). Endpoint
  /api/v1/reportes con hasta 5 fotos.

- Validación de coordenadas dentro de Ayacucho. Anti-spam exclusivamente
  en Redis con TTL 24 h; sin IP ni hash en BD.

- Estados RECIBIDO/EN_REVISION/APROBADO/DESCARTADO/RESUELTO. Página
  /reportar en \< 60 s (RF-69), GPS + pin ajustable (RF-71), anonimato
  opcional (RF-72).

- Mapa público /mapa-incidentes (RF-74) y bandeja de moderación con
  notas internas (RF-76).

**Bloque 9 — Agenda cultural**

**Requiere:** *Bloque 5*

- CRUD de eventos con clonado anual (RF-86). Calendario mensual (RF-79),
  ficha con clima (RF-80, RF-88).

- «Próximos eventos» con countdown (RF-84) y filtros por tipo (RF-85).

- RF-84b: selector de fechas de viaje (localStorage) y vista «Durante mi
  visita» con eventos coincidentes + clima por día.

**Bloque 10 — Panel de administración completo**

**Requiere:** *Bloques 6, 7, 8, 9*

- Dashboard Chart.js (RF-52): totales, gráficos de visitas, categorías,
  registros.

- Analítica (RF-52b): UPSERT diario con throttling; tráfico por sección
  y por negocio.

- Gestión de usuarios y roles (RF-51); CRUD de rutas (RF-53); audit log
  vía @EntityListeners (RF-56).

**Bloque 11 — Directorio de negocios, slider geolocalizado y compartir**

**Requiere:** *Bloques 5, 7*

- Registro de negocio (RF-104), aprobación admin, listado público
  (RF-105), panel propio (RF-107), WhatsApp con mensaje predefinido
  (RF-110).

- Slider antes/después (RF-11) + modo geolocalizado «Párate aquí»
  (RF-11b): reusa useProximidad.

- Compartir con URL/QR/share nativo (RF-15). Videos vía YouTube embed
  (RF-12).

**Bloque 12 — Identidad visual final y cierre de alcance**

**Requiere:** *todos los anteriores*

- Dark mode completo (RF-94), 0 hex hardcodeados, tipografía dual
  (RF-91), microinteracciones (RF-95), skeletons con CLS \< 0.1 (RF-96).

- Revisión formal de alcance (MoSCoW): lo que no esté terminado y sea
  COULD se recorta y documenta. Después de este punto no se agregan
  features.

**Bloque 13 — Pulido: performance, SEO, accesibilidad y testing**

**Requiere:** *Bloque 12*

- Lighthouse ≥ 90 en 5 páginas críticas. WebP + lazy loading + srcset
  (RNF-03). sitemap.xml dinámico, robots.txt, JSON-LD
  (TouristAttraction, Event), Open Graph.

- Pruebas JMeter contra local/staging (política 7.7): carga base 50
  usuarios (P95 \< 500 ms) y pico con rampa a 500; documentar efecto de
  caché.

- Auditoría WCAG 2.1 AA; CSS lógico (RF-100); cobertura JaCoCo ≥ 70%
  (RNF-33); E2E Playwright en flujos críticos; verificación
  cross-browser.

**Bloque 14 — Documentación, datos reales y sustentación**

**Requiere:** *Bloque 13*

- README profesional; /docs con arquitectura, decisiones, modelo, API
  reference.

- Carga real: 50+ lugares patrimoniales con fotos, traducciones y
  horarios verificados; 30+ eventos del año.

- Onboarding visual; /acerca-del-proyecto; slides; video demo 3-5 min;
  paper IEEE; guion de defensa (sección 10); ensayos cronometrados;
  backup y entornos congelados.

**Regla de oro sin sprints** No se avanza a un bloque cuyo prerrequisito no esté terminado y demostrable. Cada bloque cerrado debe dejar algo mostrable (una pantalla, un endpoint en Swagger, una migración que corre). Si un bloque se atrasa, se recorta primero un elemento COULD (4.2), se documenta la decisión, y se continúa. No hay «sprints internos» sin valor visible.  
---


**9. Convenciones y reglas de oro**

**9.1 Reglas de oro**

- Cada bloque de construcción deja algo demostrable. No hay trabajo sin
  valor visible.

- No saltar dependencias: lo pendiente de un bloque anterior se completa
  antes de seguir.

- Ante atraso, se recorta primero un elemento COULD documentado (4.2).

- Cerrar el alcance en el Bloque 12. La documentación y datos reales
  (Bloque 14) no introducen código nuevo de features.

- Todo cambio va en rama + Pull Request + CI. Secrets solo en .env
  locales y paneles de proveedores.

- Cada feature incluye sus tests; la cobertura se mantiene desde el
  principio, no se arregla al final.

- Conviene grabar un clip corto de cada avance: materia prima del video
  final y evidencia de progreso.

**9.2 Convenciones de código**

- **Backend:** paquetes por feature; clases PascalCase; DTOs con sufijo
  Request/Response; métodos camelCase con verbos; constantes MAYÚSCULAS;
  endpoints REST en plural minúsculas; cero SQL concatenado;
  @Transactional explícito en services.

- **Frontend:** componentes PascalCase; hooks con prefijo use;
  TypeScript estricto; cero textos hardcodeados (todo por t('clave'));
  cero hex en componentes (solo tokens.css); Server Components por
  defecto, 'use client' solo cuando se necesita estado, eventos o APIs
  del navegador.

**9.3 Convenciones de Git**

main protegido (deploy a producción); develop (staging);
feature/{RF}-descripcion; fix/descripcion; Conventional Commits en
español (feat:, fix:, docs:, refactor:, test:); PRs obligatorios. Al no
haber sprints, se retiran los tags por sprint; se recomienda etiquetar
por hito (p. ej. modelo-datos, auth, mapa, cierre-alcance).

**10. Cómo defender el proyecto ante el jurado**

Preguntas probables del jurado con respuestas blindadas, coherentes con
las secciones 5 y 6, e incluyendo las nuevas relativas a web tradicional
y al título de la tesis.

**10.1 Sobre el título y el alcance**

**Tu título dice «aplicación web», ¿por qué no una PWA o una app
móvil?**

Precisamente por fidelidad al título y por simplicidad de sustentación.
Es una aplicación web tradicional servida por navegador, con SSR/SSG/ISR
para rendimiento y SEO. Se descartó la PWA (service worker, instalación,
offline) para no ampliar el alcance más allá de lo que el título declara
y reducir la superficie de fallo el día de la defensa.

**El título habla de «patrimonio cultural», pero tu sistema tiene clima,
negocios y gamificación. ¿No se sale del tema?**

El patrimonio cultural es el núcleo: publicación estructurada,
multilingüe y auditable de lugares patrimoniales, su historia, imágenes
históricas comparables in situ, rutas y agenda cultural. Las demás capas
existen para servir a ese núcleo: el clima y las recomendaciones
contextualizan la visita al patrimonio; el módulo ciudadano lo preserva;
el directorio conecta el patrimonio con su ecosistema local. Si se
pidiera un recorte estricto al título, el mapa de la sección 4.3 muestra
qué es núcleo y qué es extensión.

**¿Por qué tantos RF si el ERS define más?**

El ERS es la visión de largo plazo. Este entregable prioriza valor
demostrable con gestión de alcance explícita (4.2); los módulos
pospuestos (marketplace, tracking, cupones, biometría, white-label)
quedan arquitectónicamente preparados.

**10.2 Sobre el stack**

**¿Por qué Spring Boot y no Node/Express?**

Estándar empresarial en Perú (banca, retail, consultoras); aprovecha la
base Java de la carrera; capas con IoC y transacciones declarativas más
maduras para módulos críticos.

**¿Por qué Next.js y no React puro si es una web tradicional?**

SSR/SSG/ISR en un solo proyecto: SEO indexable para las fichas
patrimoniales, mejor performance en 3G. Una SPA pura no da SEO sin
trabajo adicional. Next.js no obliga a ser PWA; aquí se usa como
framework web tradicional.

**¿Por qué Supabase si usas Spring Boot?**

Se usa Supabase exclusivamente como PostgreSQL 16 gestionado con PostGIS
y backups; su auth, storage y API no se emplean porque esas
responsabilidades son de Spring Security, Cloudinary y Spring Boot. El
objetivo formativo es una capa de negocio empresarial en Java, no un
BaaS. Una alternativa igual de válida sería Neon o Railway Postgres.

**¿Por qué PostgreSQL y no MySQL?**

Búsquedas geoespaciales con PostGIS: índices GIST, ST_DWithin,
ST_Within, validación de coordenadas. Es el estándar en sistemas con
geolocalización.

**¿Por qué MapLibre y no Leaflet o Google Maps?**

Google/Mapbox exigen tarjeta y riesgo de facturación; Leaflet (raster)
no cumple RNF-04 ni ofrece 3D. MapLibre es fork BSD-3 de Mapbox
mantenido por AWS y Meta: WebGL vectorial, 3D, costo cero.

**10.3 Sobre arquitectura y datos**

**¿Cómo separas lógica de negocio y acceso a datos?**

Controller/Service/Repository con responsabilidad única; el Controller
nunca toca el Repository; lógica testeable aislada (RNF-33).

**¿Por qué no exponer entidades en el API?**

Seguridad (no exponer estructura interna ni campos como password_hash) y
desacoplamiento (los DTOs aíslan el contrato); MapStruct convierte en
compilación.

**¿Cómo logras SEO siendo app moderna?**

Server Components con HTML completo, ISR, JSON-LD (TouristAttraction,
Event) y sitemap dinámico. Al ser web tradicional con SSR, el contenido
patrimonial es indexable sin trabajo extra de hidratación.

**¿Tu modelo cumple 3FN?**

Sí: 3FN estricta y BCNF en las 35 entidades (24 de dominio, 8 de
traducción, 3 pivote). Sin atributos derivados ni dependencias
transitivas en tablas base. Los agregados (rankings) viven en
EstadisticaLugar, refrescada cada 5 min con CONCURRENTLY bajo lock
ShedLock. Técnica reconocida (Stonebraker, Date).

**¿Y las tablas de analítica no violan 3FN?**

Son tablas de hechos agregadas por día (patrón data warehouse). Sus
contadores no derivan de otra tabla porque los eventos crudos no se
persisten por diseño: la tabla de hechos es la fuente primaria y sus
atributos dependen únicamente de su clave (dimensión + fecha).

**¿Por qué UUID v7 y no autoincrementales?**

No exponen el tamaño de la BD, son únicos globalmente y la variante v7
es ordenable por tiempo. Se genera en backend con uuid-creator porque ni
Java 21 ni PostgreSQL 16 lo traen nativo.

**¿Cómo escalas si el sistema crece?**

Backend stateless (JWT por petición) que escala horizontalmente; jobs
con ShedLock para que N instancias no dupliquen tareas; Pgbouncer +
HikariCP; Redis para caché y rate limiting; Cloudinary sirve medios
directo al cliente.

**¿Qué pasa si OpenWeatherMap se cae en plena demo?**

Resilience4j abre el circuito y el sistema sirve el último clima
cacheado con aviso de antigüedad: degradación elegante, nunca pantalla
rota.

**10.4 Sobre seguridad y privacidad**

**¿SQL injection?**

Exclusivamente JPA parametrizado; cero concatenación; verificado en code
review.

**¿Dónde viven los tokens?**

Access token en memoria; refresh token en cookie httpOnly + Secure +
SameSite, hasheado en BD con rotación. Ni XSS puede robar el refresh ni
el access se persiste.

**¿El reporte anónimo es realmente anónimo?**

Sí: cero persistencia de IP, ni siquiera hasheada (reversible por fuerza
bruta en el espacio IPv4). El anti-spam es un contador volátil en Redis
con TTL 24 h que se autodestruye. Privacidad por diseño.

**10.5 Diferenciación frente a GeoPerú**

GeoPerú es la plataforma nacional de datos georreferenciados del Estado,
orientada a funcionarios y planificadores para identificar brechas y
hacer seguimiento territorial: un visor de capas oficiales. Este sistema
es un producto de experiencia y difusión patrimonial para el turista de
a pie: responde «¿qué hago hoy, ahora, aquí?» con recomendaciones
contextuales, estado abierto/cerrado, distancias a pie, guía por
proximidad, pasaporte gamificado y contenido vivo de la comunidad
(reseñas, fotos, reportes).

Además invierte la dirección del dato: mientras GeoPerú va del Estado al
ciudadano, el módulo de preservación va del ciudadano al Estado. Como
trabajo futuro, la capa de incidentes aprobados podría exponerse como
servicio georreferenciado consumible por entidades públicas: el sistema
se convierte en fuente ciudadana de datos de preservación del patrimonio
que el Estado hoy no tiene.

**11. Anexos**

**11.1 Setup inicial del proyecto**

1.  Node.js 20+ y pnpm 9+.

2.  Java 21 (Temurin).

3.  Docker Desktop.

4.  Git configurado.

5.  git clone del repositorio.

6.  Crear .env.local en apps/web y apps/api desde .env.example.

7.  docker compose up -d.

8.  pnpm install.

9.  pnpm db:migrate.

10. pnpm db:seed.

11. pnpm dev.

12. http://localhost:3000 (frontend) y
    http://localhost:8080/api/v1/swagger-ui (API).

**11.2 Variables de entorno**

**Backend (apps/api/.env.local)**

SPRING_PROFILES_ACTIVE=dev;
DATABASE_URL=jdbc:postgresql://localhost:5432/huamanga; DATABASE_USER;
DATABASE_PASSWORD; REDIS_URL=redis://localhost:6379; JWT_SECRET (openssl
rand -base64 32); JWT_EXPIRATION=86400000;
JWT_REFRESH_EXPIRATION=604800000; COOKIE_SECURE=true; CLOUDINARY_URL;
OPENWEATHER_API_KEY; MAIL_HOST/PORT/USERNAME/PASSWORD;
CORS_ALLOWED_ORIGINS; REVALIDATE_WEBHOOK_URL y REVALIDATE_SECRET
(revalidación ISR, 5.5).

**Frontend (apps/web/.env.local)**

NEXT_PUBLIC_API_URL=http://localhost:8080/api/v1;
NEXT_PUBLIC_MAPTILER_KEY; NEXT_PUBLIC_CLOUDINARY_CLOUD_NAME;
NEXT_PUBLIC_APP_NAME=Turismo Huamanga; NEXT_PUBLIC_DEFAULT_LOCALE=es;
REVALIDATE_SECRET (validación del webhook entrante).

**11.3 Comandos clave**

| **Comando**                              | **Función**                                        |
|------------------------------------------|----------------------------------------------------|
| docker compose up -d / down              | Levanta o detiene PostgreSQL + Redis locales       |
| pnpm dev                                 | Frontend (3000) y backend (8080) en paralelo       |
| pnpm --filter web dev / --filter api dev | Solo frontend / solo backend (invoca ./mvnw)       |
| pnpm db:migrate / db:seed / db:studio    | Migraciones Flyway / datos de prueba / cliente SQL |
| pnpm test / test:coverage / lint / build | Tests, cobertura, linter, build de producción      |
| git push origin main                     | Despliegue automático a producción                 |

**11.4 Documentos de referencia**

ERS del proyecto (visión completa de RF y RNF). Diagramas de
arquitectura y ERD. Estructura del proyecto. Este plan de desarrollo
(versión 9.0).

**12. Registro de cambios de la versión 9.1**

**12.1 Cambios de plataforma y estructura (respecto a la v8.0 con PWA)**

| **\#** | **Cambio**                                                                                                                                                               |
|--------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| 1      | De PWA a aplicación web tradicional: se elimina next-pwa, service worker, manifiesto de instalación y modo offline. Se conserva el diseño responsivo mobile-first (CSS). |
| 2      | Se retira el RNF de modo offline (antiguo RNF-08, dependiente del Service Worker). RNF-08 solo aparece ahora como «retirado».                                            |
| 3      | Se eliminan los 15 sprints y las fases datadas. Se reemplazan por un orden lógico de construcción por dependencias (sección 8).                                          |
| 4      | Se corrigen los datos institucionales: Escuela de Posgrado / Pontificia. Se retira el periodo de fechas.                                                                 |
| 5      | Se reencuadra el alcance con el patrimonio cultural como núcleo, para alinear con el título de la tesis (secciones 1, 4.3, 10.1).                                        |

**12.2 Corrección de cifras (revisión 9.1)**

La versión 8.0 arrastraba cifras que no cuadraban con el contenido
realmente listado (afirmaba haber «unificado cifras» sin lograrlo del
todo). La revisión 9.1 recontó cada elemento uno por uno y unificó los
números en todo el documento:

| **Elemento**              | **Cifra previa (incorrecta)** | **Cifra corregida y verificada**                                                      |
|---------------------------|-------------------------------|---------------------------------------------------------------------------------------|
| Requisitos Funcionales    | 74 / 75 (inconsistente)       | 76 RF (68 base + 8 de experiencia turística), verificado contra las tablas por módulo |
| Entidades de datos        | 31                            | 35 entidades (24 de dominio + 8 de traducción i18n + 3 pivote N:M), sin duplicados    |
| Requisitos No Funcionales | 31 (incluía el offline)       | 30 RNF vigentes (RNF-08 retirado, solo figura en este changelog)                      |

**Recomendación de coherencia documental** Estas cifras finales (76 RF · 35 entidades · 30 RNF) deben ser idénticas en el ERS, en el diagrama entidad-relación (ERD) y en las diapositivas de sustentación. La incoherencia numérica entre documentos es de lo primero que detecta un jurado; unificarla es la corrección más barata y de mayor impacto.  
---


**12.3 Refinamientos de esta versión**

- Rol de Supabase reformulado en positivo (base gestionada
  intercambiable, RNF-39). Sección 2.3.

- Riesgo de free tiers y plan B de hosting documentados en la sección 7
  (7.6).

- Mapa título ↔ alcance (núcleo patrimonial vs. extensiones) para
  navegar el alcance con criterio. Sección 4.3.

- Justificación de Negocio.horario como texto libre informativo
  (decisión basada en el uso del dato, no inconsistencia). Sección 6.6.

- Defensa de los campos JSONB (criterio de insignia, detalles de
  auditoría) frente a 1FN. Secciones 6.1 y 6.6.

- Defensa de Lugar con distrito_id + coordenadas frente a una posible
  objeción de 3FN. Sección 6.6.

- Ajuste de redacción: «abierto/cerrado en tiempo real» → «calculado al
  momento», más preciso técnicamente.

**12.4 Se conserva de la v8.0**

- Los 8 RF de experiencia turística (RF-08, 09b, 09c, 09d, 11b, 19b,
  39b, 84b).

- Modelo de datos con vista materializada, 3FN estricta y BCNF, y todas
  las correcciones de consistencia previas (ReporteContenido con dos
  FKs, calificación solo en vista materializada, anti-spam sin IP, CHECK
  inmutable en imágenes históricas).

- Patrón JWT (access en memoria + refresh en cookie httpOnly), ShedLock,
  Resilience4j, UUID v7 en backend, gestión de contenido admin + ISR.

*Versión 9.1 — 76 RF · 35 entidades · 30 RNF · web tradicional ·
patrimonio como núcleo · sin sprints*

*Documento guía del proyecto de fin de carrera. Sujeto a revisiones
conforme avanza la construcción.*
