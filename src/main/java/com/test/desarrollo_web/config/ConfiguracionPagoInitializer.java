package com.test.desarrollo_web.config;

import com.test.desarrollo_web.Models.ConfiguracionPago;
import com.test.desarrollo_web.Repository.ConfiguracionPagoRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

@Configuration
public class ConfiguracionPagoInitializer {

    @Bean
    @Order(55)
    public CommandLineRunner inicializarConfiguracionPagos(ConfiguracionPagoRepository repository,
                                                           CatalogoProperties catalogoProperties) {
        return args -> {
            crearSiNoExiste(repository, ConfiguracionPago.Canal.YAPE,
                    catalogoProperties.getYapeCelular(), "Titular Yape");
            crearSiNoExiste(repository, ConfiguracionPago.Canal.PLIN,
                    catalogoProperties.getPlinCelular(), "Titular Plin");
        };
    }

    private void crearSiNoExiste(ConfiguracionPagoRepository repository,
                                 ConfiguracionPago.Canal canal,
                                 String celular,
                                 String titularDefault) {
        repository.findByCanal(canal).orElseGet(() -> {
            ConfiguracionPago config = new ConfiguracionPago();
            config.setCanal(canal);
            config.setCelular(celular != null && !celular.isBlank() ? celular.trim() : "000000000");
            config.setNombreTitular(titularDefault);
            config.setEstado(ConfiguracionPago.Estado.ACTIVO);
            return repository.save(config);
        });
    }
}
