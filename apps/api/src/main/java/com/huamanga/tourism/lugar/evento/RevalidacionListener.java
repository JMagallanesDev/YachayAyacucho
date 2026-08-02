package com.huamanga.tourism.lugar.evento;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * Avisa a Next.js de que debe regenerar la pagina de un lugar (seccion 5.5).
 *
 * <p>Con ISR, las fichas patrimoniales se sirven como HTML pre-generado desde
 * el CDN sin tocar la base de datos. El precio de esa velocidad es que una
 * edicion no se ve hasta que la pagina se regenera. Este webhook cierra ese
 * hueco: el administrador guarda y la pagina publica se actualiza en segundos,
 * sin desplegar nada.</p>
 */
@Component
public class RevalidacionListener {

    private static final Logger log = LoggerFactory.getLogger(RevalidacionListener.class);

    /** El secreto va en cabecera: en la URL acabaria en los logs de acceso. */
    private static final String CABECERA_SECRETO = "X-Revalidate-Secret";

    private final HttpClient cliente;
    private final String urlWebhook;
    private final String secreto;

    public RevalidacionListener(@Value("${app.revalidacion.url:}") String urlWebhook,
                                @Value("${app.revalidacion.secreto:}") String secreto) {
        this.urlWebhook = urlWebhook;
        this.secreto = secreto;
        this.cliente = HttpClient.newBuilder()
                // Timeout corto: guardar un lugar no puede quedarse esperando
                // a que responda un servicio externo.
                .connectTimeout(Duration.ofSeconds(2))
                .build();
    }

    /**
     * Se ejecuta <strong>despues</strong> de confirmar la transaccion.
     *
     * <p>Es la parte que no puede hacerse de otra manera. Si el aviso saliera
     * dentro de la transaccion, Next regeneraria la pagina leyendo datos aun
     * no confirmados —o que acaban en rollback— y quedaria cacheada una
     * version que nunca llego a existir. Con AFTER_COMMIT, cuando Next viene a
     * releer, lo guardado ya es definitivo.</p>
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void alGuardarLugar(LugarGuardadoEvent evento) {
        if (urlWebhook.isBlank() || secreto.isBlank()) {
            log.debug("Revalidacion ISR no configurada; se omite el aviso para {}", evento.slug());
            return;
        }

        try {
            HttpRequest peticion = HttpRequest.newBuilder()
                    .uri(URI.create(urlWebhook))
                    .header("Content-Type", "application/json")
                    .header(CABECERA_SECRETO, secreto)
                    .timeout(Duration.ofSeconds(3))
                    .POST(HttpRequest.BodyPublishers.ofString(
                            "{\"slug\":\"" + evento.slug() + "\"}"))
                    .build();

            HttpResponse<String> respuesta = cliente.send(peticion, HttpResponse.BodyHandlers.ofString());

            if (respuesta.statusCode() == 200) {
                log.info("Revalidacion ISR solicitada para el lugar {}", evento.slug());
            } else {
                log.warn("El webhook de revalidacion respondio {} para el lugar {}",
                        respuesta.statusCode(), evento.slug());
            }
        } catch (InterruptedException ex) {
            // Restaurar la marca de interrupcion es obligatorio: tragarsela
            // dejaria al hilo sin saber que le pidieron parar.
            Thread.currentThread().interrupt();
            log.warn("Aviso de revalidacion interrumpido para el lugar {}", evento.slug());
        } catch (Exception ex) {
            // Deliberadamente no se propaga. El lugar ya esta guardado y
            // confirmado: que Vercel este caido no puede convertirse en un
            // error para el administrador. La pagina se regenerara igualmente
            // cuando venza su revalidacion por tiempo.
            log.error("No se pudo avisar a Next.js para revalidar el lugar {}. "
                    + "La pagina se actualizara cuando expire su cache.", evento.slug(), ex);
        }
    }
}
