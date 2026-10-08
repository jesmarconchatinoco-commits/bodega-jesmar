package com.test.desarrollo_web.config;



import org.springframework.beans.factory.annotation.Value;

import org.springframework.context.annotation.Configuration;

import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;

import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;



import java.nio.file.Path;



@Configuration

public class WebResourceConfig implements WebMvcConfigurer {



    private final StoragePathResolver storagePathResolver;



    @Value("${app.upload.dir:src/main/resources/imagen}")

    private String uploadDir;



    public WebResourceConfig(StoragePathResolver storagePathResolver) {

        this.storagePathResolver = storagePathResolver;

    }



    @Override

    public void addResourceHandlers(ResourceHandlerRegistry registry) {

        Path uploadPath = storagePathResolver.resolve(uploadDir);

        String location = uploadPath.toUri().toString();

        if (!location.endsWith("/")) {

            location += "/";

        }

        registry.addResourceHandler("/imagen/**")

                .addResourceLocations(location);

    }

}


