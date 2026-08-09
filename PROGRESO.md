# PROGRESO — Yachay Ayacucho

Bitácora de avance del proyecto. Claude Code actualiza este archivo al terminar cada
bloque; el usuario lo lee para retomar entre sesiones. El orden de bloques está en
`docs/PLAN_DE_DESARROLLO.md`, sección 8.

## Estado actual
- **Bloque en curso:** ninguno (Bloque 9 terminado, pendiente de commit del usuario).
- **Próximo bloque:** Bloque 10 — Panel de administración completo.
- **Versiones vigentes:** Spring Boot 4.1.0 · Next.js 16.2.12 · React 19.2.8 · Java 21 · Node 24 ·
  Hibernate 7.4.1 · Flyway 12.4 · Testcontainers 2.0.5.

## Registro de bloques

| Bloque | Estado | Qué se hizo | Commit del usuario |
|--------|--------|-------------|--------------------|
| 0 — Cimientos | ✅ Completado | Monorepo pnpm (apps/web + apps/api), Docker Compose con PostgreSQL 16 + PostGIS 3.5 y Redis 7, `.env` único en la raíz leído por las tres partes, endpoint `/api/v1/health` con comprobación real de BD y caché (9 tests en verde), Tailwind v4 + `tokens.css` con la paleta Ayacucho en OKLCH, capa CSS de "sensación de app nativa", portada SSR que muestra el estado en vivo, Dockerfile del backend y README actualizado. **Migrado después a Spring Boot 4.1.0 y Next.js 16.2.12** (ver sección de migración) | — |
| 1 — Modelo de datos | ✅ Completado | 35 tablas + 1 vista materializada en 14 migraciones Flyway, 36 clases JPA package-by-feature, 35 repositorios, 89 índices (GIST espaciales, GIN full-text, parciales, únicos parciales), todos los CHECK de la sección 6.5, UUID v7 en backend y en SQL, auditoría y soft delete, catálogos de referencia (11 provincias, 119 distritos, 8+7 categorías, 7 tipos de incidente, 8 insignias), seed de demostración idempotente, job de refresco de la vista con ShedLock sobre Redis, y 50 tests contra PostGIS real con Testcontainers | — |
| 2 — Autenticación y seguridad | ✅ Completado | JWT HS256 de 15 min emitido con las clases nativas de Spring Security 7, refresh token de 7 días en cookie httpOnly hasheado con SHA-256, rotación con **detección de reutilización** que revoca todas las sesiones ante un robo (migración V15), BCrypt cost 12, `@PreAuthorize` por rol, errores 401/403 en ProblemDetail, rate limiting en Redis con lectura validada de X-Forwarded-For, y frontend con access token solo en memoria (Zustand sin persist), refresh silencioso y `proxy.ts` protegiendo /perfil y /admin. **37 tests de seguridad**, 87 en total | — |
| 3 — i18n + CRUD lugares (backend) | ✅ Completado | CRUD completo de `/api/v1/lugares` (solo ADMIN escribe) con guardado transaccional de lugar + traducciones + grilla horaria, DTOs con MapStruct, tres validadores propios (bounds de Ayacucho, español obligatorio, horarios sin solapes), `abiertoAhora` calculado en servidor, mensajes de error traducidos por `Accept-Language`, webhook de revalidación ISR disparado **después del commit**, y next-intl con rutas `/es` y `/en`, detección de idioma, selector con persistencia en cookie y textos y mensajes de Zod traducidos. **111 tests** | — |
| 4 — Listado/detalle/búsqueda (frontend) | ✅ Completado | Listado `/lugares` con búsqueda por texto completo (debounce 300 ms), filtros por categoría, filtros combinados, tres órdenes y paginación, **todo reflejado en la URL**; tarjetas con insignia abierto/cerrado calculada en el navegador y distancia a pie tras un gesto explícito; ficha `/lugares/[slug]` renderizada en el servidor con galería Embla (swipe + pinch-zoom), bloque «Antes de ir» y grilla horaria; ISR (listado 5 min, fichas 1 h) con prefetch a TanStack Query e hidratación sin parpadeo; en el backend, endpoint único `explorar` con `to_tsvector` en español, `/categorias`, y estadísticas en el resumen; seed ampliado a 15 lugares. **125 tests** + 23 comprobaciones en navegador real | — |
| 5 — Mapa, clima, recomendaciones, proximidad | ✅ Completado | Mapa MapLibre + MapTiler con vista 3D inclinada y edificios extruidos, clusters en GPU, límites de Ayacucho, GPS, toggles por categoría, 3 rutas temáticas como polilíneas y deep links a Google Maps/Waze/Apple Maps; clima de OpenWeatherMap con **caché de dos niveles** (fresco 30 min + último bueno 24 h) y circuit breaker programático, pronóstico agregado por días y consejos por altitud; `RecomendacionService` que cruza apertura, clima, hora y categoría devolviendo **el motivo de cada sugerencia**; planificador por fecha; `useProximidad` con histéresis 50/80 m y supresión de 2 h. **155 tests** + 22 comprobaciones en navegador real | — |
| 6 — Reseñas, calificaciones, fotos | ✅ Completado | CRUD de reseñas con una sola por persona y lugar, edición y baja lógica **reutilizable**; promedio leído solo de la vista materializada, con **carril rápido de 30 s** para que no tarde 5 min en moverse; subida de fotos a Cloudinary con petición firmada desde el backend, **tres barreras de validación** (tamaño, números mágicos y decodificación real) y transformaciones de entrega `f_auto,q_auto`; galería pública solo con aprobadas; bandejas de moderación de fotos y reseñas en `/admin`, con borrado real del binario **e invalidación del CDN** al rechazar; anti-spam por cuenta en Redis; seed con 6 usuarios y 31 reseñas que hace que los rankings del Bloque 4 por fin ordenen. **185 tests** + 17 comprobaciones en navegador real | — |
| 7 — Favoritos, check-in, pasaporte, reportes | ✅ Completado | Favoritos guardados en el servidor con corazón optimista (RF-35, RF-95) y `/perfil/favoritos`; check-in por GPS validado en el backend con PostGIS sobre `geography`, **cuatro barreras** (radio 150 m, precisión mínima, enfriamiento de 24 h y detección de salto imposible) y el punto enviado guardado para auditoría; motor de insignias que **interpreta el criterio JSONB** y concede **en la misma transacción** que la visita; pasaporte con sellos, las 8 insignias —obtenidas y por obtener— y progreso por ruta **calculado al vuelo**, más diploma compartible; reportes de contenido con XOR de las dos FK y paso a `EN_REVISION` al tercero; seed de 29 check-ins que llena el ranking «más visitados». **210 tests** + 14 comprobaciones en navegador real | — |
| 8 — Preservación ciudadana | ✅ Completado | Denuncia de daños al patrimonio con **anonimato real por diseño**: un `@PrePersist` en la entidad borra `usuario_id`, `nombre_reportante` y la auditoría (`created_by`/`updated_by`) antes de que la fila toque el disco, de modo que la garantía no depende de que nadie olvide un `if`; anti-spam **sin identidad**, con HMAC-SHA256 de la IP bajo una sal aleatoria que vive solo en memoria y rota cada día, guardado únicamente en Redis con TTL de 24 h; las fotos se **recodifican para borrar el EXIF** (y con él el GPS y el modelo del móvil) antes de subirlas; formulario de un solo paso con los 7 tipos como botones, GPS con pin ajustable y anónimo por defecto; validación de coordenadas dentro de Ayacucho; mapa público que solo muestra lo **aprobado o resuelto**; bandeja de moderación con los 5 estados y notas internas que el API nunca devuelve en público; y activación de la insignia `GUARDIAN`, que solo cuenta reportes **identificados**. **225 tests** + 21 comprobaciones en navegador real | — |
| 9 — Agenda cultural | ✅ Completado | Calendario mensual con la rejilla construida sobre `Date.UTC` y **el mes y el filtro en la URL**, de modo que los botones de mes son enlaces y la vista se comparte; ficha de evento con clima; **clonado anual que copia la plantilla pero nunca la fecha vieja** —la Semana Santa es móvil— y nace en BORRADOR, con `evento_origen_id` (migración V16) para no crear gemelos; los eventos de varios días aparecen en todos sus días por una consulta de solape; «próximos eventos» en la portada con cuenta regresiva; «Durante mi visita» con las fechas en cookie leída por el servidor; y clima con **cuatro estados explícitos**, ninguno de ellos un error: pronóstico, temporada cuando aún falta mucho, no disponible y pasado. Todo el manejo de fechas pasa por `lib/fechas.ts`, que fija UTC al formatear. **270 tests** + 23 comprobaciones en navegador real, incluida **la misma fecha vista desde tres husos separados por 25 horas** | — |
| 10 — Panel de administración | Pendiente | — | — |
| 11 — Directorio, slider geolocalizado, compartir | Pendiente | — | — |
| 12 — Identidad visual y cierre de alcance | Pendiente | — | — |
| 13 — Pulido: performance, SEO, accesibilidad, testing | Pendiente | — | — |
| 14 — Documentación, datos reales, sustentación | Pendiente | — | — |

---

## Decisión de versiones (la más importante del Bloque 0)

El `CLAUDE.md` original fijaba Spring Boot 3.x y Next.js 15. Al construir el Bloque 0 se
descubrió que **ambas ramas habían quedado por detrás**: `start.spring.io` ya ni siquiera
ofrece la rama 3.x, y Spring Boot 3.5 está fuera de la ventana de soporte OSS gratuito.

El usuario decidió actualizar el `CLAUDE.md` a **Spring Boot 4.x y Next.js 16**, con este
razonamiento: *el proyecto no es solo la tesis, es un producto real que se mantendrá y
publicará después*, así que debe correr sobre versiones en soporte. Y el momento barato
para migrar es con el proyecto casi vacío, no con 35 entidades y 76 RF encima.

La migración se ejecutó sobre el Bloque 0 ya terminado y **todas las verificaciones
quedaron en verde**.

### Qué cambió en el backend (3.5.16 → 4.1.0)

| Cambio | Detalle |
|---|---|
| `parent` del pom | `3.5.16` → `4.1.0`. Debajo: Spring Framework 7.0.8, Jackson 3.1.4, JUnit Jupiter 6.0.3, Mockito 5.23, Hibernate 7.4.1, Flyway 12.4, Testcontainers 2.0.5 |
| Starter web | `spring-boot-starter-web` → `spring-boot-starter-webmvc`. El primero sigue existiendo pero su propio POM lo declara deprecado |
| Starters de test | Boot 4 partió `spring-boot-starter-test` en starters por módulo. Ahora se usan `-webmvc-test`, `-jdbc-test`, `-data-redis-test`, `-actuator-test` y `-validation-test`. El de webmvc es el que aporta MockMvc y `@WebMvcTest` |
| SpringDoc | `2.8.17` → `3.0.3` (la línea 2.x es para Spring Framework 6) |
| **Cambio de código** | `@WebMvcTest` se movió de `org.springframework.boot.test.autoconfigure.web.servlet` a **`org.springframework.boot.webmvc.test.autoconfigure`**. Fue el único error de compilación de toda la migración, en `HealthControllerTest` |

**Riesgos que NO se materializaron:**
- *SpringDoc 3.0.3* está compilado contra Boot 4.0.5 y corremos 4.1.0, pero funciona:
  `/swagger-ui` y `/api-docs` devuelven 200 y el spec (OpenAPI 3.1.0) describe el
  endpoint con sus tags y metadatos. **No hizo falta el plan B.**
- *Jackson 2 → 3*: la salida JSON es idéntica (`Instant` en ISO-8601, enums como texto).
  El test del controller ya afirmaba la forma exacta del JSON y siguió pasando.
- *JUnit 5 → 6*: los paquetes `org.junit.jupiter.api.*` no cambiaron; el código de test
  compiló sin tocarlo.
- *`org.springframework.lang.NonNull`*: se comprobó listando el JAR de Spring Framework
  7.0.8 que la clase sigue existiendo. `CorsConfig` no se tocó.

### Qué cambió en el frontend (Next 15.5.22 → 16.2.12)

| Cambio | Detalle |
|---|---|
| Versiones | `next` 16.2.12, `react`/`react-dom` 19.2.8, `@types/*` al día |
| **`data-scroll-behavior="smooth"`** | Añadido al `<html>` del layout raíz. Hasta Next 15, Next anulaba el `scroll-behavior: smooth` de nuestro CSS durante los cambios de ruta para que la navegación saltara arriba al instante; **en Next 16 ya no lo hace por defecto**. Sin este atributo, cada navegación haría un scroll animado hasta el inicio: lo contrario de la sensación de app nativa que persigue el proyecto |
| **ESLint** | `eslint.config.mjs` reescrito a *flat config* nativo. La configuración generada por Next 15 usaba el puente `FlatCompat` de `@eslint/eslintrc`, que con `eslint-config-next` 16 **falla** con `TypeError: Converting circular structure to JSON`. Ahora se importan directamente `eslint-config-next/core-web-vitals` y `/typescript`, que ya exportan arrays de flat config. Se eliminó la dependencia `@eslint/eslintrc` |
| Turbopack | Es el bundler por defecto en `dev` y `build` desde Next 16. No hizo falta configurar nada: no teníamos config de webpack (que es lo que haría fallar el build) y Tailwind v4 funciona sin ajustes |
| `tsconfig.json` | Next lo reconfiguró solo: `jsx` a `react-jsx` (runtime automático de React, obligatorio) y `.next/dev/types` añadido a `include` (el dev server ahora escribe en `.next/dev`) |

**Cambios de Next 16 que se revisaron y NO nos afectan:** el renombrado de `middleware` a
`proxy` (no tenemos), la eliminación del acceso síncrono a `params`/`searchParams`/
`cookies`/`headers` (no los usamos), los nuevos defaults de `next/image` (aún no la
usamos — **revisar en el Bloque 6**, sobre todo `qualities` y `minimumCacheTTL`), la
eliminación de `next lint` (ya llamábamos a `eslint` directo), PPR y
`serverRuntimeConfig`.

**Nota sobre el output del build:** Next 16 eliminó las columnas `size` y `First Load JS`
de `next build` por considerarlas imprecisas con React Server Components. La tabla del
build ahora solo lista rutas y su estrategia de render.

---

## Verificaciones ejecutadas (tras la migración)

| Verificación | Resultado |
|---|---|
| `docker compose ps` | Ambos `healthy`. PostgreSQL 16.9, PostGIS 3.5 (`USE_GEOS=1 USE_PROJ=1`), Redis `PONG` |
| `mvnw test` | **BUILD SUCCESS — 9 tests, 0 fallos**, sobre Spring Boot 4.1.0 |
| `GET /api/v1/health` (todo arriba) | **HTTP 200**, `status: UP`, postgresql 1 ms, redis 1 ms |
| `GET /api/v1/health` (Redis apagado) | **HTTP 503**, `status: DOWN`, redis DOWN tras 2007 ms (timeout de 2 s). Al restaurar Redis, vuelve a 200 |
| `/api/v1/swagger-ui` y `/api/v1/api-docs` | HTTP 200 ambos. Spec OpenAPI 3.1.0 correcto |
| `pnpm lint` | Sin errores (tras reescribir la config a flat config) |
| `pnpm --filter web type-check` | Sin errores |
| `pnpm build` | Next.js 16.2.12 (Turbopack), compilado en 2.1 s, ruta `/` dinámica (ƒ) |
| Frontend en `localhost:3000` | HTTP 200. El HTML del SSR trae los datos reales del backend, y el `<html>` sale con `lang="es"`, las variables de fuente y `data-scroll-behavior="smooth"` |
| Cabeceras | Sin `x-powered-by` (`poweredByHeader: false` funcionando) |
| Imagen Docker del backend | Reconstruida sobre Boot 4: 370 MB. Arranca en ~8 s configurada **solo** por variables de entorno, responde HTTP 200 con ambos componentes UP, y corre como usuario `yachay` (uid 100), no root |

---

## Bloque 1 — Modelo de datos

### Verificaciones ejecutadas

| Verificación | Resultado |
|---|---|
| `mvnw test` | **BUILD SUCCESS — 50 tests, 0 fallos** |
| `mvnw verify` (JaCoCo) | **All coverage checks have been met.** 94% de líneas sobre las clases con lógica |
| Mapeo JPA vs. esquema (`ddl-auto=validate`) | Las **36 clases** validan contra el esquema creado por Flyway desde cero |
| Recuento de tablas | **35 tablas** + **1 vista materializada**; 8 de traducción + 3 pivote + 24 de dominio |
| Arranque contra base vacía | Las **14 migraciones** se aplican solas: 35 tablas, 89 índices, vista materializada |
| Catálogos | 4 roles, 11 provincias, **119 distritos**, 8 categorías de lugar, 7 de negocio, 7 tipos de incidente, 8 insignias, 60 traducciones |
| UUID v7 | 164 identificadores de catálogo, todos versión 7 y monótonos en el tiempo |
| Constraints | 11 tests que **intentan violar** cada CHECK/UNIQUE y comprueban que PostgreSQL los rechaza |
| Planes de ejecución (RNF-30) | 9 consultas con 5.000 lugares sintéticos + `ANALYZE`: todas usan índice |
| Vista materializada | `REFRESH CONCURRENTLY` funciona; se comprueba además que **falla sin el índice único** |
| Job programado | Disparó a las 21:05:00 exactas en 8 ms; ShedLock registró `job-lock:yachay:refrescarEstadisticaLugar` en Redis |
| `pnpm db:seed` | Aplica 5 lugares con traducciones, 45 horarios y 1 ruta; **idempotente** (segunda ejecución: 0 filas) |
| `pnpm db:migrate` / `flyway:info` | Schema version 14, todas las migraciones en `Success` |
| `/api/v1/health`, Swagger | HTTP 200 |
| `pnpm lint` / `pnpm build` | Sin errores (frontend intacto) |

### Tres hallazgos que los tests destaparon

1. **El índice GIST sobre `geometry` no sirve para buscar por cercanía en metros.** Las
   consultas de "explorar cerca" (RF-07) y "a X min caminando" (RF-09c) convierten la
   columna con `ubicacion::geography` para medir metros reales, y esa conversión deja
   fuera de juego al índice sobre la geometría pura: PostgreSQL caía en Seq Scan. Se
   añadió `idx_lugar_ubicacion_geog`, un índice funcional sobre la expresión exacta que
   usa la consulta. **La tabla 6.4 del plan debería recoger este índice adicional.**
2. **`color_hex` estaba declarado `CHAR(7)`** y el validador de esquema lo detectó al
   no cuadrar con el mapeo Java. Se cambió a `VARCHAR(7)` en las tres tablas afectadas,
   que además es lo correcto: `CHAR` rellena con espacios hasta la longitud fija.
3. **Spring Boot 4 modularizó la autoconfiguración de Flyway.** Con solo `flyway-core`
   en el classpath las migraciones **no se ejecutan y no avisan**: el esquema
   simplemente no existe. Hay que usar `spring-boot-starter-flyway`.

### Decisiones de modelado

- **36 clases `@Entity`, 35 tablas.** La clase 36 es `EstadisticaLugar`, que mapea la
  vista materializada y se cuenta aparte, tal como declara el plan ("35 entidades + 1
  vista materializada"). Se mapea con `@Subselect` y no como tabla porque una vista
  materializada no aparece como tabla en los metadatos JDBC y la validación de esquema
  la daría por inexistente.
- **UUID v7 por generador de Hibernate, no asignado en el constructor.** Si la entidad
  naciera con el id puesto, Spring Data la trataría como "no nueva" y ejecutaría un
  SELECT antes de cada INSERT. Hay un test que comprueba que el id es null antes de
  persistir.
- **Dos generadores de UUID v7, uno en cada lado.** `uuid-creator` para la aplicación y
  la función `uuid_generar_v7()` para lo que se inserta por SQL (catálogos y seeds).
  Sin la función habría que elegir entre incrustar cientos de UUID literales o usar
  `gen_random_uuid()`, que produce v4 y rompería la convención.
- **Tres `@MappedSuperclass` en vez de uno**, porque las tablas no son homogéneas:
  `EntidadCreacion` (solo `created_at`, para hechos inmutables como el check-in),
  `EntidadBase` (+ `updated_at`) y `EntidadAuditable` (+ `created_by`, `updated_by`,
  `deleted_at`). Las 8 traducciones usan `MarcaTiempoTraduccion`, sin clave subrogada.
- **Soft delete en 6 entidades** (Usuario, Lugar, Evento, RutaTematica, Negocio,
  Reporte) con `@SQLRestriction("deleted_at IS NULL")`, tal como se aprobó. El resto
  modela su ciclo de vida con el campo `estado`.
- **Todas las asociaciones `@ManyToOne` en LAZY explícito.** El default de JPA es EAGER
  y es la causa más frecuente del problema N+1.
- **Marcas de tiempo en `TIMESTAMPTZ` ↔ `Instant`, todo en UTC.** El plan no lo
  especificaba; sin zona horaria, el cálculo de "abierto ahora" (RF-09b) se rompería en
  cuanto el servidor no estuviera en Lima.

### 📌 Correcciones adicionales para los documentos de tesis (las hace el usuario)

Se suman a las ya anotadas más abajo:
- **Sección 5.2 (paquetes):** añadir `geografia`, `favorito`, `checkin`, `moderacion` y
  `analitica` a la lista de paquetes. Meterlos en los existentes rompería la cohesión
  (p. ej. `ReporteContenido`, que es moderación de contenido, no un reporte ciudadano).
- **Sección 6.4 (índices):** añadir `idx_lugar_ubicacion_geog`, el índice funcional
  sobre `ubicacion::geography` sin el cual las búsquedas por cercanía no usan índice.
- **Anexo 11.3 (comandos):** `pnpm db:migrate` es opcional; las migraciones se aplican
  solas al arrancar el backend.

### Pendientes anotados para bloques siguientes

- **Bloque 2:** sustituir el `password_hash` del admin del seed
  (`SIN_HASH_VALIDO_HASTA_BLOQUE_2`) por un BCrypt real de coste 12, y hacer que
  `JpaAuditoriaConfig.auditorActual()` devuelva el usuario del SecurityContext para que
  `created_by`/`updated_by` se rellenen solos.
- **Bloque 5:** revisar la regla del CLAUDE.md sobre `GenericJackson2JsonRedisSerializer`
  bajo Jackson 3 al configurar la caché.
- **Códigos UBIGEO:** los 119 distritos llevan nombres de la división política oficial y
  códigos con la estructura UBIGEO del INEI. Conviene cotejarlos contra el padrón
  vigente antes de la sustentación.
- **Tests de integración con Surefire:** los tests con Testcontainers se ejecutan en la
  fase `test` junto a los unitarios. Si en el Bloque 13 interesa separarlos, habría que
  renombrarlos a `*IT` y añadir Failsafe.

---

## Bloque 9 — Agenda cultural

### Las fechas, que era el encargo delicado

El fallo del Bloque 4 —horarios desplazados cinco horas— tenía una versión peor esperando
en este bloque. `"2027-03-21"` no es un instante: es un día del calendario. Pero
`new Date("2027-03-21")` **sí** crea un instante, el de la medianoche en UTC, y al pintarlo
en la zona del navegador sale el **20** de marzo en cualquier huso al oeste de Greenwich —es
decir, para todo el público de esta aplicación—. En Tokio se vería bien, así que el fallo
habría sobrevivido a cualquier revisión hecha desde Europa o Asia.

Tres decisiones lo cierran:

| Decisión | Qué garantiza |
|---|---|
| En el backend, `LocalDate` de punta a punta y `DATE` en PostgreSQL | Una fecha sin hora no puede desplazarse. Jackson la serializa como `"2027-03-21"` |
| En el frontend, **todo pasa por `lib/fechas.ts`**, que construye la fecha con `Date.UTC` y formatea con `timeZone: "UTC"` | El día que llegó del servidor es el día que se pinta. Ningún componente convierte una fecha por su cuenta |
| `TiempoAyacucho.hoy(clock)` en el backend y `hoyEnAyacucho()` en el frontend | «Hoy» es el día **en Huamanga**, no en UTC ni en la zona de quien mira |

Esa última importa más de lo que parece. La JVM corre en UTC desde el Bloque 4, así que
`LocalDate.now(clock)` devuelve la fecha **UTC**: a partir de las siete de la tarde en
Ayacucho ya es mañana. Sin corregirlo, una fiesta que se celebra hoy desaparecería de la
portada esa misma tarde y la cuenta regresiva restaría un día de más — un fallo que solo se
manifiesta cinco horas al día y que habría pasado todas las pruebas de la mañana.
Hay un test que lo fija: a las 21:00 de Huamanga, `TiempoAyacucho.hoy()` devuelve hoy
mientras `LocalDate.now(clock)` ya devuelve mañana.

**La comprobación en navegador real** carga la misma ficha desde tres husos horarios
—Kiritimati (UTC+14), Lima (UTC−05) y Midway (UTC−11), separados por 25 horas— y compara:

```
en la base de datos:   2026-03-27
Kiritimati  (UTC+14):  27 de marzo – 5 de abril de 2026
Lima        (UTC-05):  27 de marzo – 5 de abril de 2026
Midway      (UTC-11):  27 de marzo – 5 de abril de 2026
```

### El clonado anual, y por qué no copia la fecha

**La Semana Santa de Ayacucho es una festividad móvil.** Va atada a la Pascua y se desplaza
casi un mes de un año a otro; el Carnaval igual. Un clonado que copiara la fecha vieja
publicaría un dato falso precisamente en la festividad que más gente consulta y que da
nombre a media tesis.

Lo que se copia es la **plantilla**: nombre, descripción, organizador, tipo, lugar, distrito
y portada. Lo que no se copia es la fecha. Y de ahí salen tres reglas:

1. **El clon nace en BORRADOR.** Nadie ve unas fechas que una persona no haya confirmado, y
   la verificación comprueba que el borrador clonado no aparece en el calendario público.
2. **Se pueden indicar las fechas reales** en la propia petición de clonado.
3. **Si no se indican, se *proponen*** desplazando el año. Es correcto para una fiesta de
   fecha fija —el 9 de diciembre lo es— y solo un punto de partida editable para una móvil.

Dos detalles que son los que de verdad rompen un clonado:

- **La duración se conserva, no se desplaza la fecha final por separado.** Del 27 de febrero
  al 2 de marzo hay 4 días; clonando a un bisiesto y desplazando ambas fechas saldrían 5. Se
  calcula `fin = inicio + duración`, y hay un test con ese caso exacto.
- **Las traducciones se copian como filas nuevas.** Su clave primaria es
  `(evento_id, idioma)`, así que reutilizar las instancias del original no las copiaría: las
  **movería**, y la edición anterior se quedaría sin texto. Un test comprueba que tras clonar
  hay dos juegos de traducciones y que el original conserva el suyo.

La columna `evento_origen_id` (migración **V16**, aprobada por el usuario) existe para que
clonar dos veces al mismo año devuelva 409 en vez de crear dos borradores gemelos que nadie
distingue.

### El clima de un evento: cuatro estados, ninguno un error

El pronóstico gratuito de OpenWeatherMap llega a cinco días, y **casi todos los eventos de
una agenda cultural están más lejos**. Es decir: no tener pronóstico es el caso normal, no un
fallo. Pintarlo en rojo o dejar un hueco con guiones describiría mal lo que pasa —el
pronóstico no ha fallado, es que todavía no existe—. Por eso la respuesta lleva un estado
explícito y no un campo nulo que el frontend tenga que interpretar:

| Estado | Cuándo | Qué se ve |
|---|---|---|
| `PRONOSTICO` | El evento cae dentro de los 5 días | Mínima, máxima, probabilidad de lluvia y los consejos del Bloque 5 |
| `FUERA_DE_ALCANCE` | Falta más | Una línea sobria: «Aún faltan 123 días, así que todavía no hay pronóstico. Suele ser época de lluvias» |
| `NO_DISPONIBLE` | El proveedor no responde | Se dice, sin fingir un dato |
| `PASADO` | Ya ocurrió | Nada |

**La temporada es cualitativa a propósito.** En la sierra sur llueve de noviembre a marzo y
la estación es seca de abril a octubre; eso es cierto y le sirve a quien planifica un viaje.
Decir «hará 18 grados en diciembre» sería inventar. **Si quieres cifras para la defensa,
deben salir de SENAMHI y citarse** — no de una estimación escrita por mí.

En «Durante mi visita» los dos estados conviven sin que se note la costura: la verificación
muestra un viaje de 5 días con estados `PRONOSTICO` los primeros y `FUERA_DE_ALCANCE` los
últimos.

### Tres fallos reales que la verificación destapó

**1. La cookie del viaje nunca llegaba al servidor.** El nombre de la cookie estaba exportado
desde `SelectorFechasViaje.tsx`, que lleva `"use client"`. Un Server Component puede importar
de un módulo de cliente, pero **lo que recibe no es el valor**: son referencias que el
bundler resuelve en el navegador. La página pedía la cookie con algo que en el servidor no
era la cadena esperada, así que el selector aparecía siempre con las fechas por defecto por
mucho que el navegador las guardara bien. La constante vive ahora en `lib/viaje.ts`, un
módulo neutro. **La regla que deja: lo que comparten servidor y cliente no vive en un archivo
con directiva.**

**2. `agendaAdmin.tipo` era a la vez etiqueta y mapa.** Pedir con `t("tipo")` una clave que
en realidad es un objeto (`tipo.RELIGIOSO`, `tipo.CIVICO`…) hacía que next-intl lanzara y el
componente de gestión de eventos **no se renderizara en absoluto**. Las etiquetas pasaron a
`etiquetaTipo` y `etiquetaEstado`.

**3. Mi propia verificación se estaba engañando.** Esperaba a que existiera el contenedor
`<ul>` de la bandeja, que se renderiza vacío desde el primer fotograma, y leía «0 eventos»
como si fuera un resultado. Eso convertía el fallo anterior en un falso negativo silencioso.
Ahora espera a que haya **contenido**, no contenedor.

### Decisiones de diseño

- **El mes y el filtro en la URL**, no en el estado de React. Los botones de mes son enlaces:
  funcionan sin JavaScript, la vista se comparte tal cual y el botón «atrás» hace lo que uno
  espera. Es la misma decisión que los filtros del Bloque 4.
- **Cookie y no `localStorage`** para las fechas de viaje. El plan (sección 8) decía
  `localStorage`; la cookie es mejor porque el servidor puede leerla y la página llega ya
  renderizada, sin parpadeo ni salto de maquetación. La página es la **única `force-dynamic`**
  del sitio, y está anotado por qué.
- **Puntos por día, no barras que crucen la rejilla.** Una fiesta de varios días pinta un
  punto en cada uno de sus días. Las barras son mucho CSS para poco valor en la pantalla que
  importa, que es la del móvil.
- **Rejilla de seis semanas siempre**, aunque sobren filas: un alto fijo evita que la página
  salte al cambiar de mes.
- **URLs de evento por UUID.** La tabla no tiene columna `slug` y añadirla sería otro cambio
  al modelo. Es peor para SEO y queda dicho.
- Se añadió `GET /distritos` (público, 119 filas) porque el formulario de alta lo necesita.
- `TiempoAyacucho` unifica la zona horaria, que estaba repetida en `ClimaService` y
  `LugarService`; ahora hay una sola definición.

### Pendiente de tu verificación

> **Las fechas de las tres festividades reales del seed hay que confirmarlas contra fuentes
> oficiales antes de la sustentación.** Son: Semana Santa 2026 (27 de marzo – 5 de abril),
> Carnaval Ayacuchano 2027 (6–9 de febrero) y Aniversario de la Batalla de Ayacucho
> (9 de diciembre). Las dos primeras son móviles y su programa concreto lo publican cada año
> el Arzobispado y la Municipalidad. Los otros dos eventos del seed están anclados a
> `CURRENT_DATE` y **marcados como datos de demostración** en su propia descripción: existen
> para que las dos ramas del clima se puedan enseñar cualquier día del año.

### Verificaciones ejecutadas

| Verificación | Resultado |
|---|---|
| `mvnw test` | **BUILD SUCCESS — 270 tests, 0 fallos** (45 nuevos: 43 del bloque más los del clima) |
| Fechas | A las 21:00 de Huamanga «hoy» sigue siendo hoy; el test comprueba además que lo ingenuo (`LocalDate.now`) ya devuelve mañana |
| Clonado | Duración preservada al cruzar un bisiesto; 29 de febrero → 28; traducciones como filas nuevas; segundo clon al mismo año → 409; evento no recurrente → 422; fecha de otro año → 400; sin ADMIN → 403 |
| Calendario | Solo publicados; una fiesta a caballo entre marzo y abril sale en los dos meses; febrero bisiesto llega al 29; los meses vecinos no se cuelan; filtro por tipo |
| «Durante mi visita» | Entra la fiesta que empezó antes de la llegada; la que acaba un día antes no; rango invertido → 422; más de 30 días → 422 |
| Clima | Los cuatro estados, incluido el evento en curso (da el tiempo de hoy, no el del día que empezó) y el viaje largo que mezcla pronóstico y temporada |
| `pnpm lint` / `type-check` / `build` | Sin errores; `/agenda`, `/agenda/[id]` y `/agenda/durante-mi-visita` en es y en |
| **Navegador real** | **23/23 comprobaciones.** Rejilla de 42 casillas, hoy resaltado y coincidiendo con la base, fiesta de 5 días marcada en sus 5 días, **la misma fecha desde tres husos separados por 25 horas**, las dos ramas del clima, navegación de diciembre a enero de 2027, filtro por tipo, cuenta regresiva en la portada, cruce del viaje con cookie que persiste, y clonado que deja el original intacto. **Consola limpia** |

---

## Bloque 8 — Preservación ciudadana

### La demostración de anonimato (la prueba para la defensa)

El guion de navegador real hace esto, en este orden, sin intervención manual:

1. **Registra a una vecina** (`vecina-<marca>@yachay-ayacucho.pe`) y **abre sesión con ella
   en el navegador**. Este es el caso que importa: no una visitante anónima que el sistema
   no conoce, sino **una persona plenamente identificada que pide que no se la nombre**.
2. Rellena el formulario de `/reportar` con el anonimato activado —que viene activado por
   defecto— y lo envía.
3. **Vuelca la fila entera de la tabla `reporte`**, columna por columna, sin elegir cuáles.

Esta es la salida literal de la última ejecución:

```
  ─── LA FILA COMPLETA EN LA BASE DE DATOS ───
  quien denuncio: vecina-1786047003267@yachay-ayacucho.pe
  su identificador en la tabla usuario: 019fd8b2-7b90-7a48-ae61-66d5f597c1a5

  -[ RECORD 1 ]---------+------------------------------------------------------------------------
  id                    | 019fd8b2-830f-7f02-8a70-82b1dc55c25c
  tipo_incidente_id     | 019fbb0d-f171-744e-ba33-b25576b8ac5b
  usuario_id            |
  nombre_reportante     |
  descripcion           | PRUEBA-1786047001487 Pintadas con aerosol en el muro lateral del templo
  ubicacion             | 0101000020E610000075931804568E52C0C520B07268512AC0
  direccion_referencial | Muro lateral, junto a la puerta
  estado                | RECIBIDO
  notas_admin           |
  es_anonimo            | t
  created_by            |
  updated_by            |
  created_at            | 2026-08-06 15:10:05.455938-05
  updated_at            | 2026-08-06 15:10:05.455938-05
  deleted_at            |
```

**Esas quince columnas son todas las que tiene la tabla.** No hay ninguna oculta: la lista
sale de `SELECT *`, no de una selección mía. Y sobre esa fila el guion comprueba tres cosas
más, por si algún día alguien añade una columna sin darse cuenta:

- El identificador de la vecina —`019fd8b2-7b90-7a48-ae61-66d5f597c1a5`— **no aparece en
  ningún punto de la fila**. La búsqueda se hace sobre `reporte::text`, es decir, sobre la
  fila entera serializada, no sobre las columnas que yo decida mirar.
- **No existe ninguna columna** cuyo nombre empiece por `ip`, acabe en `_ip` o contenga
  `hash`. Se consulta `information_schema.columns`, así que es una afirmación sobre el
  esquema, no sobre esta fila.
- La única clave del anti-spam en Redis es `reporte:anon:L1kMqsD6lW1IBnn35-eDqQ`, y
  **ninguna clave contiene una IP**.

#### Dos advertencias honestas sobre esta demostración

**Primera: `updated_by` sí se rellena después de moderar.** Si en la defensa alguien abre la
fila una vez aprobada, verá un UUID ahí. Es el del **administrador que revisó la denuncia**,
no el de quien la puso, y el guion lo comprueba explícitamente
(`created_by / updated_by = NULL / 019fbb0e-…`, que es el id del admin). Esa trazabilidad
debe existir: sin ella nadie respondería de por qué se publicó o se descartó una denuncia.
Por eso el borrado se hace **solo en `@PrePersist` y no en `@PreUpdate`**. Y por eso la
demostración usa **dos personas distintas**: si la vecina y el moderador fueran la misma
cuenta —como ocurría en mi primera versión de la prueba—, ese UUID se podría leer como el
del denunciante y la demostración perdería toda su fuerza.

**Segunda: esto prueba lo que guarda la aplicación, no lo que ve la red.** El proveedor de
internet de la vecina y cualquiera que controle la red por la que pasa la petición siguen
sabiendo que su equipo habló con este servidor. Contra eso una aplicación web no puede
hacer nada, y prometer lo contrario sería falso. Lo que se garantiza es concreto y
verificable: **el sistema no conserva ningún dato que permita atribuir la denuncia**, ni
ahora ni ante una orden judicial futura, porque el dato sencillamente no está.

### La tensión del bloque: anti-spam sin identidad

Es el problema de fondo. Limitar el abuso pide reconocer a quien repite; el anonimato pide
no reconocer a nadie. La solución tiene tres piezas:

| Pieza | Qué consigue |
|---|---|
| **HMAC-SHA256 de la IP con una sal aleatoria de 32 bytes** | Un SHA-256 pelado de una IP **no sirve**: solo hay ~4.300 millones de IPv4, y recorrerlas todas para encontrar la que produce un hash dado es cuestión de minutos. Con una sal secreta, esa fuerza bruta es imposible |
| **La sal vive solo en memoria y rota cada día** | No está en el `.env`, ni en la base, ni en ningún archivo. Un reinicio del servidor la destruye, y con ella cualquier posibilidad de recomputar la huella. Es una garantía **estructural**, no una promesa |
| **Solo en Redis, con TTL de 24 h** | La clave se autodestruye. Nada llega nunca a PostgreSQL |

Se permiten **5 reportes anónimos cada 24 horas** por huella, y **si Redis no responde el
límite se abre**, no se cierra: una caída de la caché no puede dejar sin voz a quien quiere
denunciar un daño al patrimonio.

### La fuga que encontré y arreglé

`Reporte` heredaba de `EntidadAuditable`, y el listener de auditoría rellena `created_by`
desde el `SecurityContext`. Es decir: **una usuaria con la sesión abierta que marcaba
«anónimo» quedaba igualmente identificada en `created_by`**, en una columna que nadie mira
y que ningún test miraba. El anonimato habría sido falso sin que se notara.

El arreglo no es «acordarse de ponerlo a null en el servicio». Es un callback en la propia
entidad:

```java
@PrePersist
void borrarRastroSiEsAnonimo() {
    if (esAnonimo) {
        borrarAuditoria();       // created_by y updated_by
        usuario = null;
        nombreReportante = null;
    }
}
```

Los callbacks declarados en la clase corren **después** de los `EntityListener`, así que
este método anula justo lo que la auditoría acaba de escribir. La diferencia importa: con
esto, **ninguna ruta futura puede saltarse la garantía**, ni un servicio nuevo, ni un import
masivo, ni un test. `borrarAuditoria()` es `protected` a propósito: solo una subclase puede
renunciar a su propia trazabilidad, y ha de hacerlo de forma explícita.

### La fuga que no está en la base de datos: el EXIF

Una foto de un móvil lleva dentro las **coordenadas exactas**, la marca y el modelo del
aparato y, a menudo, el número de serie. Publicar la foto de una denuncia anónima tal cual
llega es publicar todo eso. Antes de subir nada a Cloudinary, `ValidadorImagen.sinMetadatos`
**decodifica la imagen y la vuelve a codificar** desde los píxeles: lo que se sube son los
píxeles y nada más. Si el original era un JPEG con transparencia, se compone sobre blanco
para no ennegrecer el resultado.

### El precio del anonimato, dicho antes y no después

`GUARDIAN` solo cuenta reportes **identificados**: no hay forma de premiar a alguien sin
saber quién es, y almacenar una identidad «solo para la insignia» sería exactamente la fuga
que este bloque evita. Lo que sí se hace es **avisar antes de enviar**, no después: si hay
sesión abierta y el anonimato está marcado, el formulario dice que ese reporte no contará
para la insignia. Quien elige el anonimato lo elige sabiendo lo que cuesta.

### Verificaciones ejecutadas

| Verificación | Resultado |
|---|---|
| `mvnw test` | **BUILD SUCCESS — 225 tests, 0 fallos**, de los que 17 son `AnonimatoReportesTest` |
| Anonimato en tests | Con sesión iniciada el reporte sigue sin identidad; el UUID del usuario **no aparece en `reporte::text`**; la tabla no tiene columnas de IP ni de hash; `GUARDIAN` cuenta los identificados e ignora los anónimos; el mapa público solo devuelve aprobados y resueltos |
| Coordenadas | Fuera de los límites de Ayacucho → 422 |
| `pnpm lint` / `type-check` / `build` | Sin errores; incluye `/es/reportar`, `/en/reportar`, `/es/mapa-incidentes` y `/en/mapa-incidentes` |
| **Navegador real** | **21/21 comprobaciones**. Sesión iniciada → reporte anónimo → **volcado de la fila** → no aparece en el mapa sin revisar → moderación con notas internas → aprobado → visible en el mapa público sin las notas → `GUARDIAN` con un reporte identificado. **Consola limpia** |

### Dos fallos reales que la verificación destapó

**1. Discordancia de hidratación en `/reportar`.** El estado inicial del GPS se calculaba
leyendo `navigator`, que en el servidor no existe: el HTML del servidor decía «ajusta las
coordenadas a mano» y el navegador pintaba «buscando tu ubicación». React tiraba el árbol y
lo regeneraba en el cliente. Se arregla arrancando siempre en `buscando` —un valor que el
servidor sí puede renderizar— y dejando que el efecto lo corrija. **La consola del navegador
lo delató; ni el `build` ni el `lint` lo habrían visto.**

**2. La tercera ejecución seguida fallaba por el rate limit.** No era un fallo del bloque,
pero enmascaraba los que sí lo serían: el guion limpia ahora `rate:*` y `reporte:anon:*` en
Redis y borra los reportes de ejecuciones anteriores antes de empezar.

### Una medición que conviene no leer de más

El guion mide **0 s** entre el primer clic y el envío, y eso **no demuestra el RF-69**.
Escribe a velocidad de máquina. Lo que sí demuestra es que **la aplicación no impone ninguna
espera**: un solo paso, sin recargas, sin un GPS que bloquee el envío y sin campos
obligatorios más allá de tipo, descripción y ubicación. Cuánto tarda una persona real hay
que medirlo con personas reales, y ese dato debería salir de las pruebas de usabilidad, no
de aquí.

---

## Bloque 7 — Favoritos, check-in, pasaporte y reportes

### Lo que el check-in garantiza y lo que no (léase antes que nada)

**Desde una web no se puede impedir que alguien falsee su posición.** La API de
geolocalización del navegador se sobrescribe desde las herramientas de desarrollo en dos
clics; de hecho, **el propio guion de verificación de este proyecto lo hace** para poder
probar la función. Impedirlo de verdad exigiría atestación de aplicación nativa, que está
fuera del alcance de una aplicación web.

Lo que sí se hace es **encarecer el fraude y dejarlo registrado**, y encuadrar la función
donde corresponde: el check-in alimenta sellos e insignias —un incentivo lúdico— y **no
desbloquea nada crítico**: ni permisos, ni contenido reservado, ni ventajas sobre otros
usuarios. Ese es el motivo por el que este nivel de garantía basta aquí y no bastaría para
un control de acceso. Las cuatro barreras:

| Barrera | Qué detiene |
|---|---|
| **Distancia en el servidor**, `ST_DWithin` sobre `geography` | Enviar cualquier coordenada. Nunca se acepta una distancia calculada por el cliente |
| **Precisión mínima** (se rechaza `accuracy` > 200 m) | La lectura de un navegador que triangula por IP, que no prueba cercanía a nada |
| **Enfriamiento de 24 h por lugar** | Inflar «más visitados» repitiendo el mismo check-in |
| **Salto imposible** (> 300 km/h desde el anterior) | Un script recorriendo los quince lugares en un minuto |

Y una quinta que no es barrera sino consecuencia: **se guarda el punto que envió el
cliente**, no el del lugar, de modo que un patrón absurdo queda registrado y es auditable.

### Verificaciones ejecutadas

| Verificación | Resultado |
|---|---|
| `mvnw test` | **BUILD SUCCESS — 210 tests, 0 fallos** (25 nuevos) |
| Check-in aceptado | Encima del lugar → 201 con distancia 0; **a 100 m también**, porque el radio son 150 |
| Check-in rechazado | A 1,5 km → 422 con la distancia real y el radio; precisión de 2 km → 422; segundo del mismo día → 409 y **una sola fila**; salto de 230 km en 2 min → 422 |
| Enfriamiento | Pasadas 24 h se vuelve a poder registrar (2 filas) |
| Auditoría | Se guarda la **coordenada enviada**, no la del lugar |
| Insignias | El primer check-in concede `PRIMER_PASO`; **no se duplica** al reevaluar; `HISTORIADOR` no se concede con un museo de tres; un `tipo` desconocido **no rompe el check-in**; visita e insignia quedan en el mismo commit |
| Pasaporte | Sellos, las 8 insignias con su estado, y progreso por ruta 1/2 → 2/2 al completar, que concede `RUTA_COMPLETA`. Un test comprueba que **no existe ninguna columna** `progreso`, `lugares_visitados` ni `sellos` |
| Reportes | Tercer reporte distinto → `EN_REVISION` y la reseña **desaparece de la lista pública**; duplicado → 409; autorreporte → 422; ni una ni dos FK a la vez → 400 |
| `pnpm lint` / `type-check` / `build` | Sin errores; 51 páginas generadas, incluidas `/perfil/favoritos` y `/perfil/pasaporte` |
| **Navegador real** | **14/14 comprobaciones**, dos pasadas seguidas: marcar favorito y verlo en el perfil, **check-in con posición simulada**, celebrar `PRIMER_PASO` y `FOTOGRAFO` en el momento, abrir el pasaporte con sus sellos y el progreso de las 3 rutas, compartir el diploma, reportar una reseña ajena, y el ranking «más visitados» ordenando `9, 5, 4, 3, 3, 2`. **Consola limpia** |

### Tres fallos reales que la verificación destapó

**1. `obtenida_en` se insertaba como NULL contra una columna `NOT NULL`.** `insignia_usuario`
no lleva listener de auditoría —a diferencia de casi todas las demás tablas—, así que
construir la entidad con *setters* dejaba el sello de tiempo vacío y **todo check-in
fallaba** en cuanto había una insignia que conceder. La entidad ya ofrecía un constructor
que compone la PK y exige la fecha; se usa ese, con el `Clock` inyectado.

**2. Mi test dejaba basura en la base compartida.** El test del criterio desconocido inserta
una insignia ficticia y no la retiraba; los contenedores de Testcontainers son **singletons
de toda la suite**, así que el catálogo pasaba a tener 9 insignias y rompía
`EsquemaYMapeoTest`, que comprueba que la migración siembra exactamente 8. Se limpia ahora
en el `@BeforeEach` y tras el propio test.

**3. Las rutas se acumulaban entre pruebas.** El `@BeforeEach` borraba `lugar_ruta` pero no
`ruta_tematica`, de modo que `rutas[0]` del pasaporte acababa siendo una ruta sobrante de la
prueba anterior, con 0 visitas. El progreso por ruta era correcto; lo que estaba mal era
contra qué se comprobaba.

### Decisiones de diseño

- **150 m y no los 100 del RF-39.** Decisión del usuario, y con buen argumento: en el centro
  histórico de Huamanga, entre calles estrechas y muros de piedra, el error típico del GPS
  de un móvil ronda los 20-50 m y empeora. Con 100 m se bloquearían check-ins de gente que
  está en la puerta. Se prioriza que el visitante honesto pueda usar la función; hacer
  trampa desde 140 m no da ningún premio que no diera desde 90.
- **La insignia se concede en la misma transacción que la visita.** Si se hiciera después,
  en un evento posterior al commit, una caída de red o del proceso justo en medio dejaría el
  sello puesto y la insignia perdida, sin forma de recuperarla salvo volver al lugar. Atadas
  al mismo commit, o se guardan las dos cosas o ninguna.
- **Un criterio JSONB desconocido se ignora, no revienta.** Lo peor que puede pasar es que
  una insignia no se conceda; jamás que falle el check-in que disparó la evaluación. Perder
  una visita real por un error de configuración sería mucho peor que no dar un premio.
- **El criterio es JSON y no columnas** porque cada insignia se gana de forma distinta;
  modelarlo con columnas daría una tabla llena de nulos o una tabla por tipo. Como la base
  **nunca consulta dentro** del JSON —lo interpreta el service—, no hay atributo
  multivaluado y la 1FN queda intacta. Añadir una insignia es un `INSERT`.
- **El progreso por ruta y los sellos se calculan al vuelo.** Son atributos derivados de los
  check-ins; almacenarlos exigiría triggers para mantenerlos sincronizados. Mismo criterio
  que con la calificación promedio del Bloque 6.
- **Ningún endpoint recibe un identificador de usuario**: todos operan sobre el del token.
  Si la ruta fuera `/usuarios/{id}/favoritos`, bastaría con olvidar una comprobación para
  que cualquiera leyera los favoritos ajenos; tomando siempre el id del token, ese error no
  se puede cometer.
- **Dos FK nullables y no una polimórfica** en `reporte_contenido`. Una sola columna con un
  `tipo` al lado sería más compacta, pero PostgreSQL no puede validar una FK que apunte a
  dos tablas: se perdería la integridad referencial y el `ON DELETE CASCADE`.
- **Contar filas equivale a contar denunciantes** porque los dos índices únicos parciales ya
  impiden que una persona reporte dos veces lo mismo: no hace falta `COUNT DISTINCT`.
- **El favorito es optimista** (RF-95): el corazón cambia en el mismo fotograma del toque y
  la petición sale después; si falla, se revierte. Esperar la ida y vuelta daría un retardo
  perceptible en una acción que debe sentirse instantánea. Respeta `prefers-reduced-motion`.
- **Marcar favorito es idempotente**, no un 409: en un botón que se pulsa con el pulgar, el
  doble toque accidental es la norma y no debería castigarse.
- **«Reportar» es un enlace discreto, no un botón llamativo.** Un botón prominente junto a
  cada opinión invita a usarlo como «no estoy de acuerdo», y tres reportes retiran el
  contenido de la vista pública.
- **El diploma se compone en el cliente** y se ofrece a la API nativa de compartir, con
  copia al portapapeles como respaldo. No se genera ninguna página pública: eso expondría
  nombre y progreso a cualquiera con el enlace, y para un diploma no compensa.

### Correcciones a los documentos de tesis (las hace el usuario)

- **RF-39: el radio pasa de 100 m a 150 m.** Justificación arriba. Conviene reflejarlo en el
  requisito y, si se cita en la memoria, explicar el porqué: es una decisión de usabilidad
  informada por el error real del GPS, no una relajación arbitraria.
- **Encuadre del check-in.** El RF-39 conviene que diga explícitamente que es un incentivo
  lúdico con validación de proximidad en servidor, **no una credencial** ni una prueba de
  presencia. Es defendible en la sustentación y evita una pregunta incómoda.
- **Endpoints nuevos no previstos en el plan:** `POST`/`GET`/`DELETE /lugares/{slug}/favorito`,
  `GET /perfil/favoritos`, `POST /lugares/{slug}/check-in`, `GET /perfil/pasaporte` y
  `POST /reportes-contenido`.
- **RF-39b, «diploma compartible»:** se implementó como texto compartido con la Web Share
  API, sin página pública. Si en la sustentación se quiere enseñar un diploma visual, sería
  un añadido de este bloque, no algo ya cubierto.

### Límites conocidos de este bloque

- **La insignia `GUARDIAN` no es obtenible todavía.** Su criterio es `REPORTES_APROBADOS`,
  que se refiere a los **reportes ciudadanos de preservación** (tabla `reporte`, Bloque 8), y
  **no** a los reportes de contenido de este bloque: son cosas distintas con nombres
  parecidos. El evaluador está implementado y devuelve 0 hasta el Bloque 8.
- **`EN_REVISION` en fotos aún no tiene bandeja propia.** La bandeja del Bloque 6 lista las
  `PENDIENTE`; una foto que llega a `EN_REVISION` por reportes desaparece de la galería pero
  no aparece en la cola de moderación. Conviene resolverlo en el Bloque 10, con el panel
  completo.
- **Un `GET` a `/lugares/{slug}/favorito` sin sesión responde 403 y no 401**, porque el
  patrón público `/lugares/**` deja pasar el filtro y es `@PreAuthorize` quien deniega. La
  protección es correcta; solo el código de estado es menos preciso de lo ideal.

---

## Bloque 6 — Reseñas, calificaciones y fotos

### Verificaciones ejecutadas

| Verificación | Resultado |
|---|---|
| `mvnw test` | **BUILD SUCCESS — 185 tests, 0 fallos** (30 nuevos) |
| Unicidad (RF-37) | Una segunda reseña del mismo usuario en el mismo lugar da **409** y **no** deja una segunda fila; el mismo usuario sí puede opinar en lugares distintos |
| Rangos | Calificaciones 0, 6 y −1 → 400 y **cero filas**; comentario de 501 caracteres → 400; solo estrellas sin comentario → 201 |
| Propiedad | Nadie puede editar la reseña de otro (**403**, y la original queda intacta); editar una reseña **oculta no la republica** |
| Promedio | Sale de la vista materializada, y hay un test que comprueba que **no existe ninguna columna de promedio en `lugar`**; ocultar la reseña de 1 estrella sube el promedio de 3.0 a 5.0; restaurarla lo devuelve a 3.0 |
| Validación de imagen | Acepta PNG y JPEG reales; **rechaza** un script PHP con nombre y cabecera de imagen, una cabecera JPEG válida seguida de basura, un SVG con `<script>`, un GIF, un archivo de 5 MB + 1 byte y uno vacío. El formato se decide por el **contenido**: un PNG llamado `.txt` y declarado `text/plain` se acepta igual |
| `pnpm lint` / `type-check` / `build` | Sin errores |
| **Navegador real** | **17/17 comprobaciones**, dos pasadas seguidas: iniciar sesión, opinar con la nota elegida (`nota=4`), editarla (`4 → 2`, marcada como editada), comprobar que ya no se ofrece escribir otra, **subir una foto de verdad a Cloudinary**, verla como pendiente y **confirmar que no aparece en la galería pública**, aprobarla desde `/admin` y verla entrar en la galería, ver el promedio moverse en ambos sentidos por el carril rápido, y el ranking «mejor valorados» ordenando `4.75, 4.67, 4.67, 4.5, 4.33, 4`. **Consola limpia** |

### Cuatro fallos reales que la verificación destapó

**1. Borrar una reseña vetaba a esa persona en ese lugar para siempre.** La baja lógica
conserva la fila, y existe un `UNIQUE (usuario_id, lugar_id)`: al intentar opinar de nuevo,
la base rechazaba la inserción y el endpoint respondía 409. Peor aún, `/resenas/mia` seguía
devolviendo la reseña borrada, así que la interfaz ofrecía «editar» algo que el usuario ya
había eliminado. Ahora `mia` ignora las `ELIMINADA` y **crear reutiliza esa misma fila** en
lugar de rechazar: la baja lógica deja de ser una condena permanente. Cubierto por dos tests
nuevos.

**2. Rechazar una foto la borraba del almacenamiento pero seguía sirviéndose.** `destroy`
elimina el **original** —comprobado: la URL sin transformar pasa a 404—, pero las versiones
transformadas que el CDN ya había servido **seguían cacheadas en el borde y respondían
200**. Una foto retirada por inapropiada continuaba siendo accesible para cualquiera que
tuviera su URL. Se añadió `invalidate=true` a la petición firmada de borrado; tras el
cambio, la URL transformada devuelve **404 en el acto**.

**3. El botón de iniciar sesión mostraba el literal `login.entrar`.** La clave existía en el
espacio `comun` pero el formulario la pedía en `login`, así que next-intl caía en su
fallback y pintaba el nombre de la clave. Estaba así **desde el Bloque 3**: ni el lint ni el
`build` ni los tests de backend pueden detectarlo, y las pruebas de navegador anteriores
nunca miraron el texto de ese botón. Salió porque esta vez el guion tuvo que iniciar sesión
de verdad.

**4. Una aserción del propio guion no comprobaba nada.** Daba por buena la reseña con solo
mirar que apareciera, y el texto que leía mostraba «5 estrellas» aunque se hubiera pulsado
la 4: el componente pinta **siempre las cinco** (unas doradas y otras grises) y
`textContent` las concatena todas. La nota se estaba guardando bien, pero la comprobación
habría pasado igual si no. Se expuso `data-calificacion` en cada reseña y ahora la
verificación afirma la nota exacta y su cambio al editar.

### Decisiones de diseño

- **El promedio nunca se guarda en `lugar`.** Sale solo de `estadistica_lugar`; una columna
  derivada rompería la 3FN. Hay un test que consulta `information_schema` para que, si
  alguien añade esa columna en el futuro, el fallo salte solo.
- **Carril rápido de 30 s en vez de columna derivada.** La vista se refresca cada 5 minutos,
  y ese retraso hacía parecer rota la aplicación: quien puntúa y no ve moverse la nota
  vuelve a puntuar. Al crear, editar, borrar o **moderar** una reseña se publica un evento
  `AFTER_COMMIT` que marca la vista como desfasada; un refresco **coalescente** la recalcula
  como mucho una vez cada 30 s. Se agrupan las peticiones a propósito:
  `REFRESH ... CONCURRENTLY` recalcula la vista entera, y hacerlo por cada reseña encadenaría
  refrescos sin descanso. El job de 5 minutos se queda como suelo de seguridad, porque cubre
  lo que entra por SQL sin pasar por la aplicación.
- **La marca «desfasada» se limpia ANTES de refrescar.** Al revés habría una ventana en la
  que una reseña nueva marcara la vista y el refresco en curso —que no la incluye— borrara
  esa marca al terminar, dejando el cambio sin recalcular hasta el ciclo de 5 minutos.
- **La lista de reseñas se lee en vivo; el promedio, de la vista.** Por eso una reseña recién
  escrita aparece al instante aunque la nota tarde. Es una diferencia real y la interfaz no
  la disimula.
- **Tres barreras para las imágenes, en orden creciente de coste** (RNF-15): tamaño en
  Tomcat, números mágicos y decodificación real con `ImageIO`. La segunda para el script
  renombrado; la tercera para lo que la segunda no puede ver —una cabecera JPEG válida
  seguida de basura—. **El `Content-Type` no se mira**: lo escribe el cliente, es una
  declaración de intenciones y no una prueba.
- **El nombre del archivo original no se usa jamás.** El `public_id` lo genera el servidor
  (`lugares/{slug}/{uuid}`), lo que elimina de raíz el recorrido de rutas y las colisiones.
- **Se valida antes de subir.** Al revés, un archivo malicioso ya estaría alojado en el CDN
  cuando decidiéramos rechazarlo. Y la subida remota va antes de escribir en la base: si
  Cloudinary falla no queda una fila apuntando a una imagen inexistente; el riesgo inverso
  deja un binario huérfano, que es el fallo barato de los dos.
- **Transformaciones en la URL de entrega, no al subir.** Al subir se guardaría una copia por
  tamaño y cada una consumiría plan; en la URL, Cloudinary genera la variante la primera vez
  que se pide y la cachea. `f_auto,q_auto` deja las fotos muy por debajo de los 200 KB del
  RNF-03 y cambiar de tamaño mañana es editar una cadena.
- **Sin SDK de Cloudinary.** La firma son cuatro líneas de SHA-1 y la subida un `POST`
  multipart con el `RestClient` que ya usa el clima. Mismo criterio que con Resilience4j:
  menos superficie que se rompa al cambiar de versión.
- **El `api_secret` no sale nunca del servidor**, y la verificación lo comprueba: ninguna
  petición del navegador lleva credenciales de Cloudinary.
- **Anti-spam por cuenta, aparte del rate limit por IP.** Son cosas distintas: aquel frena la
  fuerza bruta contra el login, este impide llenar la base de contenido. Una persona cambia
  de IP y varias comparten una. Falla abriendo, como se acordó en el Bloque 2.
- **Editar no manda a moderación, pero se marca.** En este modelo las reseñas son
  post-moderadas (nacen `PUBLICADA`) y `EN_REVISION` ya significa «acumuló 3 reportes»
  (RF-45). Pre-moderarlas exigiría una columna nueva, es decir, tocar el modelo. En su lugar,
  una reseña editada tras publicarse **aparece marcada en la bandeja** (se detecta por
  `updated_at > created_at`) y editar una oculta **no la republica**: no hay cambiazo
  posible tras una revisión.
- **Al rechazar, se conserva la fila y se borra el binario.** Trazabilidad de la decisión y
  detección de reincidentes, sin gastar almacenamiento. Si el borrado remoto falla, la foto
  queda rechazada igualmente —deja de verse— y el huérfano se anota en el log: manda el
  estado en nuestra base.

### Correcciones a los documentos de tesis (las hace el usuario)

- **Endpoints nuevos no previstos en el plan:** `GET/POST /lugares/{slug}/resenas`,
  `GET /lugares/{slug}/resenas/mia`, `PUT`/`DELETE /lugares/{slug}/resenas/{id}`,
  `GET/POST /lugares/{slug}/fotos`, `GET /lugares/{slug}/fotos/mias`, y las seis rutas de
  `/admin/moderacion`.
- **RF-38 conviene precisar el límite:** «hasta 5 fotos» se implementó como **5 por usuario
  y lugar**, no 5 en total por lugar, que dejaría la galería a merced de quien llegara
  primero.
- **Marca de agua en las fotos:** se dejó fuera por decisión tuya, para no entorpecer la
  vista del patrimonio. Si algún día interesa, Cloudinary la aplica como una transformación
  más en la URL de entrega (`l_logo,o_60,g_south_east`), sin tocar el original ni volver a
  subir nada: sería un cambio de una línea en `TransformacionesCloudinary`.

### Límites conocidos de este bloque

- **`MAS_VISITADOS` sigue plano.** El ranking funciona y está probado, pero los check-ins
  llegan en el Bloque 7, así que todos los lugares tienen 0 visitas y el orden cae en el
  desempate alfabético. `MEJOR_VALORADOS` sí ordena ya.
- **Una reseña revivida aparece marcada como «editada».** Al reutilizar la fila se conserva
  su `created_at`, así que la bandeja la señala. Para moderación es el comportamiento
  deseable —el contenido cambió después de publicarse—, pero conviene saber por qué ocurre.
- **RF-45 (reportes de contenido) no entra aquí:** es del Bloque 7. El estado `EN_REVISION`
  existe en el modelo y la bandeja ya lo muestra, pero todavía nada lo activa.

---

## Bloque 5 — Mapa, clima, recomendaciones y proximidad

### Verificaciones ejecutadas

| Verificación | Resultado |
|---|---|
| `mvnw test` | **BUILD SUCCESS — 155 tests, 0 fallos** (30 nuevos), confirmado en **dos pasadas seguidas** tras corregir un test intermitente |
| Clima degradado | Con el proveedor caído sirve el último clima conocido **marcado como obsoleto y con su hora**; sin nada cacheado responde «no disponible» sin lanzar; tres visitas seguidas cuestan **una sola** llamada externa |
| Reglas de recomendación | Deterministas con reloj y clima fijos: un lugar cerrado queda **descartado**, con lluvia el museo supera al mirador, con sol ocurre lo contrario, y un clima obsoleto **no puntúa** |
| GeoJSON del mapa | `FeatureCollection` válido, coordenadas en orden `[longitud, latitud]`, sin borradores, con color y categoría por chincheta |
| Rutas | Paradas en **orden de recorrido** (se insertan desordenadas a propósito), con coordenadas, excluyendo lugares no publicados |
| `pnpm lint` / `type-check` / `build` | Sin errores; `/es/mapa` y `/en/mapa` pre-generadas con ISR de 5 min |
| **Navegador real** | **22/22 comprobaciones**, dos pasadas seguidas: el mapa carga el estilo de MapTiler y dibuja (evento `load`, con tiles `.pbf` reales), la clave de MapTiler viaja en la URL **y la de OpenWeatherMap no sale al navegador**, 15 lugares, toggle 2D/3D, 8 categorías, 3 rutas con sus paradas, clima con temperatura y consejos traducidos, planificador con 6 sugerencias motivadas, proximidad que **exige gesto** y avisa al llegar a la catedral, versión inglesa completa y **consola limpia** |

### Cuatro fallos reales que la verificación destapó

**1. El mapa no dibujaba nada, y el `<canvas>` lo disimulaba.** MapLibre GL 6.1.0 dejó de
empaquetar su *web worker* dentro del bundle y lo carga como módulo ESM aparte; Turbopack
no resuelve esa URL y el navegador terminaba pidiendo **la propia página**, recibía HTML y
abortaba con «non-JavaScript MIME type». Sin worker no hay descarga ni decodificación de
tiles: el lienzo existía, tenía tamaño correcto y WebGL funcionaba, pero el mapa estaba
muerto. Se fijó **maplibre-gl 5.24.0**, que incorpora el worker como blob y funciona con
cualquier empaquetador; es la línea que sigue recibiendo versiones. *(Corrección a anotar:
el stack dice «MapLibre GL JS» sin versión; conviene fijar la 5.x hasta que Turbopack
resuelva el worker de la 6.)*

**2. La capa de edificios 3D apuntaba a una fuente inexistente.** Se declaró
`source="openmaptiles"`, pero el estilo *streets-v2* de MapTiler nombra su fuente
`maptiler_planet`. Además **ya trae una capa `Building 3D`** de tipo `fill-extrusion` con
las alturas reales. Se eliminó la capa propia y el toggle ahora cambia la visibilidad de la
del estilo, con `try/catch` por si algún día se cambia de estilo. El zoom inicial pasó de
14 a 15 porque esa capa tiene `minzoom: 15`; con 14 la vista se inclinaba sin edificios y
el modo 3D parecía roto. **Los edificios extruidos de Huamanga sí existen en los datos de
MapTiler**, así que el RF-17 se cumple entero.

**3. El aviso de proximidad no volvía a aparecer nunca.** La marca de «ya avisado» se
escribía al **mostrar** el aviso. Si el componente se volvía a montar —StrictMode lo hace
en desarrollo, y en producción basta con entrar en una ficha y volver— el estado en memoria
se perdía pero la marca sobrevivía, y el banner ya no reaparecía aunque se siguiera delante
del monumento: quedaba un «te avisaremos al llegar» eterno estando justo en el sitio. Ahora
la marca se escribe **al descartar el aviso o al alejarse**, nunca al mostrarlo.

**4. Un test de seguridad del Bloque 2 pasaba por suerte.** `tokenManipuladoDevuelve401`
alteraba el **último** carácter de la firma. La firma HMAC-256 son 32 bytes que en Base64URL
ocupan 43 caracteres: 43 × 6 = 258 bits para 256 significativos, de modo que el último
carácter solo aporta 4 bits útiles y los dos finales se descartan al decodificar. Cambiar
`A` por `B` ahí produce **exactamente los mismos bytes**, la firma sigue siendo válida y el
endpoint responde 200. Fallaba una de cada tres ejecuciones según cómo terminara la firma
generada. Ahora se altera el primer carácter, que sí aporta sus 6 bits. Cinco ejecuciones
seguidas en verde.

### Decisiones de diseño

- **Caché de dos niveles para el clima, y no `@Cacheable`.** Una caché con expiración
  protege de llamar de más al proveedor, pero **no protege de que el proveedor se caiga**:
  a los 30 minutos la entrada desaparece y, si en ese momento OpenWeatherMap no responde,
  no queda nada que servir. Por eso el mismo dato se guarda en `:fresco` (30 min, evita
  llamadas) y en `:ultimo-bueno` (24 h, sobrevive a la caída). La anotación sabe guardar y
  expirar, pero no sabe «si falla, dame lo último que tuvieras».
- **El circuit breaker no es quien devuelve el respaldo** —de eso se encarga la caché—,
  sino quien **deja de insistir**. Sin él, con el proveedor caído cada visitante esperaría
  sus 3 segundos de timeout antes de recibir el dato cacheado.
- **Resilience4j en su versión núcleo, no el starter de Spring Boot.** No existe módulo
  para Spring Boot 4 y el de la línea 3 arrastra problemas conocidos aquí. El starter solo
  aporta el azúcar de las anotaciones; el cortacircuitos vive en
  `resilience4j-circuitbreaker`, que es Java puro y no puede romperse al cambiar de versión
  del framework.
- **Un clima obsoleto informa pero no decide.** Sirve para decir «hace 2 h llovía», pero no
  puntúa en las recomendaciones: mandar a alguien a un museo por una lluvia que quizá
  escampó hace rato es decidir con datos caducados.
- **Las recomendaciones muestran su motivo.** Es lo que separa una sugerencia útil de una
  caja negra: quien lee «está abierto y a cubierto de la lluvia» entiende la lógica y puede
  discrepar. Los motivos viajan como **claves i18n**, nunca como frases armadas en el
  servidor, que quedarían fijadas en el idioma de la primera petición que llenara la caché.
- **Fuera de horario se explica en vez de desaparecer.** De noche no hay nada abierto y el
  motor devuelve cero sugerencias, que es correcto; pero ocultar la sección dejaba un hueco
  inexplicable justo cuando más gente planea el día siguiente.
- **Los puntos van en una fuente GeoJSON, no como marcadores de React.** Es lo que permite
  cumplir el RNF-04: 200 elementos del DOM sincronizados con la cámara hunden los
  fotogramas, mientras que dentro de la fuente los dibuja la GPU.
- **El mapa se carga con `ssr: false`** y la página sigue siendo Server Component. Debajo se
  renderiza **la misma información en HTML**: un mapa WebGL es invisible para los buscadores
  y para un lector de pantalla.
- **Histéresis en la proximidad**: se entra a 50 m y no se sale hasta los 80. Con un único
  umbral, el ruido normal del GPS haría aparecer y desaparecer el aviso sin parar en el
  borde.

### La clave de MapTiler: por qué es pública y por qué NO se hace proxy

La clave de MapTiler **viaja al navegador con cada petición de tile**; no hay forma de
ocultarla en un mapa de cliente. Su protección no es el secreto sino el **ámbito**: en el
panel de MapTiler se restringe por *Allowed HTTP origins* para que solo funcione desde
nuestro dominio. La verificación lo confirma: la clave aparece en las URLs de
`api.maptiler.com`.

Se evaluó reenviar los tiles a través del backend para que la clave no saliera del
servidor. **Se descartó**, y conviene dejar escrito por qué:

- **No protege realmente.** Cualquiera puede golpear el proxy y consumir la misma cuota;
  solo se pierde la atribución de quién lo hizo.
- **Rompe el RNF-04.** Cada sesión de mapa son cientos de tiles; pasarlos por Railway añade
  latencia, consume ancho de banda y anula el CDN de MapTiler.
- **No está contemplado en el plan gratuito.** Redistribuir tiles desde servidor propio
  excede lo que permiten sus términos: es un riesgo de cuenta, no solo técnico.

Tendría sentido únicamente con un plan que lo permita explícitamente, y en ese caso la
variable pasaría a llamarse `MAPTILER_KEY` (sin `NEXT_PUBLIC_`). La de OpenWeatherMap es el
caso contrario y así está implementada: **nunca sale del backend**, el navegador solo habla
con nuestro API. La verificación comprueba que ninguna petición del navegador lleva `appid=`.

### Correcciones a los documentos de tesis (las hace el usuario)

- **RF-26, pronóstico de 7 días.** El plan gratuito de OpenWeatherMap no entrega pronóstico
  diario: da **5 días en pasos de 3 horas**, que es lo que se agrega por día. El diario de 8
  días exige One Call, de pago por llamada. Procede corregir el RF a «5 días» o dar de alta
  One Call; el cambio sería una sola clase (`ClienteOpenWeather`).
- **Endpoints nuevos no previstos en el plan:** `GET /lugares/mapa` (GeoJSON), `GET /rutas`
  y `GET /rutas/{slug}`, `GET /clima`, `GET /clima/pronostico`, `GET /recomendaciones` y
  `GET /recomendaciones/planificador`.
- **CLAUDE.md, serializador de Redis.** Pide `GenericJackson2JsonRedisSerializer`, que es el
  de Jackson 2. Spring Data Redis 4.1 incluye ambos, pero el proyecto va en **Jackson 3**
  (`tools.jackson`) y la clase correcta es `GenericJacksonJsonRedisSerializer`, sin el «2».
  Queda así implementado. *(Resuelve la nota abierta desde el Bloque 0.)*
- **Versión de MapLibre**: fijar 5.x en el stack mientras la 6.x no cargue su worker bajo
  Turbopack.

### Límites conocidos de este bloque

- **El planificador no tiene interfaz propia todavía.** El endpoint funciona y está probado
  (6 sugerencias motivadas para mañana), pero la pantalla de planificación por fecha encaja
  mejor junto a la agenda cultural del Bloque 9.
- **La verificación en navegador corre con WebGL por software** (SwiftShader), que es mucho
  más lento que una GPU real. El mapa carga y dibuja, pero MapLibre nunca llega al evento
  `idle`; por eso la comprobación usa `load`. En un equipo con GPU esto no ocurre.

---

## Bloque 4 — Listado, ficha y búsqueda

### Verificaciones ejecutadas

| Verificación | Resultado |
|---|---|
| `mvnw test` | **BUILD SUCCESS — 125 tests, 0 fallos** (14 nuevos: 13 del explorador + 1 de regresión horaria) |
| Búsqueda full-text | «templos» encuentra «Templo…» y también la Catedral por su descripción (lematización del diccionario `spanish`); término en blanco **no filtra**; sin resultados devuelve una página vacía, no un error |
| Filtros y orden | Categoría, calificación mínima y texto **combinables entre sí**; tres órdenes; un lugar recién publicado aparece **aunque la vista materializada no se haya refrescado** |
| `pnpm lint` / `type-check` | Sin errores |
| `pnpm build` | Compilado. **30 fichas pre-generadas** (15 lugares × 2 idiomas), listado con `revalidate 5m`, fichas con `1h` |
| **Navegador real** | **23/23 comprobaciones**, dos pasadas seguidas: 12 tarjetas de 15, buscar «museo» deja 2 sin recargar, chips de categoría filtran, categoría + texto conviven en la URL, la paginación pasa a la 2.ª página (3 tarjetas), esqueletos visibles en red lenta, la ficha abre y muestra insignia, «Antes de ir» (8 datos) y los 7 días de horario, el contenido patrimonial **viaja en el HTML del servidor**, el listado en inglés sin textos incrustados, ninguna distancia antes del gesto y minutos a pie después, **consola limpia y sin avisos de hidratación** |

### Dos fallos reales que la verificación destapó

**1. Los horarios se guardaban y leían con cinco horas de desfase.** La base tenía
06:00–20:00 y el API devolvía 01:00–15:00. La causa era
`hibernate.jdbc.time_zone: UTC` (correcto y necesario para los instantes): Hibernate lo
aplicaba **también a las columnas `TIME`**, que aquí son *hora de pared* —«el templo abre
a las 09:00» no es un instante y no debe convertirse a nada—. Con la JVM en Lima, un
horario enviado como 09:00 se almacenaba como 14:00 y se volvía a leer como 09:00:
**escritura y lectura se compensaban entre sí**, así que la aplicación parecía coherente
consigo misma y los tests pasaban; la discrepancia solo salía a la luz frente a los datos
cargados por SQL, y el resultado dependía de la zona horaria de la máquina.

La corrección es un bloque estático en `TourismApplication` que fija la JVM en UTC, con lo
que la conversión pasa a ser la identidad. Va en un bloque estático y no en `main` porque
los tests no pasan por `main` pero sí cargan la clase. Nada de la aplicación dependía de
la zona por defecto: el `Clock` es `systemUTC()` y los cálculos sobre la hora de Ayacucho
nombran su zona explícitamente.

El test de regresión mira **la columna de la base, no la respuesta del API**: una
aserción sobre el JSON pasaba igual con el fallo presente. Se comprobó que el test falla
al desactivar la corrección y pasa al restaurarla.

**2. `useSearchParams()` sin límite de Suspense rompía el `build`.** En `next dev`
funcionaba; al compilar, Next no puede pre-generar una página cuyo componente cliente lee
parámetros que solo existen al servir la petición. Se envolvió el explorador en
`<Suspense>` con el esqueleto como respaldo: ahora lo que se pre-genera y cachea es el
esqueleto, y los filtros se resuelven al llegar la petición.

### Decisiones de diseño

- **La insignia abierto/cerrado se calcula en el navegador, con la zona de Ayacucho
  fijada.** Es la única forma de que conviva con el cacheado: un «Abierto» calculado al
  generar el HTML seguiría diciéndolo horas después. Se usa `useSyncExternalStore`, que
  resuelve dos cosas a la vez: distingue servidor de cliente sin efectos —evitando el
  aviso de hidratación— y, con una suscripción por minuto, hace que la insignia cambie
  sola cuando el lugar abre o cierra con la página abierta. Por eso el API sigue enviando
  la **grilla horaria** y no un booleano ya calculado.
- **La geolocalización nunca se pide al cargar.** Un diálogo de permiso que aparece antes
  de que se entienda qué se gana con él se deniega, y **una denegación es pegajosa**: el
  navegador no vuelve a preguntar y la función queda muerta en ese dispositivo. Al montar
  solo se *consulta* el estado con la Permissions API, que no abre ningún diálogo; el
  botón únicamente aparece si el permiso está sin decidir.
- **La posición no se persiste.** Una ubicación guardada de ayer mentiría sobre la
  distancia de hoy.
- **4 km/h ajustados por altitud**, en un único sitio (`lib/geo.ts`). A partir de 90 min
  se muestran kilómetros: «a 407 min caminando» es cierto e inservible. *(Si el usuario
  localiza una fuente académica sobre el efecto de la altitud en la marcha, se cita en la
  tesis.)*
- **Los criterios viven en la URL**, no en estado local: la búsqueda se puede compartir,
  el botón atrás funciona y recargar no pierde los filtros. La URL se sincroniza con el
  texto **ya estabilizado**, no con cada tecla: escribir «catedral» dejaría ocho entradas
  en el historial.
- **`LEFT JOIN` con la vista materializada, no `JOIN`.** Con un `JOIN` normal, un lugar
  recién publicado desaparecería del listado hasta el siguiente refresco de la vista
  (hasta 5 minutos). Hay un test que lo cubre.
- **El orden viaja como código numérico**, nunca como texto concatenado en el `ORDER BY`.
- **Las estadísticas de la página se traen en una sola consulta** por identificadores y se
  cruzan en memoria: pedirlas una a una serían 21 viajes a la base para pintar 20
  estrellas.
- **El `QueryClient` se crea con `useState`**, jamás como constante de módulo: en el
  servidor un módulo se comparte entre peticiones y mezclaría los datos de distintos
  visitantes.
- **Los esqueletos miden exactamente lo que la tarjeta real** (`h-52`). Si midieran
  distinto, la página daría un salto al llegar los datos y el CLS se saldría del 0.1 que
  exige el RNF-24.

### Correcciones a los documentos de tesis (las hace el usuario)

- **Sección 8 del plan, Bloque 4.** El bloque se describe como solo de frontend, pero el
  backend del Bloque 3 no tenía búsqueda, filtros combinados, rankings ni estadísticas en
  el resumen: sin eso no hay listado que construir. Esas piezas se añadieron **dentro del
  Bloque 4**, tal como se acordó.
- **Nuevo endpoint público `GET /api/v1/categorias`**, no previsto en el plan. Alimenta
  los chips de filtro; es catálogo de solo lectura.
- **La ficha expone ahora `fotos`** (solo las aprobadas por moderación). El campo existe y
  la galería está construida y conectada, pero **viene vacío**: la subida de fotos es un
  bloque posterior y el seed no trae imágenes. Conviene decidir si en la sustentación se
  cargan fotos reales con su autoría.

### Límites conocidos de este bloque

- **Los rankings salen planos con los datos actuales.** «Mejor valorados» y «Más
  visitados» funcionan —hay tests que los prueban insertando datos—, pero el seed no tiene
  reseñas ni visitas porque ambas llegan en bloques posteriores, así que en pantalla el
  orden coincide con el alfabético.
- **La galería no muestra nada todavía**, por lo mismo: no hay fotos que mostrar.

---

## Bloque 3 — i18n base + CRUD de lugares

### Verificaciones ejecutadas

| Verificación | Resultado |
|---|---|
| `mvnw test` | **BUILD SUCCESS — 111 tests, 0 fallos** (23 nuevos: 20 de CRUD, 3 de revalidación) |
| Permisos (RF-47) | Crear/editar/borrar sin token → **401**; con rol USUARIO → **403**; con ADMIN → 201/200/204. El listado público es accesible sin cuenta (RF-34) |
| Guardado transaccional | Un `POST` deja 1 lugar + 2 traducciones + 2 horarios; si la validación falla, **0 filas en las tres tablas** |
| Actualización | Un `PUT` con menos traducciones y horarios los **reemplaza sin acumular** (1 y 1) |
| Borrado | 204 y la ficha pasa a 404, pero la fila se conserva con `deleted_at` |
| Validación | Coordenadas de Lima → 400 en `errores.longitud`; sin español → 400; turnos solapados → 400; slug con mayúsculas → 400; idioma repetido → 400 |
| Idiomas del API | `?idioma=ES` → «Catedral de Ayacucho», `?idioma=EN` → «Ayacucho Cathedral»; sin traducción inglesa devuelve la española con `traduccionPorDefecto: true` |
| Errores traducidos | El mismo error con `Accept-Language: es` → «Las coordenadas quedan fuera de la region Ayacucho»; con `en` → «The coordinates fall outside the Ayacucho region» y `title: Invalid data` |
| Borradores | 404 para el público, 200 para ADMIN; el listado público no los incluye |
| Webhook ISR | Se dispara al guardar y al dar de baja, con el secreto en cabecera; **no se dispara si la operación fue rechazada** |
| `pnpm build` | Compilado; `/es` y `/en` generados estáticamente para las 4 páginas |
| `type-check` / `lint` | Sin errores |
| **Navegador real** | `/` → `/es` según `Accept-Language`; selector cambia a `/en/login` con textos en inglés; **cookie `NEXT_LOCALE=en` escrita y `/` pasa a llevar a `/en`**; login sigue funcionando bajo `/es/login` → `/es/perfil`; recarga con `200 /auth/refresh` mantiene la sesión; CRUD desde el navegador: crear 201, ES/EN correctos, validación 400 con mensaje en inglés, borrar 204; **cero errores de JavaScript** |

### El bug que solo se vio en el navegador

**El idioma no persistía.** El selector cambiaba la página y los textos, pero la cookie
`NEXT_LOCALE` no se escribía nunca: al volver a `/`, el sitio regresaba al idioma detectado
del navegador. La causa era que el selector sustituía el prefijo de la URL a mano con
`next/navigation`, y **la cookie solo la escribe next-intl cuando el cambio pasa por sus
propias APIs de navegación**. Se creó `src/i18n/navegacion.ts` con `createNavigation` y el
selector ahora llama a `router.replace(ruta, { locale })`.

Es exactamente el tipo de fallo que la lección del Bloque 2 anticipaba: los tests de
backend estaban todos en verde y el `build` también, porque el defecto vivía en el
almacén de cookies de un navegador real.

### Decisiones de diseño

- **Cookie en vez de `localStorage` para el idioma.** `localStorage` no existe en el
  servidor, así que no puede decidir el idioma antes de renderizar ni redirigir a quien
  entra por `/`. **⚠️ Hay que corregir el RF-63.**
- **`abiertoAhora` se calcula en el servidor**, con la zona `America/Lima`. El reloj del
  visitante puede estar en cualquier huso, y el lugar abre según la hora de Ayacucho.
- **Un borrador responde 404, no 403.** Un 403 confirmaría que el recurso existe, y un
  borrador es precisamente lo que aún no debe saberse que existe.
- **Validación de turnos solapados** (aprobada, no estaba en el plan): sin ella se podría
  guardar «9:00-13:00» y «12:00-18:00» el mismo día, y «abierto ahora» respondería sobre
  datos contradictorios.
- **El webhook se dispara con `@TransactionalEventListener(AFTER_COMMIT)`.** Dentro de la
  transacción, Next regeneraría la página leyendo datos aún no confirmados —o que acaban
  en rollback— y quedaría cacheada una versión que nunca existió. Hay un test que lo
  comprueba.
- **El webhook nunca rompe el guardado**: `try/catch` con timeout corto. Si Vercel está
  caído, el lugar se guarda igual y la página se regenera cuando expire su caché.
- **`timingSafeEqual` para el secreto de revalidación.** Con `===`, la comparación se
  detiene en el primer byte distinto y ese tiempo es medible: se puede reconstruir el
  secreto byte a byte.
- **`@BatchSize` en las colecciones.** En un listado paginado no se pueden traer
  colecciones con `JOIN FETCH` —Hibernate paginaría en memoria, algo que la configuración
  tiene prohibido—, así que se cargan después en lotes: una página de 20 lugares cuesta
  una consulta extra, no 20.
- **Los esquemas Zod reciben el traductor**, en vez de tener los textos incrustados: las
  *reglas* siguen siendo la fuente de verdad y los *textos* vienen de next-intl.

### 📌 Correcciones adicionales para los documentos de tesis

- **RF-63:** el idioma persiste en **cookie**, no en `localStorage`.
- **Sección 6.4:** añadir el índice `idx_lugar_ubicacion_geog` (ya anotado en el Bloque 1).

### Pendientes anotados

- **Bloque 4:** las páginas `/[locale]/lugares` y `/[locale]/lugares/[slug]` aún no
  existen; `revalidatePath` sobre una ruta no generada es inocuo, así que el circuito de
  revalidación ya está montado y probado a la espera de ellas.
- **Bloque 10:** el CRUD no tiene interfaz todavía; se verificó por HTTP y desde el
  contexto del navegador.

---

## Bloque 2 — Autenticación y seguridad

### Verificaciones ejecutadas

| Verificación | Resultado |
|---|---|
| `mvnw test` | **BUILD SUCCESS — 88 tests, 0 fallos** (38 de seguridad) |
| Ciclo completo en navegador real | Login → `/perfil` → recarga que mantiene la sesión → `/admin`, sin errores de JavaScript |
| `mvnw verify` (JaCoCo) | All coverage checks met — **88%** de líneas sobre 22 clases con lógica |
| Registro + BCrypt | `POST /auth/register` → 201; hash guardado con prefijo `$2a$12$` (cost 12, RNF-12) |
| Login | 200 con access token de **900 s** y cookie `HttpOnly`, `SameSite=Lax`, `Path=/api/v1/auth` |
| Refresh hasheado | En BD se guarda SHA-256 de 64 caracteres, distinto del valor de la cookie |
| `/auth/me` con token | 200 con los datos correctos |
| `/auth/me` sin token | **401** con ProblemDetail `no-autenticado` |
| Token manipulado / con otro secreto / caducado / con otro emisor | **401** en los cuatro casos |
| `/admin/resumen` con rol USUARIO | **403** `acceso-denegado` (no 401) |
| `/admin/resumen` con rol ADMIN | 200 `{"lugares":5,"usuarios":2}` |
| Escalada de privilegios | Reescribir el claim `rol` a ADMIN → **401**: la firma cubre el payload |
| Rotación | Refresh → 200 con cookie **distinta**; el token anterior → **401** |
| **Reutilización** | Presentar un token ya rotado → 401 **y se revocan todas las sesiones**: en BD `usados=1 revocados=2 vivos=0` |
| Logout | 204 con `Max-Age=0`; el refresh deja de servir |
| Enumeración de usuarios | Correo inexistente y contraseña incorrecta devuelven **cuerpos idénticos** |
| Rate limiting | Corta en el intento 7 con **429** y `Retry-After: 60` |
| XFF falsificado | Ignorado desde un peer no confiable; tres cabeceras distintas dan la misma identidad |
| Auditoría | `created_by`/`updated_by` se rellenan solos desde el SecurityContext |
| `proxy.ts` | `/perfil` y `/admin` sin cookie → **307** a `/login?continuar=...`; con cookie → 200 |
| `pnpm lint` / `type-check` / `build` | Sin errores; 5 rutas compiladas |

### Corrección posterior: el login no funcionaba en el navegador

Los 87 tests y las pruebas con `curl` pasaban, pero al abrir la aplicación de verdad el
login no llegaba a `/perfil`. Se diagnosticó con un navegador real (Edge headless dirigido
por CDP, sin añadir dependencias) y salieron **tres defectos encadenados** que ni los tests
de MockMvc ni `curl` podían ver, porque ninguno de los dos guarda cookies como un
navegador ni ejecuta React:

1. **La cookie era invisible para `proxy.ts`.** Se emitía con `Path=/api/v1/auth` por
   minimizar su alcance, pero un navegador solo envía una cookie a rutas que empiecen por
   su `Path`. El proxy de Next.js corre en `/perfil`, así que nunca la veía y **rebotaba al
   usuario a `/login` justo después de un login correcto**. Se amplió a `Path=/`, que es
   el alcance habitual de una cookie de sesión; sigue siendo httpOnly, Secure y SameSite,
   que son las protecciones que de verdad importan.
2. **Dos cookies con el mismo nombre.** Al cambiar el `Path`, cualquier navegador que ya
   tuviera la cookie antigua acababa con dos, y enviaba primero la caducada por ser la de
   ruta más específica. El login y el logout emiten ahora también una orden de borrado para
   la ruta antigua, así que se limpia sola sin que nadie tenga que borrar cookies a mano.
3. **La detección de reutilización se disparaba sola.** React StrictMode ejecuta los
   efectos dos veces en desarrollo, así que salían dos `/auth/refresh` con la misma cookie
   y el segundo se interpretaba como robo: **la propia medida de seguridad cerraba la
   sesión del usuario legítimo**. Se resolvió compartiendo una única promesa de renovación
   (*single-flight*), que además hacía falta en producción: varias peticiones que caduquen
   a la vez dispararían una renovación cada una.

También se corrigió que **`proxy.ts` no puede proteger nada en el despliegue previsto**:
con el frontend en Vercel y el backend en Railway, la cookie pertenece al dominio del
backend y el proxy nunca la verá. Se añadió la guarda de cliente `useSesionRequerida`,
que es la que cubre ese caso.

De propina se silenciaron los ~40 avisos de Spring Data Redis al arrancar
(`spring.data.redis.repositories.enabled: false`): no existe ningún repositorio de Redis
en el proyecto, la caché y el rate limiting usan `StringRedisTemplate` directamente.

**Verificado en navegador real, perfil limpio:** login → `/perfil` con los datos correctos
→ recarga con `200 /auth/refresh` y la sesión persiste → `/admin` accesible con rol ADMIN
→ cero errores de JavaScript → una sola cookie, httpOnly y `Path=/`.

**Lección para los siguientes bloques:** MockMvc y `curl` no sustituyen a un navegador.
No manejan el almacén de cookies con sus reglas de `Path`, ni ejecutan React, ni
reproducen StrictMode. Conviene una comprobación en navegador real al cerrar cada bloque
que toque sesión o formularios.

### Cuatro bugs que los tests destaparon

1. **La revocación en cascada se deshacía sola.** Al detectar la reutilización de un
   refresh token, el servicio revocaba todas las sesiones y acto seguido lanzaba una
   excepción — y Spring hace *rollback* ante cualquier `RuntimeException`, así que la
   revocación se borraba. El sistema detectaba el robo y no hacía nada. Se corrigió con
   `noRollbackFor`: la excepción es una señal de negocio, no un fallo que deba anular el
   trabajo ya hecho. **Es el bug más grave del bloque y solo se ve con un test que
   compruebe el efecto, no el código de respuesta.**
2. **`LazyInitializationException` al rotar.** El servicio devolvía el `Usuario` como
   proxy perezoso y la respuesta se construía fuera de la transacción. Con
   `open-in-view=false` —que el CLAUDE.md exige justamente para esto— reventaba. Se
   añadió un `JOIN FETCH` del usuario y su rol en la consulta del token.
3. **El perfil `dev` nunca estuvo activo.** `SPRING_PROFILES_ACTIVE=dev` en el `.env` no
   activaba nada: esa forma en mayúsculas solo la reconoce Spring cuando llega como
   variable de entorno real del sistema; leída de un archivo de propiedades es una clave
   más. Venía así desde el Bloque 0 y no se notó porque nada dependía del perfil. Se
   declaró `spring.profiles.active` de forma explícita en `application.yml`.
4. **Jackson 3 en Boot 4:** `com.fasterxml.jackson.databind.ObjectMapper` ya no es un
   bean. Los tests pasaron a usar JsonPath, que no depende de la versión de Jackson.

### Decisiones de seguridad

- **Access token de 15 minutos**, no 24 horas. Un JWT no se puede revocar: si se filtra,
  el atacante lo usa hasta que caduque. La rotación del refresh permite tenerlo corto sin
  que el usuario lo note, porque el frontend lo renueva en silencio.
  **⚠️ Hay que corregir RF-32 y la sección 5.3 de la tesis, que dicen 24 h.**
- **`spring-boot-starter-oauth2-resource-server` en vez de jjwt.** Evita escribir un
  filtro propio que extraiga el Bearer, lo valide y construya el `Authentication`: ese
  filtro casero es donde se cuelan los fallos. Aquí lo aporta Spring ya auditado.
- **Detección de reutilización.** Rotar borrando la fila deja al sistema ciego: no
  distingue "token caducado" de "token robado y reproducido". Guardando `usado_en`, un
  token ya rotado que reaparece solo puede significar robo, y la respuesta correcta es
  cortar **todas** las sesiones del usuario, no solo esa petición.
- **SHA-256 y no BCrypt para el refresh token.** BCrypt es lento a propósito para
  proteger secretos de baja entropía elegidos por humanos. Un refresh son 256 bits
  aleatorios: no hay diccionario que valga, y el hash debe ser rápido y determinista
  porque el token se busca *por su hash* en cada renovación.
- **Mitigación de enumeración de usuarios.** Cuando el correo no existe se ejecuta
  igualmente un BCrypt contra un hash señuelo, para que el tiempo de respuesta no
  delate qué correos están registrados.
- **X-Forwarded-For validado.** Solo se lee si la conexión directa viene de un proxy
  conocido, y se recorre de derecha a izquierda. Leída a ciegas, la cabecera convierte
  el rate limiting en decoración: basta con mandar una IP inventada distinta cada vez.
- **Rate limiting *fail-open*.** Si Redis cae, la petición pasa. Degradar el límite es
  preferible a dejar el sistema inaccesible por una caída de la caché.
- **La protección de rutas del frontend es UX, no seguridad.** `proxy.ts` solo puede
  comprobar si *existe* la cookie, no validarla. La autorización real es del backend con
  `@PreAuthorize`: quien fuerce `/admin` verá el cascarón y recibirá 403 en cada llamada.

### ⚠️ Pendiente para el despliegue: `SameSite`

`COOKIE_SAMESITE=Lax` funciona en local, pero **con el frontend en Vercel y el backend en
Railway son dominios distintos y el navegador no enviaría la cookie**: `/auth/refresh`
dejaría de funcionar en cuanto se despliegue. Al desplegar hay que poner
`COOKIE_SAMESITE=None` y `COOKIE_SECURE=true`. La defensa CSRF que lo compensa ya está
implementada: `/auth/refresh` exige la cabecera `X-Refresh-Request`, que un formulario de
otro dominio no puede añadir sin un preflight CORS que el allowlist rechaza.
Con dominio propio (`yachay.pe` + `api.yachay.pe`) se podría volver a `Lax`, que es más
limpio.

### 📌 Correcciones adicionales para los documentos de tesis

Se suman a las anteriores:
- **RF-32 y sección 5.3:** el access token dura **15 minutos**, no 24 horas.
- **Sección 5.6:** `src/middleware.ts` se llama `src/proxy.ts` desde Next.js 16.
- **Sección 5.3:** añadir que `SameSite` será `None` en el despliegue actual, con la
  cabecera anti-CSRF como compensación.

### Pendientes anotados para bloques siguientes

- **Bloque 3:** el job de limpieza de refresh tokens caducados
  (`RefreshTokenRepository.eliminarCaducados`) está escrito pero aún no programado.
- **Bloque 10:** `AdminController` solo expone `/admin/resumen`, creado para poder
  verificar la autorización por rol de extremo a extremo. El panel real llega allí.
- **Producción:** `ADMIN_PASSWORD_INICIAL` solo actúa en perfil `dev`. En producción el
  administrador debe crearse por otra vía.

---

## Notas / decisiones tomadas

### 1. Un solo `.env` en la raíz — y **desviación documentada** respecto al plan
El `CLAUDE.md` exige un único `.env` real y un único `.env.example` en la raíz. El
anexo **11.2 del `PLAN_DE_DESARROLLO.md` dice lo contrario**: describe
`apps/api/.env.local` y `apps/web/.env.local` por separado. Se siguió el `CLAUDE.md`,
que tiene precedencia. El `README.md` ya está actualizado al esquema real.

Cómo lee cada parte ese archivo, **sin ninguna librería añadida**:
- **Docker Compose:** nativo, lee el `.env` contiguo al `docker-compose.yml`.
- **Spring Boot:** `spring.config.import` con la pista `[.properties]`, en
  `application.yml`. Dos rutas (`./.env` y `../../.env`) para cubrir el arranque
  desde la raíz y desde `apps/api`. **Sigue funcionando igual en Boot 4.**
- **Next.js:** `process.loadEnvFile()` en `next.config.ts`, API nativa de Node ≥ 20.12.

En los tres casos, una variable ya presente en el entorno real **gana** sobre la del
archivo: en producción mandan los paneles de Vercel/Railway (RNF-39).

### 2. 📌 Correcciones pendientes en los documentos de tesis (las hace el usuario)
Dos puntos donde `docs/PLAN_DE_DESARROLLO.md` quedó desalineado con la construcción real:
- **Anexo 11.2:** describe `.env.local` por aplicación; debe decir un único `.env` en la raíz.
- **Sección 2 (stack):** dice "Spring Boot 3.x" y "Next.js 15"; ahora es **Spring Boot 4.x
  y Next.js 16**, con la justificación de producto real y versiones en soporte.

### 3. Dependencias deliberadamente aplazadas al Bloque 1
El `pom.xml` solo trae lo que el Bloque 0 usa: webmvc, validation, actuator, jdbc,
data-redis, driver PostgreSQL, SpringDoc, Lombok y los starters de test. **JPA, Flyway,
hibernate-spatial, MapStruct, uuid-creator, Resilience4j, ShedLock, Testcontainers y
JaCoCo entran en el Bloque 1**, y hay que elegirlos ya compatibles con Spring Boot 4:
la BOM gestiona Hibernate 7.4.1, Flyway 12.4.0 y Testcontainers 2.0.5, así que
**hibernate-spatial debe ir en 7.4.1** para no desalinearse de hibernate-core.

### 4. Paleta Ayacucho: anclaje de los 5 colores oficiales (RF-90)
Los hex acordados se convirtieron a OKLCH y cada uno se ancló al peldaño de la escala
cuya luminosidad le corresponde, de modo que la rampa quede monótona y dos colores del
mismo peldaño pesen visualmente igual:

| Color | Hex origen | Token oficial | Inspiración |
|---|---|---|---|
| Retablo | `#B3202B` | `retablo-600` | Rojo carmín de cochinilla, retablo ayacuchano |
| Añil | `#24406E` | `anil-800` | Índigo de los textiles andinos |
| Quinua | `#C0703A` | `quinua-500` | Ocre del barro cocido |
| Sillar | `#E9D8C8` | `sillar-200` | Piedra rosada de los templos de Huamanga |
| Puna | `#46704F` | `puna-600` | Verde de altura |

Se añadió la neutral cálida `piedra` (hue heredado del sillar) para no usar grises
azulados. Los peldaños extremos llevan un **tope de gamut sRGB**: sin él, las cromas
altas a luminosidad 0.97 quedarían fuera de gamut y cada navegador las recortaría a su
manera. El afinado final es del Bloque 12.

### 5. `/api/v1` como context-path del servidor
En vez de repetir el prefijo en cada controller, se fijó
`server.servlet.context-path: /api/v1`. Cumple el RNF-28 de una vez y garantiza que
ningún endpoint futuro se olvide de versionarse. Los controllers solo declaran su ruta
propia (`/health`).

### 6. El health endpoint devuelve 503, no 200, cuando algo falla
Un proceso Java vivo que no puede hablar con PostgreSQL no está sano. Devolver 503
permite que UptimeRobot (RNF-07) detecte la degradación de verdad. El endpoint **nunca
expone el mensaje real de la excepción** (puede contener host, puerto o usuario de la
BD): devuelve un texto genérico y registra la causa completa en el log. Hay un test
que lo verifica.

### 7. Avisos conocidos, no resueltos a propósito
- **Mockito se auto-adjunta como agente.** Desde la migración, `mvnw test` imprime un
  aviso: Mockito se auto-adjunta para habilitar el *inline mock maker*, y eso dejará de
  funcionar en futuras versiones de la JDK. **No rompe nada hoy.** La solución es
  declarar Mockito como `-javaagent` en la configuración de Surefire; se hará en el
  **Bloque 1**, cuando haya que tocar el pom igualmente para JaCoCo y Testcontainers.
- **Regla del CLAUDE.md sobre `GenericJackson2JsonRedisSerializer`:** hay que revisarla
  bajo Jackson 3, cuya API vive en `tools.jackson`. Aplica cuando se configure la caché
  de Redis (Bloque 5), no antes.

### 8. Otros
- `pnpm 11` exige aprobar los paquetes que ejecutan scripts de instalación. Aprobados en
  `pnpm-workspace.yaml`: `sharp` y `unrs-resolver`, ambos del toolchain oficial de Next.
  Además, pnpm registró en `minimumReleaseAgeExclude` los `@types/react*` recién
  publicados: es su control de cadena de suministro, no un problema.
- `COOKIE_SECURE=false` en el `.env` local: con `true` el navegador descarta la cookie
  sobre HTTP. En producción debe ser `true`.
- Se inyecta `Clock` como bean en lugar de llamar a `Instant.now()`. Hará falta para
  fijar el tiempo en los tests de "abierto ahora" (RF-09b) y del motor de
  recomendaciones (RF-08).
- Los textos de la portada están escritos directamente en el componente porque
  **next-intl entra en el Bloque 3**. Esa página es provisional.
- Antes de migrar se guardó una copia del estado del Bloque 0 sin migrar (49 archivos)
  en la carpeta temporal de la sesión, por no existir aún un commit al que volver.

### 9. Pendiente del usuario: despliegue
Vercel y Railway **no se tocaron** (requieren iniciar sesión con la cuenta del usuario).
Queda todo preparado: `Dockerfile` multi-stage con usuario sin privilegios, variables
documentadas y monorepo con estructura que Vercel detecta.
