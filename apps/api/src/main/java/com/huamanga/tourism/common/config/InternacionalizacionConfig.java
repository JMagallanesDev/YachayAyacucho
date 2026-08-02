package com.huamanga.tourism.common.config;

import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.i18n.AcceptHeaderLocaleResolver;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;

/**
 * Mensajes del API en el idioma del cliente (RF-60, RNF-23).
 *
 * <p>No basta con traducir la interfaz: si el backend responde "El correo o la
 * contrasena no son correctos" a un visitante que navega en ingles, la
 * traduccion del sitio se rompe justo donde mas se nota, que es cuando algo
 * falla.</p>
 */
@Configuration
public class InternacionalizacionConfig {

    /** Espanol por defecto: es la lengua base del contenido patrimonial. */
    public static final Locale IDIOMA_POR_DEFECTO = Locale.forLanguageTag("es");
    public static final List<Locale> IDIOMAS_SOPORTADOS =
            List.of(Locale.forLanguageTag("es"), Locale.forLanguageTag("en"));

    @Bean
    public MessageSource messageSource() {
        ReloadableResourceBundleMessageSource fuente = new ReloadableResourceBundleMessageSource();
        fuente.setBasenames("classpath:mensajes/mensajes", "classpath:mensajes/validacion");
        // UTF-8 explicito: por defecto los .properties se leen en ISO-8859-1 y
        // cualquier tilde saldria corrompida.
        fuente.setDefaultEncoding(StandardCharsets.UTF_8.name());
        fuente.setDefaultLocale(IDIOMA_POR_DEFECTO);
        // Si falta una clave se muestra la clave misma en vez de reventar:
        // un texto sin traducir es un fallo cosmetico, no una caida.
        fuente.setUseCodeAsDefaultMessage(true);
        return fuente;
    }

    /**
     * Conecta Bean Validation con el MessageSource.
     *
     * <p>Sin esto, las anotaciones {@code @NotNull(message = "{clave}")}
     * buscarian en el ValidationMessages.properties de Hibernate Validator y
     * nunca encontrarian nuestras claves.</p>
     */
    @Bean
    public LocalValidatorFactoryBean validator(MessageSource messageSource) {
        LocalValidatorFactoryBean validador = new LocalValidatorFactoryBean();
        validador.setValidationMessageSource(messageSource);
        return validador;
    }

    /**
     * El idioma sale de la cabecera Accept-Language que envia el navegador.
     *
     * <p>Frances y aleman se negocian y caen a la primera coincidencia de la
     * lista; como no estan soportados, terminan en espanol por defecto. En el
     * frontend caen a ingles, tal como fija el alcance del proyecto.</p>
     */
    @Bean
    public LocaleResolver localeResolver() {
        AcceptHeaderLocaleResolver resolutor = new AcceptHeaderLocaleResolver();
        resolutor.setSupportedLocales(IDIOMAS_SOPORTADOS);
        resolutor.setDefaultLocale(IDIOMA_POR_DEFECTO);
        return resolutor;
    }
}
