package com.huamanga.tourism.auth.service;

import com.huamanga.tourism.auth.dto.AutenticacionResponse;
import com.huamanga.tourism.auth.dto.LoginRequest;
import com.huamanga.tourism.auth.dto.RegistroRequest;
import com.huamanga.tourism.auth.dto.UsuarioResponse;
import com.huamanga.tourism.auth.exception.CredencialesInvalidasException;
import com.huamanga.tourism.auth.exception.EmailYaRegistradoException;
import com.huamanga.tourism.usuario.domain.EstadoUsuario;
import com.huamanga.tourism.usuario.domain.NombreRol;
import com.huamanga.tourism.usuario.domain.Rol;
import com.huamanga.tourism.usuario.domain.Usuario;
import com.huamanga.tourism.usuario.repository.RolRepository;
import com.huamanga.tourism.usuario.repository.UsuarioRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Registro y autenticacion de usuarios.
 */
@Service
public class AuthService {

    /**
     * Hash senuelo con el que se compara cuando el correo no existe.
     *
     * <p>Sin esto, un login con correo inexistente responderia en microsegundos
     * y uno con correo real pero contrasena mala tardaria lo que tarda BCrypt
     * con coste 12: unos 200 ms. Esa diferencia es medible desde fuera y
     * convierte el login en un detector de correos registrados. Ejecutando el
     * BCrypt igualmente, ambos caminos tardan lo mismo.</p>
     */
    private static final String HASH_SENUELO =
            "$2a$12$C6UzMDM.H6dfI/f/IKcEe.7BOoQMy4HVrGGGm8yXKMFhqTBGNCu9y";

    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;

    public AuthService(UsuarioRepository usuarioRepository,
                       RolRepository rolRepository,
                       PasswordEncoder passwordEncoder,
                       TokenService tokenService) {
        this.usuarioRepository = usuarioRepository;
        this.rolRepository = rolRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
    }

    /**
     * Da de alta un usuario con rol USUARIO (RF-31).
     *
     * <p>Aqui si se distingue el correo duplicado con un 409: es informacion
     * que el propio interesado necesita para entender que ya tiene cuenta, y
     * cualquier formulario de registro la expone igualmente.</p>
     */
    @Transactional
    public UsuarioResponse registrar(RegistroRequest peticion) {
        if (usuarioRepository.existsByEmail(peticion.email())) {
            throw new EmailYaRegistradoException();
        }

        Rol rolUsuario = rolRepository.findByNombre(NombreRol.USUARIO)
                .orElseThrow(() -> new IllegalStateException(
                        "Falta el rol USUARIO: revisa la migracion de catalogos V14"));

        Usuario usuario = new Usuario();
        usuario.setEmail(peticion.email());
        usuario.setNombre(peticion.nombre());
        usuario.setRol(rolUsuario);
        usuario.setEstado(EstadoUsuario.ACTIVO);
        // BCrypt con coste 12 (RNF-12). La contrasena en claro no se guarda
        // en ninguna variable mas alla de esta linea.
        usuario.setPasswordHash(passwordEncoder.encode(peticion.password()));

        return aRespuesta(usuarioRepository.save(usuario));
    }

    /**
     * Verifica credenciales y devuelve el access token (RF-32).
     *
     * <p>Toda ruta de fallo lanza la misma excepcion, con el mismo mensaje y
     * el mismo codigo: correo inexistente, contrasena incorrecta o cuenta
     * suspendida son indistinguibles desde fuera.</p>
     */
    @Transactional(readOnly = true)
    public SesionIniciada autenticar(LoginRequest peticion) {
        Usuario usuario = usuarioRepository.findByEmailConRol(peticion.email()).orElse(null);

        if (usuario == null) {
            passwordEncoder.matches(peticion.password(), HASH_SENUELO);
            throw new CredencialesInvalidasException();
        }
        if (!passwordEncoder.matches(peticion.password(), usuario.getPasswordHash())) {
            throw new CredencialesInvalidasException();
        }
        if (usuario.getEstado() != EstadoUsuario.ACTIVO) {
            throw new CredencialesInvalidasException();
        }

        // Se devuelve tambien la entidad para que el controller pueda emitir
        // el refresh token sin volver a consultar la base.
        return new SesionIniciada(usuario, construirRespuesta(usuario));
    }

    /** Usuario autenticado junto con la respuesta que se le devuelve. */
    public record SesionIniciada(Usuario usuario, AutenticacionResponse respuesta) {
    }

    public AutenticacionResponse construirRespuesta(Usuario usuario) {
        return new AutenticacionResponse(
                tokenService.generarAccessToken(usuario),
                tokenService.segundosDeVidaDelAccessToken(),
                aRespuesta(usuario));
    }

    @Transactional(readOnly = true)
    public UsuarioResponse buscarPorId(UUID id) {
        return usuarioRepository.findById(id)
                .map(this::aRespuesta)
                .orElseThrow(CredencialesInvalidasException::new);
    }

    private UsuarioResponse aRespuesta(Usuario usuario) {
        return new UsuarioResponse(
                usuario.getId(),
                usuario.getEmail(),
                usuario.getNombre(),
                usuario.getRol().getNombre(),
                usuario.getCreatedAt());
    }
}
