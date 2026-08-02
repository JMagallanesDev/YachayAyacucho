package com.huamanga.tourism.common.seguridad;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Respuesta 403 cuando el usuario esta autenticado pero le falta el rol.
 *
 * <p>La diferencia con el 401 importa y es semantica: 401 significa "no se
 * quien eres", 403 significa "se quien eres y no puedes". Devolver 401 en
 * ambos casos haria que el frontend mandara al usuario a iniciar sesion
 * cuando ya la tiene iniciada, en un bucle sin salida.</p>
 */
@Component
public class ManejadorAccesoDenegado implements AccessDeniedHandler {

    private static final Logger log = LoggerFactory.getLogger(ManejadorAccesoDenegado.class);

    @Override
    public void handle(HttpServletRequest peticion,
                       HttpServletResponse respuesta,
                       AccessDeniedException excepcion) throws IOException {
        var autenticacion = SecurityContextHolder.getContext().getAuthentication();
        // Se registra: un usuario tocando endpoints que no le corresponden es
        // justo lo que interesa ver en una auditoria de seguridad.
        log.warn("Acceso denegado a {} sobre {} {}",
                autenticacion != null ? autenticacion.getName() : "anonimo",
                peticion.getMethod(), peticion.getRequestURI());

        respuesta.setStatus(HttpStatus.FORBIDDEN.value());
        respuesta.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        respuesta.getWriter().write("""
                {"type":"https://yachay-ayacucho.pe/errores/acceso-denegado",\
                "title":"Acceso denegado",\
                "status":403,\
                "detail":"Tu cuenta no tiene permisos para realizar esta accion.",\
                "errorCode":"acceso-denegado"}""");
    }
}
