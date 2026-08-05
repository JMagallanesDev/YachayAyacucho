package com.huamanga.tourism;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.TimeZone;

@SpringBootApplication
public class TourismApplication {

	/**
	 * La JVM corre en UTC, siempre y en todas partes.
	 *
	 * <p>{@code hibernate.jdbc.time_zone: UTC} (application.yml) hace que
	 * Hibernate convierta entre UTC y la zona por defecto de la JVM al leer y
	 * escribir. Eso es lo correcto para los instantes, pero Hibernate lo aplica
	 * tambien a las columnas {@code TIME}, que aqui son <strong>hora de
	 * pared</strong>: "el templo abre a las 09:00" no es un instante y no debe
	 * convertirse a nada.</p>
	 *
	 * <p>Con la JVM en Lima (-05:00) el efecto era que un horario de 06:00 en la
	 * base se leia como 01:00, y uno guardado como 09:00 acababa almacenado como
	 * 14:00. Escritura y lectura se compensaban entre si, asi que la aplicacion
	 * parecia coherente consigo misma y los tests pasaban; la discrepancia solo
	 * aparecia frente a los datos cargados por SQL. Peor aun, el resultado
	 * dependia de la zona de la maquina: el mismo codigo daba horarios distintos
	 * en un portatil de Ayacucho y en un servidor europeo.</p>
	 *
	 * <p>Fijando la JVM en UTC la conversion es la identidad y las horas de
	 * pared viajan intactas. Nada de la aplicacion depende de la zona por
	 * defecto: el {@code Clock} es {@code systemUTC()} y todo calculo sobre la
	 * hora de Ayacucho nombra su zona explicitamente.</p>
	 *
	 * <p>Va en un bloque estatico y no en {@code main} a proposito: los tests no
	 * pasan por {@code main}, pero si cargan esta clase, de modo que asi
	 * ejecutan bajo la misma zona que produccion.</p>
	 */
	static {
		TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
	}

	public static void main(String[] args) {
		SpringApplication.run(TourismApplication.class, args);
	}

}
