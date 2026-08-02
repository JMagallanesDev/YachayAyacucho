# Sistema Web de Turismo y Patrimonio Cultural — Huamanga

Aplicación web para la publicación de información del patrimonio cultural de Ayacucho.

**Stack:** Next.js 16 + React 19.2 (frontend) · Spring Boot 4 / Java 21 (backend) · PostgreSQL 16 + PostGIS · Redis
**Monorepo:** `apps/web` (frontend) · `apps/api` (backend)

---

## Índice

1. [Requisitos previos](#1-requisitos-previos)
2. [Configurar un equipo nuevo desde cero](#2-configurar-un-equipo-nuevo-desde-cero)
3. [El día a día: arrancar el proyecto](#3-el-día-a-día-arrancar-el-proyecto)
4. [Trabajar en dos dispositivos (PC ⇄ laptop)](#4-trabajar-en-dos-dispositivos-pc--laptop)
5. [Comandos de referencia](#5-comandos-de-referencia)
6. [Solución de problemas comunes](#6-solución-de-problemas-comunes)

---

## 1. Requisitos previos

Instala esto **una vez por equipo** (tanto en la PC como en la laptop):

| Herramienta | Versión | Para qué |
|---|---|---|
| Node.js | **20.12** o superior | Correr el frontend Next.js. La versión mínima no es capricho: el proyecto usa `process.loadEnvFile`, la API nativa de Node con la que el frontend lee el `.env` de la raíz |
| pnpm | 9 o superior | Gestor de paquetes del monorepo |
| Java (Temurin/OpenJDK) | 21 LTS | Correr el backend Spring Boot |
| Docker Desktop | Última | Levantar PostgreSQL + Redis locales |
| Git | Última | Sincronizar el código entre equipos |

> Maven **no** hace falta instalarlo: el proyecto incluye el Maven Wrapper (`mvnw`), que descarga la versión correcta por su cuenta la primera vez.

**Verifica que todo quedó instalado** (abre una terminal y corre):

```bash
node --version      # v20.x o superior
pnpm --version      # 9.x o superior
java --version      # 21.x
docker --version    # cualquier versión reciente
git --version       # cualquier versión reciente
```

> Si `pnpm` no está: instálalo con `npm install -g pnpm`.
> Docker Desktop debe estar **abierto y corriendo** (ícono en la barra de tareas) antes de usar cualquier comando `docker`.

---

## 2. Configurar un equipo nuevo desde cero

Sigue estos pasos **en orden**. Sirve tanto para la primera vez en tu PC como para configurar la laptop.

### Paso 1 — Clonar el repositorio

```bash
git clone https://github.com/<tu-usuario>/<tu-repo>.git
cd <tu-repo>
```

### Paso 2 — Crear el archivo de secretos (`.env`)

> ⚠️ **IMPORTANTE:** el archivo `.env` **NO está en Git** (por seguridad — RNF-17). Git te trae el proyecto, pero no las claves. Tienes que crearlo a mano en cada equipo.

**Hay un único `.env`, y vive en la raíz del proyecto.** No existen `.env.local` ni archivos `.env` por aplicación: las dos apps y Docker leen del mismo sitio. Cópialo de la plantilla y rellena los valores:

```bash
# Windows PowerShell
Copy-Item .env.example .env

# macOS / Linux
cp .env.example .env
```

Abre el `.env` y pega los valores reales. **Recupéralos desde tu copia privada** (tu gestor de contraseñas o archivo cifrado — ver la nota al final de esta sección). El `.env.example` documenta cada variable y viene con los valores de desarrollo local ya sugeridos.

#### ¿Cómo llega ese archivo a cada parte?

| Quién | Mecanismo | Configurado en |
|---|---|---|
| Docker Compose | Nativo: lee el `.env` que está junto al `docker-compose.yml` | — (automático) |
| Backend (Spring Boot) | `spring.config.import` con la pista `[.properties]` | [apps/api/src/main/resources/application.yml](apps/api/src/main/resources/application.yml) |
| Frontend (Next.js) | `process.loadEnvFile()`, API nativa de Node | [apps/web/next.config.ts](apps/web/next.config.ts) |

Ninguno de los tres necesita una librería extra, y en los tres casos **una variable ya definida en el entorno real gana sobre la del archivo**. Por eso en producción mandan las variables del panel de Vercel/Railway y la ausencia del `.env` no rompe nada.

> Notas sobre valores concretos:
> - `JWT_SECRET` puede ser distinto en cada equipo (cambiarlo solo invalida sesiones viejas). Mínimo 32 bytes: HS256 exige una clave de 256 bits.
> - `ADMIN_PASSWORD_INICIAL` da acceso al administrador del seed. Solo se aplica en perfil `dev` y solo si la cuenta sigue sin contraseña.
> - `COOKIE_SAMESITE` debe ser `Lax` en local y **`None` en producción** (con `COOKIE_SECURE=true`), porque frontend y backend están en dominios distintos.
> - `REVALIDATE_SECRET` es un único valor compartido por `web` y `api`; al vivir en un solo archivo ya no hay forma de que se descuadren.
> - `COOKIE_SECURE` debe ser `false` en local (HTTP) y `true` en producción (HTTPS).
> - Las claves de terceros (MapTiler, Cloudinary, OpenWeather) pueden quedar vacías: no se usan hasta los Bloques 5 y 6.

### Paso 3 — Levantar la infraestructura local (Docker)

Con Docker Desktop **abierto**, levanta PostgreSQL + PostGIS + Redis:

```bash
docker compose up -d
```

Verifica que los contenedores están corriendo:

```bash
docker compose ps
```

Deberías ver dos servicios en estado `running` (postgres y redis).

### Paso 4 — Instalar dependencias

```bash
pnpm install
```

Esto instala las dependencias de Node del frontend. (El backend Java descarga las suyas con Maven la primera vez que lo corras.)

### Paso 5 — Construir la base de datos

**Las migraciones se aplican solas al arrancar el backend**, así que normalmente no tienes que hacer nada aquí: al ejecutar `pnpm dev` (paso 6), Flyway crea las 35 tablas, los 89 índices, la vista materializada y los datos de referencia.

Si quieres lanzarlas a mano sin levantar la aplicación:

```bash
pnpm db:migrate    # Flyway: tablas, índices, constraints, vista materializada, catálogos
pnpm db:seed       # Datos de demostración: admin, 5 lugares con traducciones y horarios
```

**Dos niveles de datos, deliberadamente separados:**

| | Qué contiene | Dónde vive | ¿Va a producción? |
|---|---|---|---|
| **Datos de referencia** | 4 roles, 11 provincias, 119 distritos, 8 categorías de lugar, 7 de negocio, 7 tipos de incidente, 8 insignias | Migración Flyway `V14__catalogos.sql` | **Sí** — sin ellos la aplicación no funciona |
| **Datos de demostración** | 1 admin, 5 lugares de Huamanga, 1 ruta temática | `db/seed/seed_demo.sql`, vía `pnpm db:seed` | **No** — son desechables |

> El seed es idempotente: puedes lanzarlo las veces que quieras sin duplicar nada.

> ⚠️ El usuario admin del seed **no puede iniciar sesión todavía**: su `password_hash` es un marcador, porque BCrypt llega en el Bloque 2. Es intencional — hasta entonces no hay login.

> Aquí no sincronizas datos con tu otro equipo — los **reconstruyes**. Es correcto y deliberado: los datos de desarrollo son desechables. Lo que garantiza que ambos equipos sean idénticos es el *esquema*, no los datos, y de eso se encarga Flyway.

### Paso 6 — Arrancar todo

```bash
pnpm dev
```

Abre en el navegador:

- **Frontend:** http://localhost:3000
- **API (estado):** http://localhost:8080/api/v1/health
- **API (Swagger):** http://localhost:8080/api/v1/swagger-ui

La portada muestra el estado en vivo de la API, PostgreSQL y Redis. Si los tres salen en verde, el equipo quedó configurado. ✅

---

### 📌 Sobre tu copia privada de secretos

Como el `.env` no viaja por Git, guarda una copia de sus valores en un lugar **privado y seguro**:

- Un gestor de contraseñas (Bitwarden, 1Password, KeePass), **o**
- Un archivo cifrado en tu Drive personal.

**Nunca** los mandes por WhatsApp, correo, ni los subas a ningún repositorio. Cuando configures un equipo nuevo, copias los valores desde ahí.

---

## 3. El día a día: arrancar el proyecto

Una vez configurado el equipo (sección 2), tu arranque diario es corto:

```bash
git pull                 # 1. Trae lo último (ver sección 4)
docker compose up -d     # 2. Levanta PostgreSQL + Redis
pnpm dev                 # 3. Arranca frontend + backend
```

Al terminar de trabajar:

```bash
# guarda tu trabajo (ver sección 4)
git add .
git commit -m "feat: descripción de lo que hiciste"
git push

docker compose down      # opcional: apaga los contenedores
```

---

## 4. Trabajar en dos dispositivos (PC ⇄ laptop)

Lo que sincroniza tu trabajo entre equipos es **Git**, no Docker. Docker solo corre las bases de datos locales de cada máquina (son independientes y no se comunican, y no hace falta que lo hagan).

### La regla de oro

> **Siempre `git pull` al empezar. Siempre `git push` al terminar.**

El 99% de los problemas de trabajar en dos equipos vienen de olvidar esto. Si editas código desactualizado, tendrás conflictos.

### Flujo recomendado

**Cuando terminas en un equipo (ej. la PC en casa):**

```bash
git add .
git commit -m "feat: lo que avancé hoy"
git push
```

**Cuando empiezas en el otro equipo (ej. la laptop):**

```bash
git pull
docker compose up -d
pnpm dev
```

### ¿Y si el `git pull` trajo migraciones nuevas?

Si en un equipo creaste una migración Flyway nueva (un archivo `V__*.sql`), al hacer `pull` en el otro equipo esos cambios llegan al código pero **aún no están aplicados** a tu base local. Corre la migración:

```bash
pnpm db:migrate    # aplica solo las migraciones nuevas que falten
```

Si quieres partir de datos limpios idénticos al seed base:

```bash
pnpm db:reset      # (si tienes este script) borra y reconstruye desde cero
# o manualmente:
docker compose down -v && docker compose up -d && pnpm db:migrate && pnpm db:seed
```

### Qué se sincroniza y qué no

| Elemento | ¿Se sincroniza? | Mecanismo |
|---|---|---|
| Código (frontend + backend) | ✅ Sí | Git (`push` / `pull`) |
| Migraciones Flyway | ✅ Sí | Git (son archivos en el repo) |
| Configuración de Docker (`docker-compose.yml`) | ✅ Sí | Git (es un archivo en el repo) |
| **Datos** de la base local | ❌ No | Se reconstruyen con `db:migrate` + `db:seed` |
| **Secretos** (`.env` de la raíz) | ❌ No | Se copian a mano desde tu gestor de contraseñas |

---

## 5. Comandos de referencia

| Comando | Función |
|---|---|
| `docker compose up -d` | Levanta PostgreSQL + Redis locales |
| `docker compose down` | Detiene los contenedores (conserva los datos) |
| `docker compose down -v` | Detiene y **borra los datos** (reinicio limpio) |
| `docker compose ps` | Ver qué contenedores están corriendo |
| `pnpm install` | Instalar dependencias del frontend |
| `pnpm dev` | Frontend (3000) + backend (8080) en paralelo |
| `pnpm dev:web` | Solo el frontend |
| `pnpm dev:api` | Solo el backend (invoca `mvnw`) |
| `pnpm build` | Build de producción del frontend |
| `pnpm lint` | Linter del frontend |
| `pnpm test` | Tests del backend |
| `pnpm docker:up` / `docker:down` | Atajos de Docker Compose |
| `pnpm docker:reset` | Borra los datos y levanta contenedores limpios |
| `pnpm db:migrate` | Aplicar migraciones Flyway a mano (también corren solas al arrancar) |
| `pnpm db:seed` | Insertar los datos de demostración (idempotente) |
| `pnpm db:studio` | Abrir `psql` contra la base local |
| `pnpm --filter api test:coverage` | Tests + informe de cobertura JaCoCo |
| `git push origin main` | Despliegue automático a producción |

---

## 6. Solución de problemas comunes

**`docker compose up` falla con "Cannot connect to the Docker daemon"**
Docker Desktop no está abierto. Ábrelo y espera a que el ícono deje de animarse, luego reintenta.

**El backend no conecta a la base de datos**
Verifica que los contenedores están arriba (`docker compose ps`) y que `DATABASE_URL` en el `.env` de la raíz apunta a `localhost:5432`. Revisa que el puerto 5432 no esté ocupado por otro PostgreSQL instalado localmente. La portada (http://localhost:3000) te dice cuál de los dos componentes falla.

**"Port 3000/8080/5432 already in use"**
Otro proceso ocupa el puerto. Ciérralo, o cambia el puerto en la configuración. En Windows: `netstat -ano | findstr :5432` para ver qué lo usa.

**Hice `git pull` y el proyecto no arranca**
Probablemente llegaron dependencias nuevas o migraciones. Corre `pnpm install` y luego `pnpm db:migrate`.

**Conflicto de merge en Git**
Editaste el mismo archivo en ambos equipos sin sincronizar. Git te marca las zonas en conflicto con `<<<<<<<` y `>>>>>>>`. Edítalas a mano dejando la versión correcta, luego `git add` y `git commit`. Para evitarlo: respeta la regla de oro (pull al empezar, push al terminar).

**Perdí mi `.env` / configuré una laptop nueva y no arranca**
Recupera los valores desde tu gestor de contraseñas y vuelve a crear el archivo (sección 2, paso 2). Sin secretos válidos, el backend no puede conectar a Cloudinary, clima, etc.

**El frontend falla con "Falta la variable de entorno NEXT_PUBLIC_API_URL"**
No existe el `.env` en la raíz, o tu Node es anterior a 20.12 y no tiene `process.loadEnvFile`. Comprueba con `node --version` y que el archivo esté en la raíz del proyecto (no dentro de `apps/web`).

**`pnpm install` avisa de "Ignored build scripts"**
pnpm 11 exige aprobar los paquetes que ejecutan scripts de instalación. Los del proyecto ya están aprobados en `pnpm-workspace.yaml` (`sharp` y `unrs-resolver`, ambos del toolchain de Next.js). Si aparece uno nuevo, revísalo antes de aprobarlo.

**El build tarda o el `.next` se comporta raro tras cambiar de rama**
Desde Next.js 16, `next dev` escribe en `.next/dev` y `next build` en `.next`, en directorios separados, así que ambos pueden correr a la vez. Si algo queda inconsistente, borra la carpeta `.next` completa y repite.

**La base local quedó en un estado raro y quiero empezar de cero**
```bash
docker compose down -v        # borra los datos
docker compose up -d          # contenedores limpios
pnpm db:migrate && pnpm db:seed
```

---

*Este README cumple el RNF-32: un desarrollador nuevo (o tú, en un equipo nuevo) puede correr el proyecto sin asistencia siguiendo estos pasos.*
