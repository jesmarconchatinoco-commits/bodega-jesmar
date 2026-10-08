package com.test.desarrollo_web;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@ConfigurationPropertiesScan
@SpringBootApplication
public class DesarrolloWebApplication {

	public static void main(String[] args) {
		SpringApplication.run(DesarrolloWebApplication.class, args);
	}

}
