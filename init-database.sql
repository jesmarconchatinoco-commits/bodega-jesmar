-- ========================================
-- Script de inicialización de la BD
-- Bodega Jesmar - Sistema de Gestión
-- ========================================

-- 1. Crear base de datos
CREATE DATABASE IF NOT EXISTS sistema_ventas 
CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE sistema_ventas;

-- 2. Usuario de la aplicación (MySQL guarda la clave con caching_sha2_password)
-- No usar root. HeidiSQL entra por 127.0.0.1; Spring usa localhost.
-- No deje aquí la contraseña real: use la misma de DB_PASSWORD en application-local.properties.
CREATE USER IF NOT EXISTS 'bodega_app'@'localhost' IDENTIFIED BY 'definir-localmente';
CREATE USER IF NOT EXISTS 'bodega_app'@'127.0.0.1' IDENTIFIED BY 'definir-localmente';
GRANT ALL PRIVILEGES ON sistema_ventas.* TO 'bodega_app'@'localhost';
GRANT ALL PRIVILEGES ON sistema_ventas.* TO 'bodega_app'@'127.0.0.1';
GRANT ALL PRIVILEGES ON bodega_productos_node.* TO 'bodega_app'@'localhost';
GRANT ALL PRIVILEGES ON bodega_productos_node.* TO 'bodega_app'@'127.0.0.1';
GRANT CREATE ON *.* TO 'bodega_app'@'localhost';
GRANT CREATE ON *.* TO 'bodega_app'@'127.0.0.1';
FLUSH PRIVILEGES;

-- 3. Tablas (se crean automáticamente por JPA/Hibernate)
-- Pero aquí dejamos la estructura de referencia

-- Tabla: modulo
CREATE TABLE IF NOT EXISTS modulo (
    id INT AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL UNIQUE,
    descripcion VARCHAR(255),
    estado VARCHAR(20) DEFAULT 'ACTIVO'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Tabla: perfil
CREATE TABLE IF NOT EXISTS perfil (
    id INT AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL UNIQUE,
    descripcion VARCHAR(255),
    estado VARCHAR(20) DEFAULT 'ACTIVO'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Tabla: perfil_modulo (relación M:M — ver Perfil.java @JoinTable name = PERFIL_MODULO)
CREATE TABLE IF NOT EXISTS perfil_modulo (
    perfil_id INT NOT NULL,
    modulo_id INT NOT NULL,
    PRIMARY KEY (perfil_id, modulo_id),
    FOREIGN KEY (perfil_id) REFERENCES perfil(id),
    FOREIGN KEY (modulo_id) REFERENCES modulo(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Tabla: imagen
CREATE TABLE IF NOT EXISTS imagen (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR(255),
    ruta VARCHAR(500) NOT NULL,
    tipo VARCHAR(50),
    tamano BIGINT,
    fecha_subida DATETIME,
    estado VARCHAR(20) DEFAULT 'ACTIVO'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Tabla: usuario
CREATE TABLE IF NOT EXISTS usuario (
    id INT AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR(150) NOT NULL,
    usuario VARCHAR(100) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    correo VARCHAR(100),
    telefono VARCHAR(20),
    imagen_id BIGINT,
    perfil_id INT,
    estado VARCHAR(20) DEFAULT 'ACTIVO',
    FOREIGN KEY (imagen_id) REFERENCES imagen(id),
    FOREIGN KEY (perfil_id) REFERENCES perfil(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Tabla: categoria
CREATE TABLE IF NOT EXISTS categoria (
    id INT AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL,
    descripcion TEXT,
    imagen_id BIGINT,
    estado VARCHAR(20) DEFAULT 'ACTIVO',
    FOREIGN KEY (imagen_id) REFERENCES imagen(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Tabla: producto
CREATE TABLE IF NOT EXISTS producto (
    id INT AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR(200) NOT NULL,
    precio DECIMAL(10, 2),
    precio_compra DECIMAL(10, 2) DEFAULT 0.00,
    stock INT DEFAULT 0,
    stock_minimo INT DEFAULT 5,
    imagen_id BIGINT,
    descripcion VARCHAR(500),
    id_categoria INT NOT NULL,
    estado VARCHAR(20) DEFAULT 'ACTIVO',
    FOREIGN KEY (imagen_id) REFERENCES imagen(id),
    FOREIGN KEY (id_categoria) REFERENCES categoria(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Tabla: producto_presentacion (precios/stock por presentación)
CREATE TABLE IF NOT EXISTS producto_presentacion (
    id INT AUTO_INCREMENT PRIMARY KEY,
    producto_id INT NOT NULL,
    nombre VARCHAR(100) NOT NULL,
    precio DECIMAL(10, 2) NOT NULL,
    precio_compra DECIMAL(10, 2) DEFAULT 0.00,
    stock INT NOT NULL DEFAULT 0,
    stock_minimo INT NOT NULL DEFAULT 0,
    codigo_barras VARCHAR(50) UNIQUE,
    imagen_id BIGINT,
    estado VARCHAR(20) NOT NULL DEFAULT 'ACTIVO',
    orden INT NOT NULL DEFAULT 0,
    FOREIGN KEY (producto_id) REFERENCES producto(id),
    FOREIGN KEY (imagen_id) REFERENCES imagen(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Tabla: presentacion_imagen (galería por presentación)
CREATE TABLE IF NOT EXISTS presentacion_imagen (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    presentacion_id INT NOT NULL,
    imagen_id BIGINT NOT NULL,
    orden INT NOT NULL DEFAULT 0,
    FOREIGN KEY (presentacion_id) REFERENCES producto_presentacion(id),
    FOREIGN KEY (imagen_id) REFERENCES imagen(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Tabla: cliente
CREATE TABLE IF NOT EXISTS cliente (
    id INT AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR(200) NOT NULL,
    documento VARCHAR(20) NOT NULL UNIQUE,
    telefono VARCHAR(20),
    correo VARCHAR(100),
    imagen_id BIGINT,
    estado VARCHAR(20) DEFAULT 'ACTIVO',
    FOREIGN KEY (imagen_id) REFERENCES imagen(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Tabla: tipo_pago
CREATE TABLE IF NOT EXISTS tipo_pago (
    id INT AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Tabla: tipo_comprobante
CREATE TABLE IF NOT EXISTS tipo_comprobante (
    id INT AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL,
    serie VARCHAR(10),
    correlativo_actual INT DEFAULT 1,
    estado VARCHAR(20) DEFAULT 'ACTIVO'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Tablas venta / detalle_venta: las crea Hibernate (nombres id_cliente, id_venta, etc.).
-- Ver entidades Ventas.java y DetalleVenta.java. No usar la tabla legacy "ventas".

-- Tabla: imagen_slider
CREATE TABLE IF NOT EXISTS imagen_slider (
    id INT AUTO_INCREMENT PRIMARY KEY,
    titulo VARCHAR(200),
    descripcion TEXT,
    imagen_id BIGINT,
    estado VARCHAR(20) DEFAULT 'ACTIVO',
    FOREIGN KEY (imagen_id) REFERENCES imagen(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ========================================
-- Insertar datos iniciales
-- ========================================

-- Módulos iniciales
INSERT IGNORE INTO modulo (nombre, descripcion, estado) VALUES 
('USUARIOS', 'Gestión de usuarios y perfiles', 'ACTIVO'),
('CATALOGO', 'Gestión de catálogo de productos', 'ACTIVO'),
('VENTAS', 'Gestión de ventas y clientes', 'ACTIVO');

-- Perfil administrador
INSERT IGNORE INTO perfil (nombre, descripcion, estado) VALUES 
('ADMIN', 'Perfil administrador con permisos completos', 'ACTIVO');

-- Asignar módulos al perfil ADMIN
INSERT IGNORE INTO perfil_modulos (perfil_id, modulos_id) 
SELECT p.id, m.id FROM perfil p, modulo m 
WHERE p.nombre = 'ADMIN';

-- Tipos de pago
INSERT IGNORE INTO tipo_pago (nombre) VALUES 
('Efectivo'),
('Tarjeta de Crédito'),
('Tarjeta de Débito'),
('Transferencia Bancaria'),
('Cheque');

-- Tipos de comprobante
INSERT IGNORE INTO tipo_comprobante (nombre, serie, correlativo_actual, estado) VALUES 
('Factura', 'F', 1, 'ACTIVO'),
('Boleta', 'B', 1, 'ACTIVO'),
('Nota de Crédito', 'NC', 1, 'ACTIVO'),
('Nota de Débito', 'ND', 1, 'ACTIVO');

-- Categorías iniciales
INSERT IGNORE INTO categoria (nombre, descripcion, estado) VALUES 
('Granos y Cereales', 'Arroz, azúcar, harinas y granos varios', 'ACTIVO'),
('Abarrotes', 'Productos de abarrote', 'ACTIVO'),
('Bebidas', 'Bebidas frías y calientes', 'ACTIVO'),
('Lácteos', 'Productos lácteos', 'ACTIVO');

-- ========================================
-- Crear índices para mejor rendimiento
-- ========================================

CREATE INDEX idx_usuario_usuario ON usuario(usuario);
CREATE INDEX idx_usuario_correo ON usuario(correo);
CREATE INDEX idx_cliente_documento ON cliente(documento);
CREATE INDEX idx_producto_nombre ON producto(nombre);
CREATE INDEX idx_categoria_nombre ON categoria(nombre);
CREATE INDEX idx_ventas_cliente ON ventas(cliente_id);
CREATE INDEX idx_ventas_estado ON ventas(estado);
CREATE INDEX idx_detalle_venta_venta ON detalle_venta(venta_id);

-- ========================================
-- Mostrar estado
-- ========================================
SELECT '✅ Base de datos configurada correctamente' as Estado;
SELECT COUNT(*) as 'Módulos' FROM modulo;
SELECT COUNT(*) as 'Perfiles' FROM perfil;
SELECT COUNT(*) as 'Tipos de Pago' FROM tipo_pago;
SELECT COUNT(*) as 'Tipos de Comprobante' FROM tipo_comprobante;
