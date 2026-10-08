-- Tabla producto: debe quedar SOLO con id_categoria (sin categoria_id).
-- Ejecutar en MySQL / Laragon / HeidiSQL

USE sistema_ventas;

-- Ver estado actual de las columnas
SELECT COLUMN_NAME
FROM INFORMATION_SCHEMA.COLUMNS
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME = 'producto'
  AND COLUMN_NAME IN ('id_categoria', 'categoria_id');

-- Si categoria_id YA NO EXISTE, no ejecute nada más.
-- Su tabla ya está correcta. Solo use id_categoria.

-- Si categoria_id AÚN EXISTE, ejecute solo este bloque:
/*
UPDATE producto
SET id_categoria = categoria_id
WHERE (id_categoria IS NULL OR id_categoria = 0)
  AND categoria_id IS NOT NULL;

ALTER TABLE producto DROP FOREIGN KEY NOMBRE_DE_LA_FK;
ALTER TABLE producto DROP COLUMN categoria_id;
*/
