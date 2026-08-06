package com.huamanga.tourism.foto.service;

import com.huamanga.tourism.foto.config.PropiedadesCloudinary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.Map;
import java.util.TreeMap;

/**
 * Subida y borrado en Cloudinary con peticiones firmadas.
 *
 * <p>El archivo <strong>pasa por el backend</strong> en vez de subirlo el
 * navegador directamente. Cuesta ancho de banda, y a cambio da lo unico que
 * importa aqui: poder mirar los bytes antes de aceptarlos. Con subida directa
 * nunca veriamos el contenido y la validacion de RNF-15 quedaria delegada.</p>
 *
 * <p>El {@code api_secret} nunca sale del servidor: solo se usa para calcular
 * la firma, y ni siquiera aparece en los mensajes de error.</p>
 */
@Component
public class ClienteCloudinary {

    private static final Logger log = LoggerFactory.getLogger(ClienteCloudinary.class);

    private static final Duration TIMEOUT = Duration.ofSeconds(20);

    private final PropiedadesCloudinary propiedades;
    private final RestClient http;

    public ClienteCloudinary(PropiedadesCloudinary propiedades) {
        this.propiedades = propiedades;

        JdkClientHttpRequestFactory fabrica = new JdkClientHttpRequestFactory(
                HttpClient.newBuilder().connectTimeout(TIMEOUT).build());
        // Subir 5 MB por una conexion lenta puede tardar; el corte es generoso
        // pero existe, para que un cuelgue no retenga el hilo indefinidamente.
        fabrica.setReadTimeout(TIMEOUT);

        this.http = RestClient.builder()
                .baseUrl("https://api.cloudinary.com/v1_1")
                .requestFactory(fabrica)
                .build();
    }

    public boolean configurado() {
        return propiedades.configurado();
    }

    /**
     * Sube una imagen ya validada.
     *
     * @param contenido bytes comprobados por {@link ValidadorImagen}
     * @param publicId  identificador que genera el backend; nunca el nombre del
     *                  archivo original
     */
    public Resultado subir(byte[] contenido, String publicId) {
        if (!configurado()) {
            throw new CloudinaryNoConfiguradoException();
        }

        long timestamp = System.currentTimeMillis() / 1000;

        // Parametros que entran en la firma. Se ordenan solos por ser TreeMap,
        // que es justo lo que exige el algoritmo de Cloudinary.
        Map<String, String> firmados = new TreeMap<>();
        firmados.put("public_id", publicId);
        firmados.put("timestamp", String.valueOf(timestamp));
        // Sobrescribir no: dos subidas nunca comparten public_id porque lleva
        // un UUID, y permitirlo abriria la puerta a pisar una foto existente.
        firmados.put("overwrite", "false");
        // Cloudinary conserva el original y sirve versiones optimizadas segun
        // el navegador. Ayuda al RNF-03 sin recomprimir nosotros.
        firmados.put("eager", "f_auto,q_auto,w_1600,c_limit");

        String firma = firmar(firmados);

        MultiValueMap<String, Object> cuerpo = new LinkedMultiValueMap<>();
        firmados.forEach(cuerpo::add);
        cuerpo.add("api_key", propiedades.apiKey());
        cuerpo.add("signature", firma);
        cuerpo.add("file", new ByteArrayResource(contenido) {
            @Override
            public String getFilename() {
                // Multipart exige un nombre. Se inventa uno inocuo: el nombre
                // que envio el cliente no se usa en ningun punto del flujo.
                return "imagen";
            }
        });

        try {
            RespuestaSubida respuesta = http.post()
                    .uri("/{cloud}/image/upload", propiedades.cloudName())
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(cuerpo)
                    .retrieve()
                    .body(RespuestaSubida.class);

            if (respuesta == null || respuesta.secure_url() == null) {
                throw new SubidaFallidaException("Cloudinary no devolvio una URL");
            }
            return new Resultado(respuesta.secure_url(), respuesta.public_id());

        } catch (SubidaFallidaException e) {
            throw e;
        } catch (Exception e) {
            // El mensaje se registra pero no se propaga al cliente: podria
            // contener detalles de la peticion firmada.
            log.error("Fallo la subida a Cloudinary: {}", e.getMessage());
            throw new SubidaFallidaException("No se pudo subir la imagen");
        }
    }

    /**
     * Borra el binario del CDN.
     *
     * <p>Devuelve si lo consiguio en vez de lanzar: al rechazar una foto, lo
     * que manda es el estado en nuestra base. Si el borrado remoto falla, la
     * foto queda rechazada igualmente —deja de verse— y el binario huerfano se
     * anota en el log.</p>
     *
     * <p>La invalidacion del CDN la propaga Cloudinary, y su propia
     * documentacion advierte de que puede tardar. El borrado del original, en
     * cambio, es inmediato: a partir de ese momento no se puede generar ninguna
     * variante nueva.</p>
     */
    public boolean borrar(String publicId) {
        if (!configurado()) {
            return false;
        }

        long timestamp = System.currentTimeMillis() / 1000;
        Map<String, String> firmados = new TreeMap<>();
        firmados.put("public_id", publicId);
        firmados.put("timestamp", String.valueOf(timestamp));
        // invalidate es imprescindible y no es evidente: `destroy` borra el
        // ORIGINAL, pero las versiones transformadas que ya sirvio el CDN
        // siguen cacheadas en el borde. Sin esto, una foto rechazada por
        // inapropiada continuaba respondiendo 200 a quien tuviera su URL,
        // aunque hubiera desaparecido de la galeria.
        firmados.put("invalidate", "true");

        MultiValueMap<String, Object> cuerpo = new LinkedMultiValueMap<>();
        firmados.forEach(cuerpo::add);
        cuerpo.add("api_key", propiedades.apiKey());
        cuerpo.add("signature", firmar(firmados));

        try {
            RespuestaBorrado respuesta = http.post()
                    .uri("/{cloud}/image/destroy", propiedades.cloudName())
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(cuerpo)
                    .retrieve()
                    .body(RespuestaBorrado.class);

            boolean borrado = respuesta != null && "ok".equals(respuesta.result());
            if (!borrado) {
                log.warn("Cloudinary no borro {}: {}", publicId,
                        respuesta != null ? respuesta.result() : "sin respuesta");
            }
            return borrado;

        } catch (Exception e) {
            log.warn("No se pudo borrar {} de Cloudinary: {}", publicId, e.getMessage());
            return false;
        }
    }

    /**
     * Firma segun el algoritmo de Cloudinary.
     *
     * <p>Parametros ordenados alfabeticamente como {@code clave=valor} unidos
     * por {@code &}, con el {@code api_secret} concatenado al final
     * <strong>sin separador</strong>, y SHA-1 en hexadecimal.</p>
     */
    private String firmar(Map<String, String> parametros) {
        String cadena = parametros.entrySet().stream()
                .map(e -> e.getKey() + "=" + e.getValue())
                .reduce((a, b) -> a + "&" + b)
                .orElse("")
                + propiedades.apiSecret();

        try {
            MessageDigest sha1 = MessageDigest.getInstance("SHA-1");
            byte[] resumen = sha1.digest(cadena.getBytes(StandardCharsets.UTF_8));

            StringBuilder hex = new StringBuilder(resumen.length * 2);
            for (byte b : resumen) {
                hex.append(Character.forDigit((b >> 4) & 0xF, 16));
                hex.append(Character.forDigit(b & 0xF, 16));
            }
            return hex.toString();

        } catch (NoSuchAlgorithmException e) {
            // SHA-1 esta en toda JVM; si falta, algo va muy mal.
            throw new IllegalStateException("SHA-1 no disponible", e);
        }
    }

    /** URL publica y public_id con el que se podra borrar despues. */
    public record Resultado(String url, String publicId) {
    }

    // Solo se declara lo que se usa: los campos nuevos que anada Cloudinary se
    // ignoran sin romper nada.
    private record RespuestaSubida(String secure_url, String public_id) {
    }

    private record RespuestaBorrado(String result) {
    }

    public static class CloudinaryNoConfiguradoException extends RuntimeException {
        public CloudinaryNoConfiguradoException() {
            super("Falta CLOUDINARY_URL: la subida de fotos esta desactivada");
        }
    }

    public static class SubidaFallidaException extends RuntimeException {
        public SubidaFallidaException(String mensaje) {
            super(mensaje);
        }
    }
}
