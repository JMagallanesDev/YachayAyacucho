package com.huamanga.tourism.common.exception;

import com.huamanga.tourism.auth.exception.CredencialesInvalidasException;
import com.huamanga.tourism.auth.exception.EmailYaRegistradoException;
import com.huamanga.tourism.auth.exception.RefreshTokenInvalidoException;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.jspecify.annotations.Nullable;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.net.URI;
import java.time.Clock;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Traduce las excepciones de dominio a respuestas HTTP con ProblemDetail
 * (RFC 7807), como exige el CLAUDE.md.
 *
 * <p>Separa deteccion de reporte: los services lanzan excepciones de negocio
 * sin saber nada de HTTP, y aqui se decide el codigo y el cuerpo. Asi el
 * formato de error es uno solo en todo el API y el frontend puede parsearlo
 * de forma uniforme.</p>
 *
 * <p>Ninguna respuesta incluye trazas ni mensajes internos (RNF-23): al
 * cliente se le da un texto claro en espanol y la causa completa queda en el
 * log del servidor.</p>
 */
@RestControllerAdvice
public class ManejadorErroresGlobal extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ManejadorErroresGlobal.class);
    private static final String BASE_TIPOS = "https://yachay-ayacucho.pe/errores/";

    private final Clock clock;

    public ManejadorErroresGlobal(Clock clock) {
        this.clock = clock;
    }

    @ExceptionHandler(CredencialesInvalidasException.class)
    public ProblemDetail manejarCredencialesInvalidas(HttpServletRequest peticion) {
        // Se registra el intento fallido pero NO el correo probado, para no
        // acabar con una lista de correos validos en los logs.
        log.info("Intento de autenticacion fallido desde {}", peticion.getRemoteAddr());
        return construir(HttpStatus.UNAUTHORIZED, "credenciales-invalidas",
                "Credenciales invalidas",
                "El correo o la contrasena no son correctos.", peticion);
    }

    @ExceptionHandler(RefreshTokenInvalidoException.class)
    public ProblemDetail manejarRefreshInvalido(RefreshTokenInvalidoException ex,
                                                HttpServletRequest peticion) {
        log.info("Refresh rechazado: {}", ex.getMessage());
        return construir(HttpStatus.UNAUTHORIZED, "sesion-expirada",
                "Sesion no valida",
                "Tu sesion ha caducado. Vuelve a iniciar sesion.", peticion);
    }

    @ExceptionHandler(EmailYaRegistradoException.class)
    public ProblemDetail manejarEmailDuplicado(HttpServletRequest peticion) {
        return construir(HttpStatus.CONFLICT, "email-registrado",
                "Correo ya registrado",
                "Ya existe una cuenta con ese correo.", peticion);
    }

    /** Errores de Bean Validation: 400 con el detalle campo a campo. */
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex,
            @Nullable HttpHeaders cabeceras,
            HttpStatusCode estado,
            WebRequest peticion) {

        Map<String, String> errores = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(error -> errores.putIfAbsent(error.getField(), error.getDefaultMessage()));

        ProblemDetail problema = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        problema.setType(URI.create(BASE_TIPOS + "validacion"));
        problema.setTitle("Datos invalidos");
        problema.setDetail("Revisa los campos marcados.");
        problema.setProperty("errores", errores);
        problema.setProperty("timestamp", clock.instant());

        return ResponseEntity.badRequest().body(problema);
    }

    private ProblemDetail construir(HttpStatus estado, String codigo, String titulo,
                                    String detalle, HttpServletRequest peticion) {
        ProblemDetail problema = ProblemDetail.forStatus(estado);
        problema.setType(URI.create(BASE_TIPOS + codigo));
        problema.setTitle(titulo);
        problema.setDetail(detalle);
        problema.setInstance(URI.create(peticion.getRequestURI()));
        problema.setProperty("errorCode", codigo);
        problema.setProperty("timestamp", clock.instant());
        return problema;
    }
}
