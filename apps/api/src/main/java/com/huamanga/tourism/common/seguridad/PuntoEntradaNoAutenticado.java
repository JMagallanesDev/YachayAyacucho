package com.huamanga.tourism.common.seguridad;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Respuesta 401 cuando falta el token o no es valido.
 *
 * <p>El cuerpo sigue el formato ProblemDetail del resto del API y
 * <strong>no dice por que</strong> falló: si el token expiró, si la firma no
 * cuadra o si nunca llegó es informacion que solo aprovecha quien esta
 * probando. El motivo real queda en el log del servidor.</p>
 */
@Component
public class PuntoEntradaNoAutenticado implements AuthenticationEntryPoint {

    @Override
    public void commence(HttpServletRequest peticion,
                         HttpServletResponse respuesta,
                         AuthenticationException excepcion) throws IOException {
        respuesta.setStatus(HttpStatus.UNAUTHORIZED.value());
        respuesta.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        respuesta.getWriter().write("""
                {"type":"https://yachay-ayacucho.pe/errores/no-autenticado",\
                "title":"No autenticado",\
                "status":401,\
                "detail":"Necesitas iniciar sesion para acceder a este recurso.",\
                "errorCode":"no-autenticado"}""");
    }
}
