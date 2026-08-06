package com.huamanga.tourism.foto.service;

import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
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

    /**
     * Devuelve la imagen re-codificada, sin ningun metadato.
     *
     * <p><strong>Para que sirve.</strong> Una foto tomada con un movil lleva
     * dentro un bloque EXIF con la <em>posicion GPS exacta</em>, el modelo del
     * telefono, su numero de serie y, en algunos fabricantes, el nombre del
     * propietario. En una denuncia anonima (RF-72) eso es una fuga mucho mas
     * grave que la IP, y ademas invisible: la foto se ve igual.</p>
     *
     * <p>Decodificar y volver a escribir descarta esos bloques, porque los
     * escritores de {@code ImageIO} solo emiten los datos de pixel salvo que se
     * les pida explicitamente copiar los metadatos. Es una garantia nuestra, no
     * una confianza en lo que haga el proveedor de imagenes.</p>
     *
     * <p>El precio es una recompresion —se pierde algo de calidad— y algo de
     * CPU. En una foto de denuncia, el anonimato pesa mas que la nitidez.</p>
     *
     * <p>Se escribe siempre en PNG cuando el original no era JPEG, para no
     * recomprimir con perdida algo que no lo era.</p>
     */
    public byte[] sinMetadatos(byte[] contenido, Formato formato) {
        try {
            BufferedImage imagen = ImageIO.read(new ByteArrayInputStream(contenido));
            if (imagen == null) {
                throw new ImagenInvalidaException("imagen-corrupta");
            }

            // JPEG no admite canal alfa: si la imagen lo trae, se compone sobre
            // blanco. Sin esto, ImageIO escribe un JPEG con los colores
            // invertidos o falla directamente.
            BufferedImage salida = imagen;
            String tipoSalida = formato == Formato.JPEG ? "jpg" : "png";

            if (formato == Formato.JPEG && imagen.getColorModel().hasAlpha()) {
                salida = new BufferedImage(
                        imagen.getWidth(), imagen.getHeight(), BufferedImage.TYPE_INT_RGB);
                var g = salida.createGraphics();
                g.drawImage(imagen, 0, 0, java.awt.Color.WHITE, null);
                g.dispose();
            }

            ByteArrayOutputStream destino = new ByteArrayOutputStream();
            if (!ImageIO.write(salida, tipoSalida, destino)) {
                throw new ImagenInvalidaException("imagen-corrupta");
            }
            return destino.toByteArray();

        } catch (ImagenInvalidaException e) {
            throw e;
        } catch (IOException | RuntimeException e) {
            throw new ImagenInvalidaException("imagen-corrupta");
        }
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
