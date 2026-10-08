package com.test.desarrollo_web.util;

import com.test.desarrollo_web.config.ImageStorageCategory;

/**
 * Rutas relativas en {@code imagen/} y URLs públicas vía {@code /archivos/}.
 */
public final class ImagenRutas {

    public static final String PUBLIC_PREFIX = "/archivos/";

    private ImagenRutas() {
    }

    public static String normalizarRuta(String ruta) {
        if (ruta == null || ruta.isBlank()) {
            return ruta;
        }
        String limpia = ruta.trim().replace('\\', '/');
        if (limpia.startsWith(PUBLIC_PREFIX)) {
            limpia = limpia.substring(PUBLIC_PREFIX.length());
        }
        if (limpia.startsWith("imagen/")) {
            limpia = limpia.substring("imagen/".length());
        }
        if (limpia.startsWith("imagenes/")) {
            limpia = limpia.substring("imagenes/".length());
        }
        return limpia
                .replaceFirst("^productos/", ImageStorageCategory.PRESENTACIONES.getFolder() + "/")
                .replaceFirst("^producto/", ImageStorageCategory.PRESENTACIONES.getFolder() + "/")
                .replaceFirst("^categorias/", ImageStorageCategory.CATEGORIAS.getFolder() + "/")
                .replaceFirst("^clientes/", ImageStorageCategory.CLIENTES.getFolder() + "/")
                .replaceFirst("^perfiles/", ImageStorageCategory.PERFILES.getFolder() + "/")
                .replaceFirst("^sliders/", ImageStorageCategory.SLIDERS.getFolder() + "/")
                .replaceFirst("^logos/", ImageStorageCategory.LOGO.getFolder() + "/")
                .replaceFirst("^login/", ImageStorageCategory.LOGO.getFolder() + "/")
                .replaceFirst("^pedidos/", ImageStorageCategory.PEDIDOS.getFolder() + "/");
    }

    public static String toPublicUrl(String ruta) {
        String normalizada = normalizarRuta(ruta);
        if (normalizada == null || normalizada.isBlank()) {
            return null;
        }
        return PUBLIC_PREFIX + normalizada;
    }
}
