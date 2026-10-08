-- =============================================================================
-- Bodega Jesmar — limpieza de esquema (sistema_ventas)
-- Ejecutar sobre una COPIA DE RESPALDO primero:  mysql -u root sistema_ventas < scripts/db-cleanup.sql
-- =============================================================================
-- Auditoría (2026-09-30):
--   • 27 tablas activas; no existe la tabla legacy "ventas" del init-database antiguo.
--   • Todas las columnas actuales están mapeadas en entidades JPA (@Column / @JoinColumn).
--   • Hibernate ddl-auto=update NO elimina columnas al quitarlas del Java; hay que usar ALTER manual.
--   • Crédito legacy: 2 ventas con cuotas, 4 filas en venta_cuota (API /ventas/api/{id}/cuotas aún las usa).
-- =============================================================================

USE sistema_ventas;

-- -----------------------------------------------------------------------------
-- 1) Tablas legacy del script init-database.sql (solo si existieran vacías)
-- -----------------------------------------------------------------------------
-- DROP TABLE IF EXISTS detalle_venta_legacy;
-- DROP TABLE IF EXISTS ventas;
-- DROP TABLE IF EXISTS perfil_modulos;

-- -----------------------------------------------------------------------------
-- 2) OPCIONAL — retirar crédito/cuotas (solo si ya no necesitas historial ni cobro de cuotas)
--    Descomenta tras respaldar. Ajusta IDs si quieres conservar algunas ventas.
-- -----------------------------------------------------------------------------
/*
DELETE FROM venta_cuota;
UPDATE venta SET deuda = 0, pago_inicial = NULL, numero_cuotas = NULL, intervalo_dias = NULL
  WHERE numero_cuotas IS NOT NULL OR intervalo_dias IS NOT NULL OR deuda > 0;

ALTER TABLE venta
  DROP COLUMN pago_inicial,
  DROP COLUMN numero_cuotas,
  DROP COLUMN intervalo_dias,
  DROP COLUMN deuda;

DROP TABLE IF EXISTS venta_cuota;
-- Después: quitar campos de Ventas.java, VentaCuota.java y endpoints de cuotas en el código.
*/

-- -----------------------------------------------------------------------------
-- 3) Verificación rápida post-cambios
-- -----------------------------------------------------------------------------
SELECT 'venta' AS tabla, COUNT(*) AS filas FROM venta
UNION ALL SELECT 'venta_cuota', COUNT(*) FROM venta_cuota
UNION ALL SELECT 'detalle_venta', COUNT(*) FROM detalle_venta;
