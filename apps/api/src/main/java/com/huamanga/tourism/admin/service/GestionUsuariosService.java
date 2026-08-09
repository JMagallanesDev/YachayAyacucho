package com.huamanga.tourism.admin.service;

import com.huamanga.tourism.admin.dto.CambiarUsuarioRequest;
import com.huamanga.tourism.admin.dto.UsuarioAdminResponse;
import com.huamanga.tourism.common.exception.RecursoNoEncontradoException;
import com.huamanga.tourism.common.seguridad.UsuarioActual;
import com.huamanga.tourism.usuario.domain.EstadoUsuario;
import com.huamanga.tourism.usuario.domain.NombreRol;
import com.huamanga.tourism.usuario.domain.Rol;
import com.huamanga.tourism.usuario.domain.Usuario;
import com.huamanga.tourism.usuario.repository.RolRepository;
import com.huamanga.tourism.usuario.repository.UsuarioRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Gestion de cuentas y roles (RF-51).
 *
 * <p><strong>Dos barreras que no son opcionales.</strong> Un panel de
 * administracion es un sitio donde un clic distraido puede dejar el sistema
 * inservible, y estas dos puertas cierran las formas de conseguirlo:</p>
 *
 * <ol>
 *   <li><strong>Nadie se cambia el rol a si mismo.</strong> Quitarse ADMIN es
 *       irreversible desde dentro: en cuanto la respuesta vuelve, el panel deja
 *       de responderte y ya no puedes deshacerlo. Solo otro administrador puede
 *       cambiarte el rol.</li>
 *   <li><strong>Nunca se llega a cero administradores.</strong> Aunque sean dos
 *       personas distintas, degradar o suspender al ultimo ADMIN dejaria el
 *       panel cerrado para siempre y sin ninguna forma de entrar salvo tocar la
 *       base de datos a mano.</li>
 * </ol>
 *
 * <p>La segunda barrera cuenta administradores <strong>activos</strong>, no
 * simplemente con rol ADMIN: suspender al ultimo administrador deja el sistema
 * igual de bloqueado que degradarlo, y es la via que se olvida al programar esto.</p>
 */
@Service
public class GestionUsuariosService {

    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;
    private final RegistroActividadService auditoria;

    public GestionUsuariosService(UsuarioRepository usuarioRepository,
                                  RolRepository rolRepository,
                                  RegistroActividadService auditoria) {
        this.usuarioRepository = usuarioRepository;
        this.rolRepository = rolRepository;
        this.auditoria = auditoria;
    }

    @Transactional(readOnly = true)
    public List<UsuarioAdminResponse> listar() {
        UUID yo = UsuarioActual.id().orElse(null);

        return usuarioRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt")).stream()
                .map(usuario -> aRespuesta(usuario, yo))
                .toList();
    }

    /**
     * Cambia el rol y/o el estado de una cuenta.
     *
     * @throws NoPuedesCambiarteElRolException  si intenta cambiarse a si mismo
     * @throws UltimoAdministradorException     si dejaria el sistema sin admins
     */
    @Transactional
    public UsuarioAdminResponse cambiar(UUID usuarioId, CambiarUsuarioRequest peticion) {
        UUID yo = UsuarioActual.idObligatorio();

        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new RecursoNoEncontradoException("usuario", usuarioId.toString()));

        boolean cambiaRol = peticion.rol() != null && peticion.rol() != usuario.getRol().getNombre();
        boolean cambiaEstado = peticion.estado() != null && peticion.estado() != usuario.getEstado();

        if (cambiaRol && usuarioId.equals(yo)) {
            throw new NoPuedesCambiarteElRolException();
        }

        // Se comprueba ANTES de tocar nada: si la operacion fuera a dejar el
        // sistema sin administradores activos, no se aplica ni la mitad.
        if (dejariaSinAdministradores(usuario, peticion, cambiaRol, cambiaEstado)) {
            throw new UltimoAdministradorException();
        }

        Map<String, String> detalles = new HashMap<>();
        detalles.put("email", usuario.getEmail());

        if (cambiaRol) {
            Rol nuevo = rolRepository.findByNombre(peticion.rol())
                    .orElseThrow(() -> new RecursoNoEncontradoException("rol", peticion.rol().name()));
            detalles.put("rolAnterior", usuario.getRol().getNombre().name());
            detalles.put("rolNuevo", nuevo.getNombre().name());
            usuario.setRol(nuevo);
        }

        if (cambiaEstado) {
            detalles.put("estadoAnterior", usuario.getEstado().name());
            detalles.put("estadoNuevo", peticion.estado().name());
            usuario.setEstado(peticion.estado());
        }

        Usuario guardado = usuarioRepository.save(usuario);

        if (cambiaRol || cambiaEstado) {
            auditoria.registrar(cambiaRol ? "CAMBIAR_ROL" : "CAMBIAR_ESTADO_USUARIO",
                    "Usuario", usuarioId, detalles);
        }

        return aRespuesta(guardado, yo);
    }

    /**
     * ¿Esta operacion apagaria al ultimo administrador activo?
     *
     * <p>Solo puede ocurrir si el afectado es hoy un ADMIN activo y la operacion
     * le quita el rol o lo deja fuera de servicio. Si no es su caso, no hace
     * falta ni contar.</p>
     */
    private boolean dejariaSinAdministradores(Usuario usuario, CambiarUsuarioRequest peticion,
                                              boolean cambiaRol, boolean cambiaEstado) {
        boolean esAdminActivo = usuario.getRol().getNombre() == NombreRol.ADMIN
                && usuario.getEstado() == EstadoUsuario.ACTIVO;

        if (!esAdminActivo) {
            return false;
        }

        boolean pierdeElRol = cambiaRol && peticion.rol() != NombreRol.ADMIN;
        boolean dejaDeEstarActivo = cambiaEstado && peticion.estado() != EstadoUsuario.ACTIVO;

        if (!pierdeElRol && !dejaDeEstarActivo) {
            return false;
        }

        return usuarioRepository.contarAdministradoresActivos() <= 1;
    }

    private UsuarioAdminResponse aRespuesta(Usuario usuario, UUID yo) {
        return new UsuarioAdminResponse(
                usuario.getId(),
                usuario.getEmail(),
                usuario.getNombre(),
                usuario.getRol().getNombre(),
                usuario.getEstado(),
                usuario.getCreatedAt(),
                usuario.getId().equals(yo));
    }

    /** Cambiarse el rol a uno mismo es irreversible desde dentro del panel. */
    public static class NoPuedesCambiarteElRolException extends RuntimeException {
        public NoPuedesCambiarteElRolException() {
            super("No puedes cambiar tu propio rol; pideselo a otro administrador");
        }
    }

    /** Quedarse sin administradores activos cierra el panel para siempre. */
    public static class UltimoAdministradorException extends RuntimeException {
        public UltimoAdministradorException() {
            super("Es el ultimo administrador activo del sistema");
        }
    }
}
