# Guía de Stack: Backend Spring Boot + Frontend Next.js 15 con sensación de app nativa

Referencia detallada para el desarrollo. El `CLAUDE.md` de la raíz resume las reglas;
este archivo explica el porqué y el cómo. Consultar al trabajar en UI nativa, geoespacial
o decisiones de arquitectura.

## Prioridad de aprendizaje/implementación
1. Server vs Client Components + estrategias de render (SSG/ISR/SSR) en App Router.
2. TanStack Query en App Router (prefetch en servidor + hydration, optimistic updates).
3. Backend: package-by-feature + ProblemDetail + seguridad JWT con rotación de refresh.
4. Capa nativa: Motion + Vaul + @use-gesture + Embla.
5. "Trucos anti-web": touch-action, tap highlight, user-select, svh/dvh, safe areas, overscroll.
6. Pulido: View Transitions, haptics, design tokens, accesibilidad.

Regla de oro: ~80% de la "sensación nativa" viene del punto 4 y 5. No dejarlos para el final.

## BACKEND (Spring Boot 3.x / Java 21)

### Arquitectura
Package-by-feature en el nivel superior con capas limpias internas. Ej.
`com.huamanga.tourism.lugar` con `web/controller`, `service`, `repository`,
`domain/entity`, `dto`, `mapper`. Agrupar por capacidad de negocio, estructurar por
frontera arquitectónica.
- Inyección por constructor con campos `private final` (Lombok @RequiredArgsConstructor).
- Separación DTO ↔ entidad estricta: MapStruct genera mappers en compilación. Records para DTOs.
- ArchUnit (opcional) para tests que verifiquen reglas de arquitectura.

### Errores, paginación, validación, docs
- ProblemDetail (RFC 7807): @RestControllerAdvice que extiende ResponseEntityExceptionHandler.
  Devolver type, title, status, detail, instance + errorCode/timestamp/traceId. El frontend
  (TanStack Query) parsea errores de forma uniforme. Separar detección (excepción de dominio en
  service) de reporte (traducir a HTTP en el advice).
- Paginación: Pageable/Page<T>; nunca devolver listas completas. Proyecciones para columnas mínimas.
- Bean Validation: @Valid en @RequestBody; MethodArgumentNotValidException integra con ProblemDetail.
- SpringDoc OpenAPI: @Operation, @Schema, tags. Documentar códigos de error y esquemas ProblemDetail.

### Seguridad JWT
Access token corto (~15 min) + refresh largo (~7 días) con rotación (cada uso invalida el
anterior). @PreAuthorize/@Secured para roles. No meter datos sensibles en el JWT (es Base64).
Refresh en cookie HttpOnly+Secure+SameSite. Stateless, CORS explícito para el dominio Next.js,
rate limiting en auth/escritura, HTTPS obligatorio, no exponer stack traces.

### Caching Redis (Upstash)
spring-boot-starter-cache + data-redis, @EnableCaching, @Cacheable/@CachePut/@CacheEvict.
CRÍTICO: reemplazar serializador JDK por GenericJackson2JsonRedisSerializer. Cuidado con el
self-invocation trap (@Cacheable en llamada interna de la misma clase no dispara). TTL por caché.
Envolver acceso a Redis con circuit breaker + CacheErrorHandler para degradar con gracia.

### Resilience4j
resilience4j-spring-boot3 (requiere aop + actuator; 2.x requiere Java 17+).
@CircuitBreaker (fallos externos como MapTiler/OpenWeather), @Retry (transitorios), @RateLimiter
(abuso), @Bulkhead (aislar recursos), @TimeLimiter (timeouts async). Orden fijo de aspectos:
Retry(CircuitBreaker(RateLimiter(TimeLimiter(Bulkhead)))). Métricas en Actuator/Micrometer.

### Geoespacial PostGIS + Hibernate Spatial
hibernate-spatial (misma versión que hibernate-core), tipos JTS (org.locationtech.jts.geom):
Point, Geometry, Polygon. Hibernate 6: PostgreSQLDialect ya trae capacidades espaciales.
Queries con ST_Distance, ST_DWithin, ST_Contains para "cerca de mí" y bounding box de MapLibre.
Índices GIST obligatorios (crear en Flyway). SRID 4326. Elegir geometry (plano) vs geography
(esférico, mejor para distancias reales).

### Rendimiento
- Virtual threads: spring.threads.virtual.enabled=true (Java 21 + Boot 3.2+). Ideal I/O-bound.
  Evitar synchronized con I/O dentro (pinning) → usar ReentrantLock.
- HikariCP: con virtual threads el pool es el límite real. maximum-pool-size por capacidad de
  PostgreSQL (~50-150), connection-timeout corto (2000-3000ms), leak-detection-threshold.
- N+1: JOIN FETCH, @EntityGraph, @BatchSize. spring.jpa.open-in-view=false.
- Índices: GIST espaciales + B-tree en FKs y columnas de filtro/orden.

### Testing
JUnit 5 + Mockito + Testcontainers con imagen PostGIS real (postgis/postgis). JaCoCo >= 70%.
Nota: @MockBean deprecado, usar @MockitoBean.

## FRONTEND (Next.js 15 / React 19)

### Render
Todo es Server Component por defecto; 'use client' solo en hojas con interactividad. Server
Components para data-heavy y SEO (fichas de patrimonio con SSG/ISR). Client Components mínimos
(mapa, drawers, formularios). Dominar revalidate (ISR) para páginas de sitios.

### Datos y estado
- TanStack Query: QueryClient nuevo por request en servidor, Providers 'use client', prefetch
  en Server Components + hydration. Gestiona caché, refetch, loading/error y optimistic updates.
- Zustand para estado de cliente (filtros de mapa, preferencias UI, sitios guardados).
- NUNCA usar Zustand/Redux para estado de servidor (error de abstracción común).

### Formularios e i18n
- React Hook Form + Zod + @hookform/resolvers. Esquema Zod como fuente de verdad + inferencia
  de tipos. shadcn/ui trae Form sobre RHF. Multi-paso: RHF + Zod + Zustand.
- next-intl: routing por locale (/es, /en), namespaces, formateo por locale, validación Zod i18n.

### Mapas
MapLibre GL JS + react-map-gl (tiles/estilos MapTiler). El mapa es Client Component (WebGL no
existe en servidor). Proteger API key por env + restricción por dominio. Plugins: clustering,
maptiler-geocoding-control, minimap.

### Estructura de carpetas
app/ con route groups (marketing)/(app). Colocar componentes de ruta junto a ella; compartidos
en components/ (components/ui/ para shadcn). lib/ para utilidades, cliente API, QueryClient.
Client Components pequeños y en las hojas; fetching arriba en Server Components.

## CAPA "APP NATIVA" (lo más importante para el usuario)

### Animación
- Motion (paquete `motion`, import "motion/react"). ÚNICA compatible con React 19. AnimatePresence
  (salidas), layout animations, micro-interacciones. NO usar "framer-motion".
- AutoAnimate (@formkit/auto-animate) para listas que se filtran/reordenan (una línea).
- React Spring solo si un gesto necesita física precisa (se integra con @use-gesture).

### Gestos
- @use-gesture/react: drag, pinch, swipe (dirección/velocidad). Fijar touch-action para no romper
  scroll. Usar en swipe de fotos, pinch-zoom, drag de tarjetas.
- Vaul: bottom sheets tipo iOS (snap points, swipe-to-dismiss, background scaling). Pieza central
  de la sensación nativa. Integrado en el Drawer de shadcn/ui. (Nota: shadcn migra Drawer de Vaul
  a Base UI; el API puede cambiar, revisar doc vigente.)
- Embla Carousel (embla-carousel-react): galerías de fotos, momentum, swipe preciso.
- Pull-to-refresh: @use-gesture (drag hacia abajo en tope) + indicador con Motion.

### Navegación móvil
- Bottom navigation bar fija (position: fixed), respeta safe areas, targets >= 44x44px. En desktop
  transformar a sidebar/top nav.
- Bottom sheet (Vaul) para acciones/filtros en móvil; Sheet lateral para navegación.
- Modales: Dialog de shadcn en desktop, Drawer/bottom sheet en móvil (patrón responsive).

### Safe areas, viewport, scroll
- env(safe-area-inset-*) con max() (ej. padding-bottom: max(1rem, env(safe-area-inset-bottom))).
  Requiere viewport-fit=cover. Crítico para la bottom nav.
- svh por defecto en full-screen (evita saltos); dvh con cautela (reflows); lvh para inmersivo.
  Tailwind: h-svh, h-dvh, h-lvh. Fallback vh.
- scroll-behavior: smooth; overscroll-behavior: contain; -webkit-overflow-scrolling: touch.

### Feedback y microinteracciones
- Haptics (navigator.vibrate): feature-detect, fallback. NO iOS Safari (solo Android Chrome).
  Para iOS, truco input[switch] de Safari 18 (use-haptic). Progressive enhancement.
- Estados active/press con :active y active:scale-95 + transición.
- Skeleton loaders (shadcn Skeleton) en vez de spinners.
- Optimistic UI con TanStack Query (onMutate) al guardar/dar like.

### Trucos anti-web (aplicar en CSS base/layout raíz)
- touch-action: manipulation (quita delay de 300ms; ya no hace falta FastClick).
- user-select: none en botones/nav/labels (mantener text en contenido real).
- -webkit-tap-highlight-color: transparent.
- -webkit-touch-callout: none en interactivos.
- View Transitions API: Next.js 15.2 experimental (no producción todavía). Alternativa:
  next-view-transitions (Shu Ding). Same-document funciona (Chrome/Edge/Safari 18+); cross-document
  requiere Chrome/Edge 126+ (no Safari/Firefox). Progressive enhancement.

## DISEÑO VISUAL
- Design tokens Tailwind v4 @theme (CSS-first, OKLCH). 3 capas: primitivos → semánticos → componente.
  Tokens semánticos para dark mode (bg-surface en vez de bg-white dark:...).
- Escala tipográfica modular con clamp() para fluidez. Espaciado con escala consistente.
- Componentes bonitos SOLO en marketing/hero: Aceternity UI, Magic UI, Motion Primitives. shadcn/ui
  como base de la app. Verificar compatibilidad con Tailwind v4 por componente.
- Dark mode: next-themes, evitar negros puros, cuidar sombras (usar bordes/glows).
- Glassmorphism (backdrop-blur + semitransparencia) para bottom nav y overlays sobre el mapa.

## ACCESIBILIDAD (WCAG 2.1/2.2 AA)
- Targets >= 44x44px (mejor práctica; AA mínimo 24px). Baja el error de tap de ~15% a ~3%.
- Contraste 4.5:1 texto normal, 3:1 texto grande.
- Respetar prefers-reduced-motion (Motion y Radix lo soportan).
- Focus visible, navegable por teclado, focus trap en modales/drawers (Radix/Vaul lo hacen).

## CAVEATS
- Versiones recientes (Next 15, React 19, Tailwind v4, shadcn): verificar compatibilidad antes de instalar.
- iOS Safari es el más restrictivo: sin Vibration API, sin cross-document View Transitions. Diseñar
  para iOS como mínimo común denominador y mejorar en Android.
- experimental.viewTransition NO recomendado para producción todavía; usar con fallback.
- Probar SIEMPRE en dispositivo real: safe areas, dvh/svh, momentum, haptics varían del emulador.
- Benchmarks: si Lighthouse mobile < 90 o LCP > 2.5s, priorizar SSR/ISR e imágenes antes que animaciones.

## RECURSOS
- Backend: docs oficiales Spring; Baeldung; SivaLabs; Dan Vega; resilience4j.readme.io; Hibernate
  Spatial (User Guide); Testcontainers.
- Frontend: docs Next.js (blog 15.2); TanStack Query ("Advanced Server Rendering"); shadcn/ui; motion.dev;
  use-gesture.netlify.app; Vaul (emilkowal.ski); Embla; MapTiler (React + MapLibre); next-intl; MDN.
- Creadores: Jack Herrington, ByteGrad, Robin Wieruch. Diseño: Refactoring UI, Smashing Magazine.
