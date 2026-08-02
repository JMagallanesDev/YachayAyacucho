package com.huamanga.tourism.auth.service;

import com.huamanga.tourism.usuario.repository.UsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Da contrasena utilizable al administrador del seed de demostracion.
 *
 * <p>Cierra el cabo que dejo el Bloque 1: la fila del admin se creaba con el
 * marcador {@code SIN_HASH_VALIDO_HASTA_BLOQUE_2} porque BCrypt aun no
 * existia en el proyecto.</p>
 *
 * <p><strong>Por que aqui y no en el SQL del seed.</strong> Un hash BCrypt
 * escrito en {@code seed_demo.sql} seria una contrasena conocida en el
 * historial de Git para siempre, y este proyecto se publica. Leyendola del
 * {@code .env} —que esta en {@code .gitignore}— no queda ningun secreto en el
 * repositorio y cada equipo usa la suya.</p>
 *
 * <p>Solo se activa en perfil {@code dev} y solo actua si el hash sigue
 * siendo el marcador: nunca pisa una contrasena que alguien ya cambio, y en
 * produccion no se ejecuta jamas.</p>
 */
@Component
@Profile("dev")
public class InicializadorAdmin implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(InicializadorAdmin.class);

    private static final String EMAIL_ADMIN = "admin@yachay-ayacucho.pe";
    private static final String MARCADOR = "SIN_HASH_VALIDO_HASTA_BLOQUE_2";

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final String passwordInicial;

    public InicializadorAdmin(UsuarioRepository usuarioRepository,
                              PasswordEncoder passwordEncoder,
                              @Value("${app.admin.password-inicial:}") String passwordInicial) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.passwordInicial = passwordInicial;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments argumentos) {
        if (passwordInicial == null || passwordInicial.isBlank()) {
            log.info("ADMIN_PASSWORD_INICIAL sin valor: el administrador del seed sigue sin acceso.");
            return;
        }

        usuarioRepository.findByEmail(EMAIL_ADMIN).ifPresent(admin -> {
            if (!MARCADOR.equals(admin.getPasswordHash())) {
                // Ya tiene una contrasena real: no se toca.
                return;
            }
            admin.setPasswordHash(passwordEncoder.encode(passwordInicial));
            usuarioRepository.save(admin);
            log.info("Administrador {} inicializado con la contrasena de ADMIN_PASSWORD_INICIAL.", EMAIL_ADMIN);
        });
    }

    /** Expuesto para los tests. */
    static String emailAdmin() {
        return EMAIL_ADMIN;
    }

    static String marcador() {
        return MARCADOR;
    }
}
