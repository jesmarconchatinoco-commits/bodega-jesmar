# 📐 ARQUITECTURA - Bodega Jesmar

## 🏗️ Estructura del Proyecto

```
desarrollo_web/
├── pom.xml                              # Configuración Maven
├── mvnw / mvnw.cmd                     # Maven Wrapper
├── README_BODEGA.md                    # Documentación principal
├── SETUP.md                            # Guía de configuración
├── ARQUITECTURA.md                     # Este archivo
├── init-database.sql                   # Script de BD
│
├── src/main/java/com/test/desarrollo_web/
│   ├── DesarrolloWebApplication.java   # Punto de entrada Spring Boot
│   │
│   ├── Models/                         # Entidades JPA
│   │   ├── Usuario.java
│   │   ├── Perfil.java
│   │   ├── Modulo.java
│   │   ├── Producto.java
│   │   ├── Categoria.java
│   │   ├── Cliente.java
│   │   ├── Ventas.java
│   │   ├── DetalleVenta.java
│   │   ├── Imagen.java
│   │   ├── ImagenSlider.java
│   │   ├── TipoPago.java
│   │   └── TipoComprobante.java
│   │
│   ├── Repository/                     # Interfaces JPA Repository
│   │   ├── UsuarioRepository.java
│   │   ├── PerfilRepository.java
│   │   ├── ModuloRepository.java
│   │   ├── ProductoRepository.java
│   │   ├── CategoriaRepository.java
│   │   ├── ClienteRepository.java
│   │   ├── VentaRepository.java
│   │   ├── DetalleVentaRepository.java
│   │   ├── ImagenRepository.java
│   │   ├── ImagenSliderRepository.java
│   │   ├── TipoPagoRepository.java
│   │   └── TipoComprobanteRepository.java
│   │
│   ├── controller/                     # Controladores MVC (15 total)
│   │   ├── HomeController.java         # Dashboard
│   │   ├── UsuarioController.java      # CRUD Usuarios
│   │   ├── PerfilController.java       # CRUD Perfiles
│   │   ├── ModuloController.java       # CRUD Módulos
│   │   ├── ProductoController.java     # CRUD Productos
│   │   ├── CategoriaController.java    # CRUD Categorías
│   │   ├── ClienteController.java      # CRUD Clientes + validación DNI/RUC
│   │   ├── VentaController.java        # CRUD Ventas
│   │   ├── DetalleVentaController.java # CRUD Detalles de Venta
│   │   ├── ImagenController.java       # CRUD Imágenes
│   │   ├── ImagenSliderController.java # CRUD Slider
│   │   ├── TipoPagoController.java     # CRUD Tipos de Pago
│   │   ├── TipoComprobanteController.java
│   │   ├── CatalogoController.java     # Catálogo público
│   │   └── FileController.java         # Servicio de archivos
│   │
│   ├── security/                       # Spring Security
│   │   ├── SecurityConfig.java         # Configuración de seguridad
│   │   └── CustomUserDetailsService.java
│   │
│   ├── service/                        # Servicios de negocio
│   │   ├── FileStorageService.java     # Gestión de archivos
│   │   └── ValidacionDniRucService.java # API externa de validación
│   │
│   └── config/
│       └── DataInitializer.java        # Datos iniciales
│
├── src/main/resources/
│   ├── application.properties           # Configuración Spring
│   │
│   ├── static/                          # Archivos estáticos
│   │   ├── css/
│   │   ├── js/
│   │   └── images/
│   │
│   └── templates/                       # Vistas Thymeleaf (30+ archivos)
│       ├── layout.html                  # Layout master
│       ├── login.html                   # Página de login
│       ├── dashboard.html               # Dashboard principal
│       ├── catalogo.html                # Catálogo público
│       │
│       ├── usuarios/
│       │   ├── list.html
│       │   └── form.html
│       │
│       ├── productos/
│       │   ├── list.html
│       │   └── form.html
│       │
│       ├── categorias/
│       │   ├── list.html
│       │   └── form.html
│       │
│       ├── clientes/
│       │   ├── list.html
│       │   └── form.html
│       │
│       ├── perfiles/
│       │   ├── list.html
│       │   └── form.html
│       │
│       ├── ventas/
│       │   ├── list.html
│       │   └── form.html
│       │
│       └── [otros módulos]/
│           ├── list.html
│           └── form.html
│
├── src/test/
│   └── java/...
│
└── uploads/                             # Almacenamiento de imágenes
    ├── productos/
    ├── categorias/
    ├── clientes/
    ├── perfiles/
    ├── sliders/
    └── imagenes/
```

---

## 🔄 Flujo de Datos

### Flujo de una Solicitud HTTP

```
1. Cliente (Navegador)
         ↓
2. Spring Security (Verificar autenticación)
         ↓
3. DispatcherServlet (Enrutador)
         ↓
4. Controller (Procesamiento)
         ↓
5. Service (Lógica de negocio)
         ↓
6. Repository (Acceso a datos via JPA/Hibernate)
         ↓
7. MariaDB (Base de datos)
         ↓
8. Respuesta (JSON o Thymeleaf template)
         ↓
9. Cliente (Navegador renderiza HTML)
```

### Ejemplo: Crear un Producto

```
POST /productos/guardar
    ↓
ProductoController.guardar()
    ↓
- Valida datos del formulario
- Procesa archivo de imagen
    ↓
FileStorageService.saveFile()
    ↓
- Genera UUID
- Crea carpeta si no existe
- Guarda archivo en /uploads/productos/
- Retorna objeto Imagen con ruta
    ↓
ProductoRepository.save()
    ↓
- INSERT en tabla PRODUCTO
- INSERT en tabla IMAGEN
    ↓
redirect:/productos
    ↓
ProductoController.listar()
    ↓
Renderiza productos/list.html con datos actualizados
```

---

## 🗄️ Modelo de Datos (Entidades)

### Relaciones principales

```
┌─────────────────────────────────────────────────┐
│                   USUARIO                       │
│  - id (PK)                                      │
│  - nombre, usuario, password                    │
│  - correo                                       │
│  - perfil_id (FK→PERFIL)                        │
│  - imagen_id (FK→IMAGEN)                        │
└─────────────────────────────────────────────────┘
        ↓                          ↓
        │                          │
   ┌────▼─────┐            ┌──────▼────────┐
   │  PERFIL   │            │     IMAGEN    │
   │ - id (PK) │            │  - id (PK)    │
   │ - nombre  │            │  - nombre     │
   │ - estado  │            │  - ruta       │
   └────┬─────┘            └──────┬────────┘
        │                         │
        │ M:M                      ├─► PRODUCTO
        │                          │
   ┌────▼──────────┐              │
   │    MODULO      │             ├─► CATEGORIA
   │  - id (PK)     │             │
   │  - nombre      │             ├─► CLIENTE
   │  - descripción │             │
   └────────────────┘             ├─► USUARIO
                                   │
                              ┌────▼─────────┐
                              │   CATEGORIA  │
                              │  - id (PK)   │
                              │  - nombre    │
                              │  - estado    │
                              └────┬─────────┘
                                   │
                              ┌────▼────────────┐
                              │    PRODUCTO     │
                              │  - id (PK)      │
                              │  - nombre       │
                              │  - precio       │
                              │  - stock        │
                              │  - imagen_id    │
                              │  - categoria_id │
                              │  - estado       │
                              └─────────────────┘
                                   │
                              ┌────▼─────────────────┐
                              │    DETALLE_VENTA     │
                              │  - id (PK)           │
                              │  - venta_id (FK)     │
                              │  - producto_id (FK)  │
                              │  - cantidad          │
                              │  - precio_unitario   │
                              │  - subtotal          │
                              └──────────────────────┘
                                   ▲
                                   │
                              ┌────┴────────────┐
                              │    VENTAS       │
                              │  - id (PK)      │
                              │  - cliente_id   │
                              │  - vendedor_id  │
                              │  - tipo_pago_id │
                              │  - total        │
                              │  - estado       │
                              └─────────────────┘
```

---

## 🔐 Capas de Seguridad

### Spring Security

```
1. Autenticación
   - Usuario/contraseña en form login
   - CustomUserDetailsService carga Usuario desde BD
   - Spring compara contraseña hasheada con BCrypt
   
2. Autorización
   - Perfil → Módulos (ManyToMany)
   - ROLE_ prefijo: ROLE_ADMIN, ROLE_USUARIO
   - @Secured("ROLE_ADMIN") en métodos
   - antMatchers("/admin/**").hasRole("ADMIN")

3. Sesión
   - HttpSession almacena Authentication
   - Cookies seguras
   - CSRF protection habilitada

4. Endpoints protegidos
   ✅ /dashboard                  → Todos autenticados
   ✅ /usuarios/**                → ROLE_ADMIN
   ✅ /productos/**               → Autenticados
   ✅ /catalogo                   → Público
   ✅ /archivos/**                → Público (imágenes)
   ❌ /login                       → Público
```

---

## 📁 Gestión de Archivos

### Sistema de Almacenamiento Local

```
Solicitud de subida
    ↓
FileStorageService.saveFile(MultipartFile, String folder)
    ↓
┌─────────────────────────────────────────┐
│ 1. Validar archivo                      │
│    - ¿No vacío?                         │
│    - ¿Extensión válida?                 │
│    - ¿Tamaño < 10MB?                    │
└─────────────────────────────────────────┘
    ↓
┌─────────────────────────────────────────┐
│ 2. Generar nombre único                 │
│    UUID.randomUUID() + extensión        │
│    Ej: a1b2c3d4.jpg                     │
└─────────────────────────────────────────┘
    ↓
┌─────────────────────────────────────────┐
│ 3. Crear carpeta si no existe           │
│    /uploads/{folder}/                   │
│    Ej: /uploads/productos/              │
└─────────────────────────────────────────┘
    ↓
┌─────────────────────────────────────────┐
│ 4. Guardar archivo físico               │
│    Path: uploads/productos/a1b2c3d4.jpg │
└─────────────────────────────────────────┘
    ↓
┌─────────────────────────────────────────┐
│ 5. Registrar en BD                      │
│    INSERT INTO IMAGEN                   │
│    - nombre: original_filename.jpg      │
│    - ruta: productos/a1b2c3d4.jpg       │
│    - tipo: image/jpeg                   │
│    - tamaño: 256000                     │
└─────────────────────────────────────────┘
    ↓
FileController.GET /archivos/{folder}/{filename}
    ↓
UrlResource.readFile(path)
    ↓
Navegador descarga/visualiza
```

### Carpetas de Almacenamiento

| Carpeta | Uso | Estructura |
|---------|-----|-----------|
| `productos/` | Imágenes de productos | UUID.ext |
| `categorias/` | Imágenes de categorías | UUID.ext |
| `clientes/` | Fotos de clientes | UUID.ext |
| `perfiles/` | Avatares de usuarios | UUID.ext |
| `sliders/` | Imágenes del slider | UUID.ext |
| `imagenes/` | Almacenamiento general | UUID.ext |

---

## 🌐 Integración API Externa

### ValidacionDniRucService

```
ClienteController.guardar()
    ↓
ValidacionDniRucService.validarDocumento(String dni)
    ↓
GET https://miapi.cloud/panel/DNI/12345678
    ↓
API Response:
{
  "success": true,
  "nombre": "Juan Pérez",
  "estado": "ACTIVO"
}
    ↓
¿Válido?
├─ Sí → Guardar cliente
└─ No → Mostrar error
```

---

## 🎨 Tecnologías Frontend

### Stack Frontend

- **Bootstrap 5.3.2** - Framework CSS
- **AdminLTE 3.4** - Template administrativo
- **Font Awesome 6.5.1** - Iconografía
- **Thymeleaf 5.3.2** - Template engine (servidor)
- **JavaScript vanilla** - Interactividad

### Componentes AdminLTE usados

```html
<!-- Small boxes (estadísticas) -->
<div class="small-box bg-info">
    <div class="inner">
        <h3>123</h3>
        <p>Usuarios</p>
    </div>
</div>

<!-- Cards -->
<div class="card card-primary">
    <div class="card-header">Título</div>
    <div class="card-body">Contenido</div>
</div>

<!-- Tables -->
<table class="table table-hover table-striped">
    ...
</table>

<!-- Alerts -->
<div class="alert alert-success">
    Mensaje exitoso
</div>

<!-- Badges -->
<span class="badge bg-primary">Estado</span>
```

---

## 🚀 Flujo de Despliegue

```
Desarrollo
    ↓
./mvnw.cmd clean install
    ↓
Tests & Compilación
    ↓
./mvnw.cmd spring-boot:run
    ↓
Servidor Tomcat en puerto 8080
    ↓
Producción (futuro)
    ↓
Java JAR o Docker container
    ↓
Cloud (Azure, AWS, etc.)
```

---

## 🔧 Configuración de Propiedades

### application.properties

```properties
# Base de Datos
spring.datasource.url=...              # Conexión JDBC
spring.datasource.username=...         # Usuario BD
spring.datasource.password=...         # Contraseña BD

# JPA/Hibernate
spring.jpa.hibernate.ddl-auto=update   # Crear/actualizar tablas
spring.jpa.show-sql=false              # Log SQL
spring.jpa.properties.hibernate.dialect=...

# Thymeleaf
spring.thymeleaf.cache=false           # No cachear templates
spring.thymeleaf.encoding=UTF-8

# Archivos
spring.servlet.multipart.max-file-size=10MB
app.upload.dir=uploads

# Servidor
server.port=8080
server.servlet.context-path=/

# Validación
app.validacion.api-url=https://miapi.cloud/panel
```

---

## ✅ Checklist de Componentes

- [x] Modelos JPA con relaciones correctas
- [x] Repositorios JPA (interfaces)
- [x] Controladores CRUD (15 total)
- [x] Spring Security con roles
- [x] Servicio de almacenamiento de archivos
- [x] Servicio de validación DNI/RUC
- [x] Thymeleaf templates (30+)
- [x] AdminLTE styling
- [x] Dashboard con estadísticas
- [x] Catálogo público
- [x] Base de datos MariaDB
- [x] Script de inicialización
- [x] Documentación completa

---

**Arquitectura Bodega Jesmar v1.0 - 2026**
