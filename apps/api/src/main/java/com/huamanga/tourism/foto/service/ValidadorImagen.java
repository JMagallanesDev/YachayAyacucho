package com.huamanga.tourism.foto.service;

import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Arrays;

/**
 * Comprueba que lo subido es realmente una imagen (RNF-15).
 *
 * <p><strong>La cabecera {@code Content-Type} no se mira.</strong> La escribe el
 * cliente: decir "image/jpeg" no cuesta nada y no prueba nada. Es una
 * declaracion de intenciones, no evidencia.</p>
 *
 * <p>Se aplican tres barreras en orden creciente de coste:</p>
 * <ol>
 *   <li><strong>Tamaño.</strong> Se rechaza antes de mirar el contenido.</li>
 *   <li><strong>Numeros magicos.</strong> Los primeros bytes identifican el
 *       formato real. Detiene el caso clasico: un script renombrado a
 *       {@code .jpg}.</li>
 *   <li><strong>Decodificacion.</strong> Se intenta construir la imagen de
 *       verdad. Detiene lo que la barrera anterior no puede ver: una cabecera
 *       JPEG valida seguida de basura, o un fichero poliglota que es imagen y
 *       otra cosa a la vez.</li>
 * </ol>
 *
 * <p>Ademas, el nombre original del archivo <strong>no se usa jamas</strong>:
 * ni para el {@code public_id}, ni para la extension, ni para nada. Asi
 * desaparecen de raiz los recorridos de ruta ({@code ../../}) y las colisiones
 * de nombres.</p>
 */
@Component
public class ValidadorImagen {

    /** RNF-15. */
    public static final long TAMANO_MAXIMO_BYTES = 5L * 1024 * 1024;

    /** Por debajo de esto no hay imagen que valga: es un archivo vacio o roto. */
    private static final int TAMANO_MINIMO_BYTES = 100;

    /** Limite defensivo: una imagen de 20 000 px agota memoria al decodificar. */
    private static final int LADO_MAXIMO_PX = 10_000;

    private static final byte[] FIRMA_JPEG = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF};
    private static final byte[] FIRMA_PNG =
            {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A};
    private static final byte[] FIRMA_RIFF = {'R', 'I', 'F', 'F'};
    private static final byte[] FIRMA_WEBP = {'W', 'E', 'B', 'P'};

    public enum Formato {
        JPEG("jpg"), PNG("png"), WEBP("webp");

        private final String extension;

        Formato(String extension) {
            this.extension = extension;
        }

        public String extension() {
            return extension;
        }
    }

    /**
     * @return el formato real detectado
     * @throws ImagenInvalidaException si no supera alguna barrera
     */
    public Formato validar(MultipartFile archivo) {
        if (archivo == null || archivo.isEmpty()) {
            throw new ImagenInvalidaException("archivo-vacio");
        }
        if (archivo.getSize() > TAMANO_MAXIMO_BYTES) {
            throw new ImagenInvalidaException("archivo-demasiado-grande");
        }
        if (archivo.getSize() < TAMANO_MINIMO_BYTES) {
            throw new ImagenInvalidaException("archivo-vacio");
        }

        byte[] contenido;
        try {
            contenido = archivo.getBytes();
        } catch (IOException e) {
            throw new ImagenInvalidaException("archivo-ilegible");
        }

        Formato formato = detectarPorFirma(contenido);
        if (formato == null) {
            throw new ImagenInvalidaException("formato-no-permitido");
        }

        // Segunda barrera: que se pueda decodificar de verdad.
        BufferedImage imagen;
        try {
            imagen = ImageIO.read(new ByteArrayInputStream(contenido));
        } catch (IOException | RuntimeException e) {
            // ImageIO lanza excepciones no declaradas ante datos corruptos.
            throw new ImagenInvalidaException("imagen-corrupta");
        }

        if (imagen == null) {
            // Firma correcta pero contenido que no es una imagen decodificable:
            // exactamente el caso de la cabecera falsificada.
            throw new ImagenInvalidaException("imagen-corrupta");
        }
        if (imagen.getWidth() > LADO_MAXIMO_PX || imagen.getHeight() > LADO_MAXIMO_PX) {
            throw new ImagenInvalidaException("imagen-demasiado-grande");
        }

        return formato;
    }

    private Formato detectarPorFirma(byte[] contenido) {
        if (empiezaPor(contenido, FIRMA_JPEG)) {
            return Formato.JPEG;
        }
        if (empiezaPor(contenido, FIRMA_PNG)) {
            return Formato.PNG;
        }
        // WebP es un contenedor RIFF: "RIFF" + 4 bytes de tamaño + "WEBP".
        if (empiezaPor(contenido, FIRMA_RIFF)
                && contenido.length > 12
                && Arrays.equals(Arrays.copyOfRange(contenido, 8, 12), FIRMA_WEBP)) {
            return Formato.WEBP;
        }
        return null;
    }

    private boolean empiezaPor(byte[] contenido, byte[] firma) {
        if (contenido.length < firma.length) {
            return false;
        }
        return Arrays.equals(Arrays.copyOfRange(contenido, 0, firma.length), firma);
    }

    /**
     * @param codigo clave estable para traducir el mensaje en el frontend; no
     *               se expone el motivo tecnico exacto para no dar pistas a
     *               quien este probando que cuela
     */
    public static class ImagenInvalidaException extends RuntimeException {

        private final String codigo;

        public ImagenInvalidaException(String codigo) {
            super("Imagen invalida: " + codigo);
            this.codigo = codigo;
        }

        public String codigo() {
            return codigo;
        }
    }
}
