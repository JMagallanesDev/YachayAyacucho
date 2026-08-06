package com.huamanga.tourism.foto;

import com.huamanga.tourism.foto.service.ValidadorImagen;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Validacion de imagenes subidas (RNF-15).
 *
 * <p>Es la parte del bloque donde un descuido se paga caro: aceptar contenido
 * arbitrario de desconocidos. Por eso cada test representa un intento concreto
 * de colar algo que no es una imagen.</p>
 */
@DisplayName("Validacion de imagen")
class ValidadorImagenTest {

    private final ValidadorImagen validador = new ValidadorImagen();

    // ---------------------------------------------------------------
    //  Lo que debe pasar
    // ---------------------------------------------------------------

    @Test
    @DisplayName("acepta un PNG de verdad")
    void aceptaPng() throws IOException {
        var archivo = new MockMultipartFile(
                "archivo", "foto.png", "image/png", imagenReal("png"));

        assertThat(validador.validar(archivo)).isEqualTo(ValidadorImagen.Formato.PNG);
    }

    @Test
    @DisplayName("acepta un JPEG de verdad")
    void aceptaJpeg() throws IOException {
        var archivo = new MockMultipartFile(
                "archivo", "foto.jpg", "image/jpeg", imagenReal("jpg"));

        assertThat(validador.validar(archivo)).isEqualTo(ValidadorImagen.Formato.JPEG);
    }

    @Test
    @DisplayName("el formato lo decide el contenido, no la extension ni la cabecera")
    void ignoraExtensionYCabecera() throws IOException {
        // Un PNG llamado ".txt" y declarado como texto plano: sigue siendo PNG.
        var archivo = new MockMultipartFile(
                "archivo", "cualquier-cosa.txt", "text/plain", imagenReal("png"));

        assertThat(validador.validar(archivo)).isEqualTo(ValidadorImagen.Formato.PNG);
    }

    // ---------------------------------------------------------------
    //  Lo que NO debe pasar
    // ---------------------------------------------------------------

    @Test
    @DisplayName("rechaza un script disfrazado de imagen")
    void rechazaScriptRenombrado() {
        // El caso clasico: contenido ejecutable con nombre y cabecera de imagen.
        // Se le da un tamaño realista —un webshell no ocupa treinta bytes— para
        // que lo que lo detenga sea la firma del contenido y no el minimo.
        byte[] php = ("<?php system($_GET['c']); "
                + "// relleno para alcanzar un tamaño verosimil ".repeat(4)
                + "?>").getBytes(StandardCharsets.UTF_8);
        var archivo = new MockMultipartFile("archivo", "inocente.jpg", "image/jpeg", php);

        assertThatThrownBy(() -> validador.validar(archivo))
                .isInstanceOf(ValidadorImagen.ImagenInvalidaException.class)
                .extracting(e -> ((ValidadorImagen.ImagenInvalidaException) e).codigo())
                .isEqualTo("formato-no-permitido");
    }

    @Test
    @DisplayName("rechaza una cabecera JPEG valida seguida de basura")
    void rechazaCabeceraFalsificada() {
        // Esto es lo que la comprobacion de numeros magicos NO puede ver por si
        // sola: los primeros bytes son un JPEG legitimo y el resto no lo es.
        // Solo intentar decodificarlo lo descubre.
        byte[] falso = new byte[600];
        falso[0] = (byte) 0xFF;
        falso[1] = (byte) 0xD8;
        falso[2] = (byte) 0xFF;
        Arrays.fill(falso, 3, falso.length, (byte) 0x41);

        var archivo = new MockMultipartFile("archivo", "falso.jpg", "image/jpeg", falso);

        assertThatThrownBy(() -> validador.validar(archivo))
                .isInstanceOf(ValidadorImagen.ImagenInvalidaException.class)
                .extracting(e -> ((ValidadorImagen.ImagenInvalidaException) e).codigo())
                .isEqualTo("imagen-corrupta");
    }

    @Test
    @DisplayName("rechaza un archivo de mas de 5 MB (RNF-15)")
    void rechazaDemasiadoGrande() {
        byte[] enorme = new byte[(int) ValidadorImagen.TAMANO_MAXIMO_BYTES + 1];
        enorme[0] = (byte) 0x89;
        enorme[1] = 'P';
        enorme[2] = 'N';
        enorme[3] = 'G';

        var archivo = new MockMultipartFile("archivo", "grande.png", "image/png", enorme);

        assertThatThrownBy(() -> validador.validar(archivo))
                .isInstanceOf(ValidadorImagen.ImagenInvalidaException.class)
                .extracting(e -> ((ValidadorImagen.ImagenInvalidaException) e).codigo())
                .isEqualTo("archivo-demasiado-grande");
    }

    @Test
    @DisplayName("rechaza un archivo vacio")
    void rechazaVacio() {
        var archivo = new MockMultipartFile("archivo", "vacio.png", "image/png", new byte[0]);

        assertThatThrownBy(() -> validador.validar(archivo))
                .isInstanceOf(ValidadorImagen.ImagenInvalidaException.class);
    }

    @Test
    @DisplayName("rechaza un SVG, que es texto y puede llevar JavaScript dentro")
    void rechazaSvg() {
        // Un SVG es una imagen para el usuario y un documento con scripts para
        // el navegador. Queda fuera de los formatos permitidos a proposito.
        byte[] svg = ("<svg xmlns='http://www.w3.org/2000/svg' width='400' height='300'>"
                + "<rect width='400' height='300' fill='#B3202B'/>"
                + "<script>alert(document.cookie)</script>"
                + "<text x='10' y='20'>parece una imagen</text></svg>")
                .getBytes(StandardCharsets.UTF_8);

        var archivo = new MockMultipartFile("archivo", "vector.svg", "image/svg+xml", svg);

        assertThatThrownBy(() -> validador.validar(archivo))
                .isInstanceOf(ValidadorImagen.ImagenInvalidaException.class)
                .extracting(e -> ((ValidadorImagen.ImagenInvalidaException) e).codigo())
                .isEqualTo("formato-no-permitido");
    }

    @Test
    @DisplayName("rechaza un GIF: no esta en la lista de formatos permitidos")
    void rechazaGif() {
        byte[] gif = new byte[200];
        byte[] firma = "GIF89a".getBytes(StandardCharsets.US_ASCII);
        System.arraycopy(firma, 0, gif, 0, firma.length);

        var archivo = new MockMultipartFile("archivo", "animado.gif", "image/gif", gif);

        assertThatThrownBy(() -> validador.validar(archivo))
                .isInstanceOf(ValidadorImagen.ImagenInvalidaException.class);
    }

    // ---------------------------------------------------------------
    //  Ayudantes
    // ---------------------------------------------------------------

    /** Genera una imagen valida de verdad, no un montaje de bytes. */
    private byte[] imagenReal(String formato) throws IOException {
        BufferedImage imagen = new BufferedImage(120, 90, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = imagen.createGraphics();
        g.setColor(new Color(179, 32, 43));
        g.fillRect(0, 0, 120, 90);
        g.dispose();

        ByteArrayOutputStream salida = new ByteArrayOutputStream();
        ImageIO.write(imagen, formato, salida);
        return salida.toByteArray();
    }
}
