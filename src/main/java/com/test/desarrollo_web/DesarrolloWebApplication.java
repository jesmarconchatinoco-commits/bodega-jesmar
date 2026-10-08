package com.test.desarrollo_web;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@ConfigurationPropertiesScan
@SpringBootApplication
public class DesarrolloWebApplication {

	public static void main(String[] args) {
		String url = System.getenv("DB_URL");
		if (url == null || url.isBlank()) {
			System.out.println("DB_URL_USADA=no definida, usa localhost");
		} else {
			System.out.println("DB_URL_USADA=" + url);
		}
		SpringApplication.run(DesarrolloWebApplication.class, args);
	}

}
