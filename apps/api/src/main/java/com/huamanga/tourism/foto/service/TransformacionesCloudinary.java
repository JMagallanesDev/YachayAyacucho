package com.huamanga.tourism.foto.service;

import org.springframework.stereotype.Component;

/**
 * Compone URLs de entrega con transformaciones (RNF-03).
 *
 * <p>Las transformaciones se aplican <strong>en la URL de entrega</strong>, no
 * al subir. La diferencia importa: al subir se guardaria una copia por cada
 * tamaño y cada una consumiria almacenamiento del plan; en la URL, Cloudinary
 * genera la variante la primera vez que se pide y la cachea en su CDN. Cambiar
 * de tamaño manana es editar una cadena, no volver a procesar el archivo.</p>
 *
 * <p>{@code f_auto} entrega WebP o AVIF a quien los soporte y JPEG al resto;
 * {@code q_auto} ajusta la compresion analizando la propia imagen. Juntos
 * dejan las fotos del patrimonio muy por debajo de los 200 KB que pide el
 * RNF-03 sin degradar visiblemente la piedra ni los retablos.</p>
 */
@Component
public class TransformacionesCloudinary {

    /** Ancho maximo en la ficha. Suficiente para pantallas de alta densidad. */
    private static final String ENTREGA = "f_auto,q_auto,w_1600,c_limit";

    /** Rejilla de la galeria: cuadrada y recortada al centro del motivo. */
    private static final String MINIATURA = "f_auto,q_auto,w_600,h_600,c_fill,g_auto";

    private static final String MARCADOR = "/upload/";

    /**
     * Inserta la transformacion en una URL de Cloudinary.
     *
     * <p>Si la URL no tiene el tramo {@code /upload/} se devuelve intacta: en
     * los tests las URLs son ficticias, y una foto sin optimizar es mejor que
     * una URL rota.</p>
     */
    public String paraEntrega(String url) {
        return conTransformacion(url, ENTREGA);
    }

    public String paraMiniatura(String url) {
        return conTransformacion(url, MINIATURA);
    }

    private String conTransformacion(String url, String transformacion) {
        if (url == null || !url.contains(MARCADOR)) {
            return url;
        }
        return url.replace(MARCADOR, MARCADOR + transformacion + "/");
    }
}
