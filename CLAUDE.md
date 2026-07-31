# CLAUDE.md — Sistema Web de Turismo y Patrimonio Cultural de Ayacucho

Instrucciones persistentes para Claude Code. Manténlo corto; el detalle vive en
`docs/STACK_GUIDE.md` (guía completa de skills y librerías) y en
`docs/PLAN_DE_DESARROLLO.md` (plan maestro: alcance, modelo de datos de 35 entidades,
y el ORDEN DE CONSTRUCCIÓN POR BLOQUES de la sección 8). Léelos cuando trabajes en UI
"tipo app nativa", geoespacial, modelo de datos o decisiones de arquitectura.

## Cómo se construye este proyecto (IMPORTANTE)
- Se construye BLOQUE POR BLOQUE siguiendo el orden de dependencias de la sección 8 de
  `docs/PLAN_DE_DESARROLLO.md`. No construir todo de golpe ni adelantar bloques.
- NO ejecutar comandos de Git (add/commit/push). El usuario los hace manualmente.
- Al terminar un avance: detenerse, explicar qué se hizo y para qué sirve, listar archivos
  creados/modificados, actualizar `PROGRESO.md`, y esperar al usuario.

## Qué es este proyecto
Aplicación WEB tradicional (NO PWA, sin service worker ni modo offline) de turismo y
patrimonio cultural de Huamanga, Ayacucho. Mobile-first, responsive, con sensación de
app nativa. Tesis: "Aplicación web para la publicación de información del patrimonio
cultural de Ayacucho, 2026". ORIENTADO A PRODUCTO REAL: no es solo un ejercicio
académico; se mantendrá y publicará después de la tesis, por eso se usan versiones
actuales y en soporte (Spring Boot 4, Next.js 16), priorizando mantenibilidad a largo plazo.

## Stack (no cambiar sin autorización)
- Backend: Spring Boot 4.x, Java 21 LTS, Spring Security + JWT, Spring Data JPA +
  Hibernate, PostgreSQL 16 + PostGIS, Redis (Upstash), MapStruct, Bean Validation,
  Flyway, Resilience4j, ShedLock, SpringDoc OpenAPI, JUnit 5 + Mockito + Testcontainers,
  JaCoCo, HikariCP.
- Frontend: Next.js 16 (App Router, SSR/SSG/ISR), TypeScript 5, Tailwind CSS v4,
  shadcn/ui + Radix, MapLibre GL JS + react-map-gl (tiles MapTiler), next-intl (es/en),
  TanStack Query, Zustand, React Hook Form + Zod.
- Monorepo: apps/web (Next.js), apps/api (Spring Boot).
- Variables de entorno: UN SOLO `.env` real en la raíz del proyecto (privado, en
  `.gitignore`, nunca subir a Git — RNF-17) + UN SOLO `.env.example` en la raíz como
  plantilla pública (sin valores reales, sí se sube a Git). NO crear `.env.local` ni
  archivos `.env` separados por app; ambas apps leen del `.env` de la raíz.

## Reglas de backend (no negociables)
- Arquitectura package-by-feature (com.huamanga.tourism.<feature>), con capas internas
  controller/service/repository/domain/dto/mapper.
- NUNCA exponer entidades JPA en controllers: siempre DTOs vía MapStruct. Records para DTOs.
- Inyección por constructor (final), nunca @Autowired en campos.
- Errores con ProblemDetail (RFC 7807) vía @RestControllerAdvice.
- Access token JWT corto en memoria; refresh token en cookie httpOnly+Secure+SameSite con rotación.
- spring.jpa.open-in-view=false. Cuidar N+1 (JOIN FETCH / @EntityGraph).
- Geoespacial: hibernate-spatial + JTS, SRID 4326, índices GIST en Flyway.
- Redis: serializador GenericJackson2JsonRedisSerializer (no el JDK por defecto).
- Virtual threads activados; dimensionar HikariCP por capacidad de PostgreSQL.
- Cada feature con sus tests. Cobertura JaCoCo >= 70%.

## Reglas de frontend (no negociables)
- Server Components por defecto; 'use client' SOLO en hojas con interactividad (mapa,
  drawers, formularios, animaciones).
- Estado de servidor = TanStack Query. Estado de cliente = Zustand. NO mezclarlos.
- Formularios: React Hook Form + Zod (esquema Zod como fuente de verdad).
- Animación: usar `motion` (import de "motion/react"), NUNCA "framer-motion" (React 19).
- Bottom sheets / drawers: Vaul (patrón responsive: Dialog en desktop, Drawer en móvil).
- Gestos: @use-gesture/react. Galerías: Embla Carousel.
- El mapa MapLibre es Client Component; proteger la API key de MapTiler por env.
- i18n: next-intl, cero textos hardcodeados (todo por t('clave')).
- Design tokens en Tailwind v4 @theme; cero hex en componentes.

## "Sensación de app nativa" (aplicar globalmente en el CSS base/layout raíz)
- touch-action: manipulation (quita delay de tap).
- -webkit-tap-highlight-color: transparent.
- user-select: none en UI (no en contenido real).
- Full-screen: usar svh por defecto; dvh con cautela.
- Safe areas: env(safe-area-inset-*) con max(); viewport-fit=cover.
- overscroll-behavior: contain.
- Targets táctiles >= 44x44px. Respetar prefers-reduced-motion.
- Haptics (Vibration API) y cross-document View Transitions: progressive enhancement con
  fallback (NO funcionan en iOS Safari; no construir nada crítico sobre ellos).

## Convenciones
- Backend: clases PascalCase, DTOs con sufijo Request/Response, endpoints REST en plural,
  @Transactional explícito en services, cero SQL concatenado.
- Frontend: componentes PascalCase, hooks con prefijo use, TypeScript estricto.
- Git: ramas feature/<RF>-descripcion, Conventional Commits en español, PRs obligatorios.

## Verificación antes de dar por terminado
- Frontend: `pnpm lint` y `pnpm build` sin errores.
- Backend: tests de la feature en verde.
- No introducir dependencias nuevas sin justificar contra el stack de arriba.

## Cuándo preguntar / detenerte
- Antes de cambiar el stack o añadir una librería no listada.
- Antes de tocar migraciones Flyway ya aplicadas (crear una nueva, no editar).
- Antes de cualquier cambio en el modelo de datos (35 entidades, 3FN estricta).
