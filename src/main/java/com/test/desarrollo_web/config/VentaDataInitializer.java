package com.test.desarrollo_web.config;

import com.test.desarrollo_web.Models.TipoComprobante;
import com.test.desarrollo_web.Models.TipoPago;
import com.test.desarrollo_web.Repository.TipoComprobanteRepository;
import com.test.desarrollo_web.Repository.TipoPagoRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

@Configuration
public class VentaDataInitializer {

    @Bean
    @Order(50)
    public CommandLineRunner inicializarDatosVentas(TipoPagoRepository tipoPagoRepository,
                                                    TipoComprobanteRepository tipoComprobanteRepository) {
        return args -> {
            crearTipoPagoSiNoExiste(tipoPagoRepository, "Contado");
            crearTipoPagoSiNoExiste(tipoPagoRepository, "Yape");
            crearTipoPagoSiNoExiste(tipoPagoRepository, "Plin");
            crearTipoPagoSiNoExiste(tipoPagoRepository, "Pago Mixto");

            crearComprobanteSiNoExiste(tipoComprobanteRepository, "NOTA DE VENTA", "N001", 0);
            crearComprobanteSiNoExiste(tipoComprobanteRepository, "BOLETA", "B001", 0);
            crearComprobanteSiNoExiste(tipoComprobanteRepository, "FACTURA", "F001", 0);
        };
    }

    private void crearTipoPagoSiNoExiste(TipoPagoRepository repo, String nombre) {
        repo.findByNombreIgnoreCase(nombre).orElseGet(() -> {
            TipoPago tipo = new TipoPago();
            tipo.setNombre(nombre);
            return repo.save(tipo);
        });
    }

    private void crearComprobanteSiNoExiste(TipoComprobanteRepository repo,
                                            String nombre, String serie, int correlativo) {
        repo.findAll().stream()
                .filter(c -> nombre.equalsIgnoreCase(c.getNombre()))
                .findFirst()
                .orElseGet(() -> {
                    TipoComprobante comprobante = new TipoComprobante();
                    comprobante.setNombre(nombre);
                    comprobante.setSerie(serie);
                    comprobante.setCorrelativoActual(correlativo);
                    comprobante.setEstado(TipoComprobante.Estado.ACTIVO);
                    return repo.save(comprobante);
                });
    }
}
