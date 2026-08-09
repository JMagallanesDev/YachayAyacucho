package com.huamanga.tourism.admin.service;

import com.huamanga.tourism.admin.domain.RegistroActividad;
import com.huamanga.tourism.admin.repository.RegistroActividadRepository;
import com.huamanga.tourism.common.seguridad.ResolutorIpCliente;
import com.huamanga.tourism.common.seguridad.UsuarioActual;
import com.huamanga.tourism.usuario.repository.UsuarioRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Registro de acciones administrativas (RF-56).
 *
 * <p><strong>Por que llamadas explicitas y no un {@code @EntityListener}.</strong>
 * Un log de auditoria tiene que poder decir «la administradora Rosa
 * <em>rechazo</em> la foto X», y esa intencion no esta en el diff de la fila: un
 * listener solo ve que {@code estado} paso de PENDIENTE a RECHAZADA, y no sabe
 * si fue una moderacion, una correccion o una migracion. Ademas, escribir una
 * entidad nueva desde un callback de JPA obliga a pelearse con la reentrada del
 * EntityManager. Llamar a {@code registrar(...)} desde el servicio que ejecuta la
 * accion es mas simple y guarda mas informacion.</p>
 *
 * <p><strong>Aqui la IP SI se guarda</strong>, al contrario que en los reportes
 * ciudadanos del Bloque 8. No es una contradiccion: alli el objetivo era proteger
 * a quien denuncia; aqui la fila ya lleva el {@code usuario_id} de un
 * administrador identificado ejerciendo privilegios sobre contenido ajeno, de
 * modo que la IP no anade identificabilidad —eso ya esta— sino trazabilidad de
 * desde donde se ejercio ese poder.</p>
 *
 * <p>Ningun fallo al auditar tumba la accion auditada: si el log falla, se
 * registra el problema y la moderacion sigue adelante. Perder una linea de
 * bitacora es peor que perder la accion, pero mucho menos.</p>
 */
@Service
public class RegistroActividadService {

    private static final Logger log = LoggerFactory.getLogger(RegistroActividadService.class);

    private final RegistroActividadRepository repositorio;
    private final UsuarioRepository usuarioRepository;
    private final ResolutorIpCliente resolutorIp;

    public RegistroActividadService(RegistroActividadRepository repositorio,
                                    UsuarioRepository usuarioRepository,
                                    ResolutorIpCliente resolutorIp) {
        this.repositorio = repositorio;
        this.usuarioRepository = usuarioRepository;
        this.resolutorIp = resolutorIp;
    }

    /**
     * Deja constancia de una accion administrativa.
     *
     * @param accion   verbo en mayusculas, p. ej. {@code APROBAR_FOTO}
     * @param entidad  nombre de la entidad afectada, p. ej. {@code Foto}
     * @param entidadId identificador de la fila afectada, si lo hay
     * @param detalles pares clave-valor con el contexto de la accion
     */
    @Transactional
    public void registrar(String accion, String entidad, UUID entidadId,
                          Map<String, String> detalles) {
        try {
            UUID autor = UsuarioActual.id().orElse(null);
            if (autor == null) {
                // La columna usuario_id es NOT NULL: un log de auditoria sin
                // autor no significa nada y es mejor no escribirlo.
                log.warn("Accion {} sobre {} sin usuario autenticado: no se audita", accion, entidad);
                return;
            }

            RegistroActividad registro = new RegistroActividad();
            registro.setUsuario(usuarioRepository.getReferenceById(autor));
            registro.setAccion(accion);
            registro.setEntidad(entidad);
            registro.setEntidadId(entidadId);
            registro.setDetalles(aJson(detalles));
            registro.setIp(ipDeLaPeticionActual());

            repositorio.save(registro);

        } catch (Exception e) {
            log.error("No se pudo auditar la accion {} sobre {}: {}", accion, entidad, e.getMessage());
        }
    }

    public void registrar(String accion, String entidad, UUID entidadId) {
        registrar(accion, entidad, entidadId, Map.of());
    }

    /**
     * Serializa los detalles a JSON sin traer una dependencia.
     *
     * <p>Son pares de cadenas cortas escritas por el propio codigo, no entrada
     * de usuario con estructura libre, asi que basta con escapar comillas y
     * barras. Se hace a mano porque inyectar un ObjectMapper aqui solo para esto
     * seria mas ruido que valor.</p>
     */
    private String aJson(Map<String, String> detalles) {
        if (detalles == null || detalles.isEmpty()) {
            return null;
        }
        return detalles.entrySet().stream()
                .map(e -> "\"" + escapar(e.getKey()) + "\":\"" + escapar(e.getValue()) + "\"")
                .collect(Collectors.joining(",", "{", "}"));
    }

    private String escapar(String texto) {
        return texto == null ? "" : texto.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    /**
     * IP de la peticion en curso, si la hay.
     *
     * <p>Se saca del {@code RequestContextHolder} en vez de recibir el
     * {@code HttpServletRequest} por parametro para que auditar no obligue a
     * arrastrar la peticion por toda la cadena de servicios. Fuera de una
     * peticion HTTP —un job, un test— devuelve null, que la columna admite.</p>
     */
    private String ipDeLaPeticionActual() {
        if (RequestContextHolder.getRequestAttributes()
                instanceof ServletRequestAttributes atributos) {
            HttpServletRequest peticion = atributos.getRequest();
            return resolutorIp.resolver(peticion);
        }
        return null;
    }
}
