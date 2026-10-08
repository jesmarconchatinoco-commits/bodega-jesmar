# 🏪 Bodega Jesmar - Sistema de Gestión

Sistema completo de gestión para bodegas desarrollado con **Spring Boot 3**, **MariaDB**, **Thymeleaf** y **AdminLTE**.

## ✨ Características Principales

- ✅ **Gestión de Usuarios** con roles y perfiles
- ✅ **Control de Acceso** basado en módulos y perfiles
- ✅ **Catálogo de Productos** con categorías
- ✅ **Gestión de Clientes** con validación DNI/RUC
- ✅ **Sistema de Ventas** y detalle de ventas
- ✅ **Almacenamiento de Imágenes** en carpetas locales
- ✅ **Dashboard Administrativo** con estadísticas
- ✅ **Interfaz Responsiva** con AdminLTE 3.4

## 📋 Requisitos Previos

- **Java 17 o superior**
- **Maven 3.6+**
- **MariaDB 10.5+** (o MySQL 8.0+)
- **Git**

## 🚀 Instalación y Configuración

### 1. Clonar o descargar el proyecto

```bash
cd c:\Users\Jesmar\Downloads\desarrollo_web
```

### 2. Crear la base de datos en MariaDB

```sql
CREATE DATABASE desarrollo_web CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER 'admin'@'localhost' IDENTIFIED BY 'admin123';
GRANT ALL PRIVILEGES ON desarrollo_web.* TO 'admin'@'localhost';
FLUSH PRIVILEGES;
```

### 3. Configurar application.properties

Edita `src/main/resources/application.properties`:

```properties
# ===== BASE DE DATOS =====
spring.datasource.url=jdbc:mariadb://localhost:3306/desarrollo_web
spring.datasource.username=admin
spring.datasource.password=admin123
spring.datasource.driver-class-name=org.mariadb.jdbc.Driver

# ===== JPA/HIBERNATE =====
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=false
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.MariaDBDialect

# ===== SERVIDOR =====
server.port=8080
server.servlet.context-path=/

# ===== ALMACENAMIENTO DE ARCHIVOS =====
app.upload.dir=uploads
app.validacion.api-url=https://miapi.cloud/panel
```

### 4. Compilar e instalar dependencias

```bash
./mvnw.cmd clean install
```

### 5. Ejecutar la aplicación

```bash
./mvnw.cmd spring-boot:run
```

La aplicación estará disponible en: **http://localhost:8080**

## 🔐 Credenciales por Defecto

| Campo | Valor |
|-------|-------|
| **Usuario** | `admin` |
| **Contraseña** | `admin123` |

> ⚠️ Cambia estas credenciales en producción

## 📁 Estructura de Carpetas

```
uploads/
├── productos/      # Imágenes de productos
├── categorias/     # Imágenes de categorías
├── clientes/       # Imágenes de clientes
├── perfiles/       # Fotos de perfil de usuarios
├── sliders/        # Imágenes del slider
└── imagenes/       # Imágenes generales
```

Todas estas carpetas se crean automáticamente en el primer inicio.

## 🗄️ Estructura de la Base de Datos

### Entidades Principales

- **Usuario** - Usuarios del sistema
- **Perfil** - Perfiles de acceso
- **Modulo** - Módulos del sistema
- **Categoria** - Categorías de productos
- **Producto** - Productos de la bodega
- **Cliente** - Clientes registrados
- **Venta** - Registros de ventas
- **DetalleVenta** - Detalles de cada venta
- **Imagen** - Almacenamiento de metadatos de imágenes
- **ImagenSlider** - Imágenes del slider de inicio
- **TipoPago** - Tipos de pago disponibles
- **TipoComprobante** - Tipos de comprobante (Factura, Boleta, etc.)

## 🎨 Características de la Interfaz

### Dashboard
- Estadísticas en tiempo real
- Cards informativos con iconos
- Acceso rápido a módulos principales
- Información del sistema

### Módulos de Gestión
- **Usuarios**: CRUD completo con carga de imágenes
- **Perfiles**: Asignación de módulos
- **Categorías**: Gestión con imágenes
- **Productos**: Inventario con relaciones a categorías
- **Clientes**: Validación DNI/RUC automática
- **Ventas**: Registro de transacciones
- **Imágenes**: Gestor de archivos
- **Configuración**: Tipos de pago y comprobantes

### Estilos
- AdminLTE 3.4 framework
- Bootstrap 5.3.2
- Font Awesome 6.5.1
- Diseño responsivo
- Gradientes modernos
- Animaciones suaves

## 🔄 Flujo de Almacenamiento de Imágenes

1. **Subida**: Usuario carga la imagen desde formulario
2. **Validación**: Se valida el formato y tamaño
3. **Generación**: Se genera UUID único para el archivo
4. **Almacenamiento**: Se guarda en carpeta local `uploads/{categoria}`
5. **Registro**: Se guarda referencia en BD (tabla `IMAGEN`)
6. **Visualización**: Se sirve desde ruta `/archivos/{carpeta}/{uuid}`

## 🔐 Seguridad

- Spring Security configurado
- Encriptación de contraseñas con BCrypt
- Control de acceso por roles
- Validación de DNI/RUC con API externa
- CSRF protection enabled
- Manejo seguro de archivos

## 🌐 Validación DNI/RUC

El sistema valida automáticamente:
- **DNI**: 8 dígitos
- **RUC**: 11 dígitos

Se conecta a: `https://miapi.cloud/panel`

Para cambiar el servidor de validación, edita `app.validacion.api-url` en `application.properties`.

## 📊 Catálogo Público

Accesible en `/catalogo` sin autenticación:
- Lista de productos activos
- Filtrado por categoría
- Información de precios
- Disponibilidad de stock

## 🛠️ Desarrollo

### Agregar nuevo módulo

1. Crear modelo en `Models/`
2. Crear repositorio en `Repository/` (interface)
3. Crear controlador en `controller/`
4. Crear vistas en `templates/`

### Agregar nuevas rutas de imagen

```java
// En FileStorageService
Files.createDirectories(this.uploadDir.resolve("nueva-carpeta"));
```

## 🐛 Solución de Problemas

### Error: "No se puede conectar a BD"
- Verifica que MariaDB esté corriendo
- Confirma credenciales en `application.properties`
- Revisa el puerto (por defecto 3306)

### Carpeta de uploads no se crea
- Verifica permisos de escritura en el directorio del proyecto
- Revisa logs para mensajes de error específicos

### Imágenes no se visualizan
- Revisa que la carpeta `uploads/` exista
- Confirma que el navegador carga desde `/archivos/{carpeta}/{archivo}`
- Verifica permisos del archivo

## 📝 API REST (Próxima versión)

Se planea agregar endpoints REST para:
- Productos
- Ventas
- Clientes
- Reportes

## 📄 Licencia

Proyecto de uso personal - Bodega Jesmar 2026

## 👨‍💻 Soporte

Para reportar problemas o sugerencias, contacta al equipo de desarrollo.

---

**Hecho con ❤️ para Bodega Jesmar**
