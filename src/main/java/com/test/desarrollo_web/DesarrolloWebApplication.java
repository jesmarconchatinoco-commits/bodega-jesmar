package com.test.desarrollo_web;

import com.test.desarrollo_web.config.PreparadorBaseDatos;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@ConfigurationPropertiesScan
@SpringBootApplication
public class DesarrolloWebApplication {

	public static void main(String[] args) {
		SpringApplication aplicacion = new SpringApplication(DesarrolloWebApplication.class);
		aplicacion.addInitializers(new PreparadorBaseDatos());
		aplicacion.run(args);
	}

}
