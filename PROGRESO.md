# PROGRESO — Yachay Ayacucho

Bitácora de avance del proyecto. Claude Code actualiza este archivo al terminar cada
bloque; el usuario lo lee para retomar entre sesiones. El orden de bloques está en
`docs/PLAN_DE_DESARROLLO.md`, sección 8.

## Estado actual
- **Bloque en curso:** ninguno (Bloque 0 terminado y migrado, pendiente de commit del usuario).
- **Próximo bloque:** Bloque 1 — Modelo de datos y migraciones.
- **Versiones vigentes:** Spring Boot 4.1.0 · Next.js 16.2.12 · React 19.2.8 · Java 21 · Node 24.

## Registro de bloques

| Bloque | Estado | Qué se hizo | Commit del usuario |
|--------|--------|-------------|--------------------|
| 0 — Cimientos | ✅ Completado | Monorepo pnpm (apps/web + apps/api), Docker Compose con PostgreSQL 16 + PostGIS 3.5 y Redis 7, `.env` único en la raíz leído por las tres partes, endpoint `/api/v1/health` con comprobación real de BD y caché (9 tests en verde), Tailwind v4 + `tokens.css` con la paleta Ayacucho en OKLCH, capa CSS de "sensación de app nativa", portada SSR que muestra el estado en vivo, Dockerfile del backend y README actualizado. **Migrado después a Spring Boot 4.1.0 y Next.js 16.2.12** (ver sección de migración) | — |
| 1 — Modelo de datos | Pendiente | — | — |
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
