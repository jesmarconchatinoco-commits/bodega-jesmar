package com.test.desarrollo_web.config;

/**
 * Subcarpetas dentro de {@code src/main/resources/imagen/}.
 */
public enum ImageStorageCategory {

    PRESENTACIONES("presentaciones"),
    CATEGORIAS("categoria"),
    CLIENTES("cliente"),
    PERFILES("perfil"),
    SLIDERS("slider"),
    LOGO("logo"),
    PEDIDOS("pedido"),
    CONFIG_PAGOS("config-pagos");

    private final String folder;

    ImageStorageCategory(String folder) {
        this.folder = folder;
    }

    public String getFolder() {
        return folder;
    }
}
