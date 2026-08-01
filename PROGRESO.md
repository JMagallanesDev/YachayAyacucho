# PROGRESO — Yachay Ayacucho

Bitácora de avance del proyecto. Claude Code actualiza este archivo al terminar cada
bloque; el usuario lo lee para retomar entre sesiones. El orden de bloques está en
`docs/PLAN_DE_DESARROLLO.md`, sección 8.

## Estado actual
- **Bloque en curso:** ninguno (Bloque 1 terminado, pendiente de commit del usuario).
- **Próximo bloque:** Bloque 2 — Autenticación y seguridad.
- **Versiones vigentes:** Spring Boot 4.1.0 · Next.js 16.2.12 · React 19.2.8 · Java 21 · Node 24 ·
  Hibernate 7.4.1 · Flyway 12.4 · Testcontainers 2.0.5.

## Registro de bloques

| Bloque | Estado | Qué se hizo | Commit del usuario |
|--------|--------|-------------|--------------------|
| 0 — Cimientos | ✅ Completado | Monorepo pnpm (apps/web + apps/api), Docker Compose con PostgreSQL 16 + PostGIS 3.5 y Redis 7, `.env` único en la raíz leído por las tres partes, endpoint `/api/v1/health` con comprobación real de BD y caché (9 tests en verde), Tailwind v4 + `tokens.css` con la paleta Ayacucho en OKLCH, capa CSS de "sensación de app nativa", portada SSR que muestra el estado en vivo, Dockerfile del backend y README actualizado. **Migrado después a Spring Boot 4.1.0 y Next.js 16.2.12** (ver sección de migración) | — |
| 1 — Modelo de datos | ✅ Completado | 35 tablas + 1 vista materializada en 14 migraciones Flyway, 36 clases JPA package-by-feature, 35 repositorios, 89 índices (GIST espaciales, GIN full-text, parciales, únicos parciales), todos los CHECK de la sección 6.5, UUID v7 en backend y en SQL, auditoría y soft delete, catálogos de referencia (11 provincias, 119 distritos, 8+7 categorías, 7 tipos de incidente, 8 insignias), seed de demostración idempotente, job de refresco de la vista con ShedLock sobre Redis, y 50 tests contra PostGIS real con Testcontainers | — |
| 2 — Autenticación y seguridad | Pendiente | — | — |
| 3 — i18n + CRUD lugares (backend) | Pendiente | — | — |
| 4 — Listado/detalle/búsqueda (frontend) | Pendiente | — | — |
| 5 — Mapa, clima, recomendaciones, proximidad | Pendiente | — | — |
| 6 — Reseñas, calificaciones, fotos | Pendiente | — | — |
| 7 — Favoritos, check-in, pasaporte, reportes | Pendiente | — | — |
| 8 — Preservación ciudadana | Pendiente | — | — |
| 9 — Agenda cultural | Pendiente | — | — |
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
