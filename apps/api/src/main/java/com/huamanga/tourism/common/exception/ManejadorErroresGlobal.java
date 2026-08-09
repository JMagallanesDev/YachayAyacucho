package com.huamanga.tourism.common.exception;

import com.huamanga.tourism.auth.exception.CredencialesInvalidasException;
import com.huamanga.tourism.auth.exception.EmailYaRegistradoException;
import com.huamanga.tourism.auth.exception.RefreshTokenInvalidoException;
import com.huamanga.tourism.checkin.service.ValidadorProximidad;
import com.huamanga.tourism.common.seguridad.AntiSpam;
import com.huamanga.tourism.evento.service.AdminEventoService;
import com.huamanga.tourism.evento.service.EventoService;
import com.huamanga.tourism.moderacion.service.ReporteContenidoService;
import com.huamanga.tourism.foto.service.ClienteCloudinary;
import com.huamanga.tourism.foto.service.FotoService;
import com.huamanga.tourism.foto.service.ValidadorImagen;
import com.huamanga.tourism.lugar.service.LugarService;
import com.huamanga.tourism.reporte.service.AntiSpamAnonimo;
import com.huamanga.tourism.reporte.service.ReporteService;
import com.huamanga.tourism.resena.service.ResenaService;
import jakarta.servlet.http.HttpServletRequest;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.net.URI;
import java.time.Clock;
import java.util.LinkedHashMap;
import java.util.Locale;
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
 * <p>Desde el Bloque 3 los textos salen del {@code MessageSource} en el idioma
 * que pida el cliente, en vez de estar incrustados en espanol: de poco sirve
 * traducir la interfaz si el mensaje de error llega en otro idioma justo
 * cuando algo falla.</p>
 *
 * <p>Ninguna respuesta incluye trazas ni mensajes internos (RNF-23): al
 * cliente se le da un texto claro y la causa completa queda en el log.</p>
 */
@RestControllerAdvice
public class ManejadorErroresGlobal extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ManejadorErroresGlobal.class);
    private static final String BASE_TIPOS = "https://yachay-ayacucho.pe/errores/";

    private final Clock clock;
    private final MessageSource mensajes;

    public ManejadorErroresGlobal(Clock clock, MessageSource mensajes) {
        this.clock = clock;
        this.mensajes = mensajes;
    }

    @ExceptionHandler(CredencialesInvalidasException.class)
    public ProblemDetail manejarCredencialesInvalidas(HttpServletRequest peticion) {
        // Se registra el intento fallido pero NO el correo probado, para no
        // acabar con una lista de correos validos en los logs.
        log.info("Intento de autenticacion fallido desde {}", peticion.getRemoteAddr());
        return construir(HttpStatus.UNAUTHORIZED, "credenciales-invalidas", peticion);
    }

    @ExceptionHandler(RefreshTokenInvalidoException.class)
    public ProblemDetail manejarRefreshInvalido(RefreshTokenInvalidoException ex,
                                                HttpServletRequest peticion) {
        log.info("Refresh rechazado: {}", ex.getMessage());
        return construir(HttpStatus.UNAUTHORIZED, "sesion-expirada", peticion);
    }

    @ExceptionHandler(EmailYaRegistradoException.class)
    public ProblemDetail manejarEmailDuplicado(HttpServletRequest peticion) {
        return construir(HttpStatus.CONFLICT, "email-registrado", peticion);
    }

    @ExceptionHandler(RecursoNoEncontradoException.class)
    public ProblemDetail manejarNoEncontrado(RecursoNoEncontradoException ex,
                                             HttpServletRequest peticion) {
        log.debug("Recurso no encontrado: {}", ex.getMessage());
        return construir(HttpStatus.NOT_FOUND, "no-encontrado", peticion);
    }

    @ExceptionHandler(LugarService.SlugDuplicadoException.class)
    public ProblemDetail manejarSlugDuplicado(HttpServletRequest peticion) {
        return construir(HttpStatus.CONFLICT, "slug-duplicado", peticion);
    }

    // ---------------------------------------------------------------
    //  Bloque 6: resenas y fotos
    // ---------------------------------------------------------------

    @ExceptionHandler(ResenaService.ResenaDuplicadaException.class)
    public ProblemDetail manejarResenaDuplicada(HttpServletRequest peticion) {
        return construir(HttpStatus.CONFLICT, "resena-duplicada", peticion);
    }

    /**
     * Editar o borrar contenido ajeno.
     *
     * <p>403 y no 404: quien lo intenta ya sabe que la resena existe —la esta
     * viendo—, asi que ocultarlo no aporta nada y confundiria el diagnostico.</p>
     */
    @ExceptionHandler(ResenaService.ResenaAjenaException.class)
    public ProblemDetail manejarResenaAjena(HttpServletRequest peticion) {
        return construir(HttpStatus.FORBIDDEN, "resena-ajena", peticion);
    }

    @ExceptionHandler(FotoService.DemasiadasFotosException.class)
    public ProblemDetail manejarDemasiadasFotos(HttpServletRequest peticion) {
        return construir(HttpStatus.CONFLICT, "limite-fotos", peticion);
    }

    /**
     * Archivo que no supera la validacion de imagen (RNF-15).
     *
     * <p>El codigo concreto viaja aparte para poder traducir el motivo, pero
     * <strong>sin detallar la comprobacion que fallo</strong>: quien este
     * probando que cuela no necesita saber si le paro la firma de bytes o la
     * decodificacion.</p>
     */
    @ExceptionHandler(ValidadorImagen.ImagenInvalidaException.class)
    public ProblemDetail manejarImagenInvalida(ValidadorImagen.ImagenInvalidaException error,
                                               HttpServletRequest peticion) {
        ProblemDetail problema = construir(HttpStatus.BAD_REQUEST, "imagen-invalida", peticion);
        problema.setProperty("motivo", error.codigo());
        return problema;
    }

    @ExceptionHandler(ClienteCloudinary.CloudinaryNoConfiguradoException.class)
    public ProblemDetail manejarCloudinarySinConfigurar(HttpServletRequest peticion) {
        return construir(HttpStatus.SERVICE_UNAVAILABLE, "fotos-no-configuradas", peticion);
    }

    @ExceptionHandler(ClienteCloudinary.SubidaFallidaException.class)
    public ProblemDetail manejarSubidaFallida(HttpServletRequest peticion) {
        return construir(HttpStatus.BAD_GATEWAY, "subida-fallida", peticion);
    }

    // ---------------------------------------------------------------
    //  Bloque 7: check-in y reportes
    // ---------------------------------------------------------------

    /**
     * Check-in fuera de radio.
     *
     * <p>Se devuelve la distancia real y el radio exigido. No es una fuga de
     * informacion: quien hace la peticion ya conoce su propia posicion y las
     * coordenadas del lugar son publicas. A cambio, permite decir "estas a
     * 340 m" en vez de un "no se pudo" que nadie sabe como resolver.</p>
     */
    @ExceptionHandler(ValidadorProximidad.DemasiadoLejosException.class)
    public ProblemDetail manejarDemasiadoLejos(ValidadorProximidad.DemasiadoLejosException error,
                                               HttpServletRequest peticion) {
        ProblemDetail problema = construir(HttpStatus.UNPROCESSABLE_CONTENT, "demasiado-lejos", peticion);
        problema.setProperty("distanciaMetros", error.distancia());
        problema.setProperty("radioMetros", error.radio());
        return problema;
    }

    @ExceptionHandler(ValidadorProximidad.PrecisionInsuficienteException.class)
    public ProblemDetail manejarPrecisionInsuficiente(HttpServletRequest peticion) {
        return construir(HttpStatus.UNPROCESSABLE_CONTENT, "precision-insuficiente", peticion);
    }

    @ExceptionHandler(ValidadorProximidad.YaHizoCheckInException.class)
    public ProblemDetail manejarCheckInRepetido(HttpServletRequest peticion) {
        return construir(HttpStatus.CONFLICT, "checkin-repetido", peticion);
    }

    @ExceptionHandler(ValidadorProximidad.SaltoImposibleException.class)
    public ProblemDetail manejarSaltoImposible(HttpServletRequest peticion) {
        return construir(HttpStatus.UNPROCESSABLE_CONTENT, "salto-imposible", peticion);
    }

    @ExceptionHandler(ReporteContenidoService.ReporteDuplicadoException.class)
    public ProblemDetail manejarReporteDuplicado(HttpServletRequest peticion) {
        return construir(HttpStatus.CONFLICT, "reporte-duplicado", peticion);
    }

    @ExceptionHandler(ReporteContenidoService.AutorreporteException.class)
    public ProblemDetail manejarAutorreporte(HttpServletRequest peticion) {
        return construir(HttpStatus.UNPROCESSABLE_CONTENT, "autorreporte", peticion);
    }

    // ---------------------------------------------------------------
    //  Bloque 8: reportes ciudadanos
    // ---------------------------------------------------------------

    @ExceptionHandler(ReporteService.FueraDeAyacuchoException.class)
    public ProblemDetail manejarFueraDeAyacucho(HttpServletRequest peticion) {
        return construir(HttpStatus.BAD_REQUEST, "fuera-de-ayacucho", peticion);
    }

    @ExceptionHandler(ReporteService.DemasiadasFotosException.class)
    public ProblemDetail manejarDemasiadasFotosReporte(HttpServletRequest peticion) {
        return construir(HttpStatus.BAD_REQUEST, "limite-fotos", peticion);
    }

    /**
     * Cupo diario de reportes agotado.
     *
     * <p>El mensaje no menciona la IP ni ningun identificador, porque no hay
     * ninguno que mencionar: el contador vive en Redis bajo una huella HMAC de
     * la que no se puede recuperar el origen.</p>
     */
    @ExceptionHandler(AntiSpamAnonimo.DemasiadosReportesException.class)
    public ProblemDetail manejarDemasiadosReportes(HttpServletRequest peticion) {
        return construir(HttpStatus.TOO_MANY_REQUESTS, "limite-reportes", peticion);
    }

    @ExceptionHandler(AntiSpam.DemasiadasPeticionesException.class)
    public ProblemDetail manejarSpam(HttpServletRequest peticion) {
        return construir(HttpStatus.TOO_MANY_REQUESTS, "demasiadas-peticiones", peticion);
    }

    // ---------------------------------------------------------------
    //  Bloque 9: agenda cultural
    // ---------------------------------------------------------------

    @ExceptionHandler(EventoService.RangoDeViajeInvalidoException.class)
    public ProblemDetail manejarRangoDeViaje(EventoService.RangoDeViajeInvalidoException error,
                                             HttpServletRequest peticion) {
        ProblemDetail problema = construir(HttpStatus.UNPROCESSABLE_CONTENT, "rango-viaje-invalido", peticion);
        problema.setProperty("motivo", error.getMessage());
        return problema;
    }

    @ExceptionHandler(AdminEventoService.EventoNoRecurrenteException.class)
    public ProblemDetail manejarEventoNoRecurrente(HttpServletRequest peticion) {
        return construir(HttpStatus.UNPROCESSABLE_CONTENT, "evento-no-recurrente", peticion);
    }

    @ExceptionHandler(AdminEventoService.ClonDuplicadoException.class)
    public ProblemDetail manejarClonDuplicado(HttpServletRequest peticion) {
        return construir(HttpStatus.CONFLICT, "clon-duplicado", peticion);
    }

    @ExceptionHandler(AdminEventoService.FechaDeClonIncoherenteException.class)
    public ProblemDetail manejarFechaDeClon(HttpServletRequest peticion) {
        return construir(HttpStatus.BAD_REQUEST, "clon-fecha-incoherente", peticion);
    }

    /**
     * Archivo mayor que el limite de multipart (RNF-15).
     *
     * <p>Se <strong>sobrescribe</strong> el metodo heredado en vez de declarar
     * un {@code @ExceptionHandler} propio: {@code ResponseEntityExceptionHandler}
     * ya mapea esta excepcion, y anadir otro handler para el mismo tipo hace
     * que Spring no arranque con «Ambiguous @ExceptionHandler method mapped».</p>
     *
     * <p>Tomcat corta el envio antes de que llegue a la aplicacion, asi que
     * este es el unico punto donde se puede convertir en un ProblemDetail
     * coherente con el resto del API.</p>
     */
    @Override
    protected ResponseEntity<Object> handleMaxUploadSizeExceededException(
            org.springframework.web.multipart.MaxUploadSizeExceededException ex,
            @Nullable HttpHeaders cabeceras,
            HttpStatusCode estado,
            WebRequest peticion) {

        Locale idioma = LocaleContextHolder.getLocale();

        // CONTENT_TOO_LARGE y no PAYLOAD_TOO_LARGE: el mismo 413, renombrado en
        // Spring 7 para seguir la RFC 9110.
        ProblemDetail problema = ProblemDetail.forStatus(HttpStatus.CONTENT_TOO_LARGE);
        problema.setType(URI.create(BASE_TIPOS + "imagen-invalida"));
        problema.setTitle(texto("error.imagen-invalida.titulo", idioma));
        problema.setDetail(texto("error.imagen-invalida.detalle", idioma));
        problema.setProperty("errorCode", "imagen-invalida");
        problema.setProperty("motivo", "archivo-demasiado-grande");
        problema.setProperty("timestamp", clock.instant());

        return ResponseEntity.status(HttpStatus.CONTENT_TOO_LARGE).body(problema);
    }

    /** Errores de Bean Validation: 400 con el detalle campo a campo. */
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex,
            @Nullable HttpHeaders cabeceras,
            HttpStatusCode estado,
            WebRequest peticion) {

        Locale idioma = LocaleContextHolder.getLocale();

        Map<String, String> errores = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(error -> errores.putIfAbsent(error.getField(), error.getDefaultMessage()));
        // Los validadores a nivel de clase no tienen campo asociado.
        ex.getBindingResult().getGlobalErrors()
                .forEach(error -> errores.putIfAbsent(error.getObjectName(), error.getDefaultMessage()));

        ProblemDetail problema = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        problema.setType(URI.create(BASE_TIPOS + "validacion"));
        problema.setTitle(texto("error.validacion.titulo", idioma));
        problema.setDetail(texto("error.validacion.detalle", idioma));
        problema.setProperty("errorCode", "validacion");
        problema.setProperty("errores", errores);
        problema.setProperty("timestamp", clock.instant());

        return ResponseEntity.badRequest().body(problema);
    }

    private ProblemDetail construir(HttpStatus estado, String codigo, HttpServletRequest peticion) {
        Locale idioma = LocaleContextHolder.getLocale();

        ProblemDetail problema = ProblemDetail.forStatus(estado);
        problema.setType(URI.create(BASE_TIPOS + codigo));
        problema.setTitle(texto("error." + codigo + ".titulo", idioma));
        problema.setDetail(texto("error." + codigo + ".detalle", idioma));
        problema.setInstance(URI.create(peticion.getRequestURI()));
        problema.setProperty("errorCode", codigo);
        problema.setProperty("timestamp", clock.instant());
        return problema;
    }

    private String texto(String clave, Locale idioma) {
        return mensajes.getMessage(clave, null, clave, idioma);
    }
}
