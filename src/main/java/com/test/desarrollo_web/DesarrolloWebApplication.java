package com.test.desarrollo_web;

import com.test.desarrollo_web.config.PreparadorBaseDatos;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@ConfigurationPropertiesScan
@SpringBootApplication
public class DesarrolloWebApplication {

	public static void main(String[] args) {
		System.out.println("ARRANQUE_DB_URL=" + clasificar(System.getenv("DB_URL")));
		System.out.println("ARRANQUE_DATABASE_URL=" + clasificar(System.getenv("DATABASE_URL")));
		SpringApplication aplicacion = new SpringApplication(DesarrolloWebApplication.class);
		aplicacion.addInitializers(new PreparadorBaseDatos());
		aplicacion.run(args);
	}

	private static String clasificar(String valor) {
		if (valor == null || valor.isBlank()) {
			return "vacia";
		}
		String texto = valor.toLowerCase();
		if (texto.contains("postgres")) {
			return "postgres";
		}
		if (texto.contains("mysql")) {
			return "mysql";
		}
		return "otro";
	}

}
