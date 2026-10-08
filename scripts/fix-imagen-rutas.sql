-- ========================================
-- Migración de rutas de imagen y limpieza legacy
-- Bodega Jesmar - sistema_ventas
-- Ejecutar una vez si no usa ImagenStorageFixer al arrancar
-- ========================================

USE sistema_ventas;

-- Normalizar prefijos antiguos en imagen.ruta
UPDATE imagen SET ruta = REPLACE(ruta, 'imagenes/', '') WHERE ruta LIKE 'imagenes/%';
UPDATE imagen SET ruta = REPLACE(ruta, 'imagen/', '') WHERE ruta LIKE 'imagen/%';
UPDATE imagen SET ruta = REPLACE(ruta, '/archivos/', '') WHERE ruta LIKE '/archivos/%';
UPDATE imagen SET ruta = REPLACE(ruta, 'productos/', 'presentaciones/') WHERE ruta LIKE 'productos/%';
UPDATE imagen SET ruta = REPLACE(ruta, 'producto/', 'presentaciones/') WHERE ruta LIKE 'producto/%';
UPDATE imagen SET ruta = REPLACE(ruta, 'categorias/', 'categoria/') WHERE ruta LIKE 'categorias/%';
UPDATE imagen SET ruta = REPLACE(ruta, 'clientes/', 'cliente/') WHERE ruta LIKE 'clientes/%';
UPDATE imagen SET ruta = REPLACE(ruta, 'perfiles/', 'perfil/') WHERE ruta LIKE 'perfiles/%';
UPDATE imagen SET ruta = REPLACE(ruta, 'sliders/', 'slider/') WHERE ruta LIKE 'sliders/%';
UPDATE imagen SET ruta = REPLACE(ruta, 'logos/', 'logo/') WHERE ruta LIKE 'logos/%';
UPDATE imagen SET ruta = REPLACE(ruta, 'login/', 'logo/') WHERE ruta LIKE 'login/%';
UPDATE imagen SET ruta = REPLACE(ruta, 'pedidos/', 'pedido/') WHERE ruta LIKE 'pedidos/%';

-- Migrar producto_imagen -> presentacion_imagen (si la tabla legacy existe)
INSERT INTO presentacion_imagen (presentacion_id, imagen_id, orden)
SELECT pp.id, pi.imagen_id, pi.orden
FROM producto_imagen pi
INNER JOIN (
    SELECT producto_id, MIN(id) AS presentacion_id
    FROM producto_presentacion
    GROUP BY producto_id
) primera ON primera.producto_id = pi.producto_id
INNER JOIN producto_presentacion pp ON pp.id = primera.presentacion_id
WHERE NOT EXISTS (
    SELECT 1 FROM presentacion_imagen ppi
    WHERE ppi.presentacion_id = pp.id AND ppi.imagen_id = pi.imagen_id
);

-- Copiar imagen principal de producto a su primera presentación
UPDATE producto_presentacion pp
INNER JOIN producto p ON p.id = pp.producto_id
INNER JOIN (
    SELECT producto_id, MIN(id) AS presentacion_id
    FROM producto_presentacion
    GROUP BY producto_id
) primera ON primera.presentacion_id = pp.id
SET pp.imagen_id = p.imagen_id
WHERE p.imagen_id IS NOT NULL AND pp.imagen_id IS NULL;

UPDATE producto SET imagen_id = NULL WHERE imagen_id IS NOT NULL;

-- Eliminar tabla legacy (ya no usada por la aplicación)
DROP TABLE IF EXISTS producto_imagen;

SELECT 'Migración de imágenes completada' AS estado;
