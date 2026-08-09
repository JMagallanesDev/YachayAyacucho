package com.huamanga.tourism.negocio.service;

import com.huamanga.tourism.common.exception.RecursoNoEncontradoException;
import com.huamanga.tourism.common.seguridad.UsuarioActual;
import com.huamanga.tourism.negocio.domain.Negocio;
import com.huamanga.tourism.negocio.repository.NegocioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * La unica puerta por la que se accede al negocio de alguien (RF-107).
 *
 * <p><strong>Por que existe esta clase en vez de un {@code if} en cada
 * metodo.</strong> {@code NEGOCIO} es el primer rol intermedio del sistema, y
 * los roles intermedios tienen un fallo caracteristico: se comprueba <em>que el
 * usuario tenga el rol</em> y se olvida comprobar <em>que el recurso sea
 * suyo</em>. El resultado es que cualquier dueno de negocio puede editar el de
 * otro sin mas que cambiar un identificador en la URL. Es control de acceso roto
 * a nivel de objeto, y es de las vulnerabilidades mas comunes que existen.</p>
 *
 * <p>Aqui el rol no autoriza nada. Lo unico que autoriza es <strong>ser el
 * gestor de esa fila concreta</strong>. Un {@code @PreAuthorize("hasRole('NEGOCIO')")}
 * seria insuficiente por si solo, y por eso ningun servicio de este paquete
 * toca un negocio sin pasar antes por aqui.</p>
 *
 * <p><strong>Sobre el 403 frente al 404.</strong> En otras partes del sistema
 * —un lugar en borrador, un evento sin publicar— se responde 404 a quien no
 * tiene permiso, para no confirmar siquiera que el recurso existe. Aqui se
 * responde <strong>403</strong>, y es la eleccion correcta por una razon
 * concreta: el directorio de negocios es publico y sus identificadores estan a
 * la vista de cualquiera en la URL de cada ficha. Ocultar la existencia de algo
 * que ya se publica no protegeria nada, y a cambio daria un mensaje de error
 * enganoso a un dueno legitimo que se equivoco de pestana.</p>
 */
@Component
public class GuardaDePropiedad {

    private static final Logger log = LoggerFactory.getLogger(GuardaDePropiedad.class);

    private final NegocioRepository negocioRepository;

    public GuardaDePropiedad(NegocioRepository negocioRepository) {
        this.negocioRepository = negocioRepository;
    }

    /**
     * Carga un negocio comprobando que pertenece a quien hace la peticion.
     *
     * @throws RecursoNoEncontradoException si el negocio no existe
     * @throws NegocioAjenoException        si existe pero es de otra persona
     */
    public Negocio mio(UUID negocioId) {
        UUID yo = UsuarioActual.idObligatorio();

        Negocio negocio = negocioRepository.findById(negocioId)
                .orElseThrow(() -> new RecursoNoEncontradoException("negocio", negocioId.toString()));

        if (!negocio.getUsuario().getId().equals(yo)) {
            // Se registra a proposito: un intento de entrar en el negocio de
            // otro es justo la senal que interesa encontrar en los logs.
            log.warn("El usuario {} intento acceder al negocio {}, que gestiona otra persona",
                    yo, negocioId);
            throw new NegocioAjenoException(negocioId);
        }

        return negocio;
    }

    /** Se tiene sesion y hasta el rol, pero el negocio es de otra persona. */
    public static class NegocioAjenoException extends RuntimeException {
        public NegocioAjenoException(UUID negocioId) {
            super("El negocio " + negocioId + " lo gestiona otra persona");
        }
    }
}
