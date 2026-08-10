# Pruebas de carga — política y cómo ejecutarlas

## La regla que no se negocia

> **Estas pruebas se ejecutan contra el Docker local o un staging temporal.
> NUNCA contra los servicios gratuitos de producción.**

Agotar la cuota de MapTiler, OpenWeather, Supabase o Upstash antes de la
sustentación sería un daño irreversible causado por una prueba que se puede
hacer en local. Por eso el plan `.jmx`:

- Tiene `host=localhost` y `puerto=8080` como valores por defecto: apuntar a
  otro sitio exige escribirlo a mano, a conciencia.
- Solo golpea endpoints que sirve **nuestro propio backend** contra PostgreSQL
  y Redis locales. No hay ni un sampler que llame a un proveedor externo.
- El único endpoint que *podría* salir a internet es `/clima`, y se calienta su
  caché de Redis **antes** de empezar, de modo que durante la prueba se sirve
  desde memoria. Una sola llamada externa en total.

## Preparación

```bash
docker compose up -d                       # PostgreSQL y Redis locales
cd apps/api && ./mvnw package -DskipTests  # el jar, no `spring-boot:run`
java -jar target/api-0.0.1-SNAPSHOT.jar    # ver nota abajo
curl -s http://localhost:8080/api/v1/clima # calienta la caché del clima
```

**Se ejecuta el jar y no `mvnw spring-boot:run`**: bajo carga alta, el proceso
que Maven bifurca terminaba con una violación de acceso del propio JVM
(`exit code -1073741819`). El jar aguanta los 500 usuarios sin un error, y
además es lo que se despliega de verdad.

## Escenarios

```bash
JM=ruta/a/apache-jmeter-5.6.3/bin/jmeter
TOKEN=$(grep '^YACHAY_TOKEN_INTERNO=' .env | cut -d= -f2)

# Carga base: 50 usuarios, 60 s. Objetivo P95 < 500 ms.
$JM -n -t scripts/carga/yachay-carga.jmx -l base.jtl \
   -Jusuarios=50 -Jrampa=10 -Jduracion=60 -Jpausa=1000 -JtokenInterno="$TOKEN"

# Pico de Semana Santa: rampa a 500 usuarios, 120 s.
$JM -n -t scripts/carga/yachay-carga.jmx -l pico.jtl \
   -Jusuarios=500 -Jrampa=60 -Jduracion=120 -Jpausa=1000 -JtokenInterno="$TOKEN"
```

### Por qué cada parámetro es como es

**`pausa=1000`** — sin tiempo de reflexión, «50 usuarios concurrentes» no simula
50 personas navegando sino 50 bucles cerrados: se generan 9 000 peticiones por
segundo y lo que se mide es el límite del generador, no el del servidor. Con la
pausa, cada usuario hace una petición por segundo, que es lo que hace una
persona.

**`keep-alive` activado** — sin él, cada petición abre y cierra un socket. Con
500 usuarios eso agota los puertos efímeros de Windows y JMeter devuelve
`BindException` en el 49 % de las muestras: otra vez el límite del cliente. Un
navegador real reutiliza la conexión, así que activarlo es además lo fiel.

**`tokenInterno`** — exime a las peticiones del rate limiting. Es correcto aquí:
los 500 usuarios simulados salen todos de `127.0.0.1` y comparten un único
contador por IP, lo cual es un artefacto de la prueba y no de producción. Sin la
exención se mediría el camino del 429 y nada más.

## Análisis

`base.jtl` y `pico.jtl` son CSV. Los percentiles por endpoint salen con:

```bash
node scripts/carga/percentiles.mjs base.jtl "Carga base"
```
