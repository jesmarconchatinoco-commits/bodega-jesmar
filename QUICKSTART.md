# 🚀 INICIO RÁPIDO - Bodega Jesmar

## ⏱️ En 5 minutos

### 1. Iniciar la Base de Datos

```powershell
# Si no lo has hecho, ejecuta el script SQL en tu gestor de BD:
init-database.sql
```

### 2. Iniciar la Aplicación

```powershell
cd "c:\Users\Jesmar\Downloads\desarrollo_web"
./mvnw.cmd spring-boot:run
```

### 3. Acceder

```
URL: http://localhost:8080
Usuario: admin
Contraseña: admin123
```

---

## 📌 Primeras Acciones

### A. Crear tu Primer Producto

1. Haz clic en **Productos** en la barra lateral
2. Botón **Nuevo Producto**
3. Completa:
   - Nombre: "Mi Producto"
   - Precio: 99.99
   - Stock: 100
   - Categoría: Electrónica
   - Sube una imagen
4. **Guardar**

### B. Crear un Cliente

1. Ve a **Clientes**
2. **Nuevo Cliente**
3. Completa:
   - Nombre: "Juan Pérez"
   - DNI: 12345678 (será validado automáticamente)
   - Teléfono: 999888777
   - Email: juan@email.com
4. **Guardar**

### C. Registrar una Venta

1. Ve a **Ventas**
2. **Nueva Venta**
3. Selecciona:
   - Cliente: "Juan Pérez"
   - Tipo de Pago: "Efectivo"
   - Comprobante: "Factura"
4. Agrega productos
5. **Guardar**

### D. Ver Catálogo Público

1. Abre otra pestaña: http://localhost:8080/catalogo
2. Verás todos los productos ACTIVOS
3. **Sin necesidad de login**

---

## 🎨 Interfaz Principal

```
┌─────────────────────────────────────────────────────┐
│  [Bodega Jesmar]                    [admin ▼] [Salir]│  ← Header
├──────────┬──────────────────────────────────────────┤
│          │ Panel de Control                         │
│ MENÚ     │                                          │
│ ────     │ [Usuarios: 1] [Productos: 5]            │
│          │ [Categorías: 3] [Clientes: 2]          │
│ 📊 Home  │                                          │
│ 👥 Usuarios                                         │
│ 📦 Productos  │ Accesos Rápidos                   │
│ 🏷️ Categorías │ - Catálogo Público               │
│ 👨 Clientes   │ - Validación DNI/RUC             │
│ 💰 Ventas    │                                     │
│ ⚙️ Configuración                                    │
│          │                                          │
└──────────┴──────────────────────────────────────────┘
```

---

## 📂 Estructura de Carpetas del Sistema

```
uploads/
├── productos/          ← Imágenes de productos
├── categorias/        ← Imágenes de categorías
├── clientes/          ← Fotos de clientes
├── perfiles/          ← Avatares de usuarios
├── sliders/           ← Imágenes del slider
└── imagenes/          ← Otras imágenes
```

Todas se crean automáticamente.

---

## 🔑 Credenciales y Acceso

### Usuario por Defecto
- **Usuario**: admin
- **Contraseña**: admin123

### Crear Nuevo Usuario (como ADMIN)

1. Ve a **Usuarios** → **Nuevo Usuario**
2. Completa:
   - Nombre: "Juan Técnico"
   - Usuario: "juan"
   - Contraseña: "juan123"
   - Email: juan@empresa.com
   - Perfil: ADMIN (o crear nuevo)
   - Foto de perfil: (opcional)
3. **Guardar**

---

## 🏷️ Gestión de Categorías

### Crear Categoría

1. **Categorías** → **Nueva Categoría**
2. Nombre: "Electrónica"
3. Descripción: "Productos electrónicos variados"
4. Subir imagen
5. **Guardar**

> Las categorías aparecen en el catálogo público

---

## 💳 Configurar Formas de Pago

### Tipos de Pago Predefinidos

Ya vienen listos:
- Efectivo
- Tarjeta de Crédito
- Tarjeta de Débito
- Transferencia
- Cheque

### Agregar Nuevo Tipo de Pago

1. **Configuración** → **Tipos de Pago** → **Nuevo**
2. Nombre: "Criptografía"
3. **Guardar**

---

## 📄 Comprobantes de Venta

### Tipos Disponibles

- **Factura** (F-001, F-002, ...)
- **Boleta** (B-001, B-002, ...)
- **Nota de Crédito** (NC-001, ...)
- **Nota de Débito** (ND-001, ...)

Los correlatives avanzan automáticamente.

---

## 🔍 Buscar y Filtrar

### En Listados

La mayoría de vistas permiten:
- Ver tabla de datos
- Editar (botón amarillo)
- Eliminar (botón rojo)
- Estado visual con badges

### Ejemplo: Productos

| Imagen | Nombre | Categoría | Precio | Stock | Estado | Acciones |
|--------|--------|-----------|--------|-------|--------|----------|
| [IMG]  | Laptop | Electrónica | S/. 1500 | 5 | ACTIVO | [Editar] [Eliminar] |

---

## 🖼️ Cargar Imágenes

### Desde Cualquier Formulario

1. Encuentra el campo "Seleccionar imagen"
2. Haz clic para elegir archivo
3. Formatos soportados: JPG, PNG, GIF, WebP
4. Máximo: 10 MB
5. **Guardar** automáticamente

**¿A dónde se guardan?**

Depende del tipo:
- Producto → `/uploads/productos/`
- Categoría → `/uploads/categorias/`
- Cliente → `/uploads/clientes/`
- Usuario → `/uploads/perfiles/`

El nombre se genera automáticamente (UUID)

---

## ✅ Validación DNI/RUC

### Automática al crear Cliente

```
¿DNI de 8 dígitos? → Valida como DNI
¿RUC de 11 dígitos? → Valida como RUC

Ejemplo:
- 12345678 → DNI válido
- 20123456789 → RUC válido
```

La API externa lo verifica y retorna datos.

**Si falla la validación**:
- Error: "Documento no válido"
- Verifica número ingresado
- Intenta nuevamente

---

## 📊 Dashboard Explicado

### Cards de Estadísticas

**Usuarios**: Total de usuarios registrados
- Haz clic para ir a gestión de usuarios

**Productos**: Total de productos en BD
- Haz clic para ir a productos

**Categorías**: Total de categorías
- Haz clic para ir a categorías

**Clientes**: Total de clientes registrados
- Haz clic para ir a clientes

### Información del Sistema

- **Ventas**: Registro de transacciones
- **Perfiles**: Roles disponibles
- **Módulos**: Funciones del sistema
- **Seguridad**: Activa (Spring Security)

---

## 🌐 Catálogo Público (Visitantes)

### Acceso Sin Login

```
http://localhost:8080/catalogo
```

**Qué ven**:
- Productos ACTIVOS solamente
- Filtro por categoría
- Imágenes y precios
- Sin acceso a edición

---

## 🔐 Cambiar Contraseña

### Como Usuario Administrador

1. Edita tu usuario
2. Busca campo "Contraseña"
3. Ingresa nueva contraseña
4. **Guardar**

> La contraseña se encripta automáticamente

---

## 📱 Responsive Design

### Funciona en

- ✅ Desktop (1920px+)
- ✅ Tablet (768px - 1024px)
- ✅ Móvil (320px - 767px)

Menú se colapsa en pantallas pequeñas.

---

## 🆘 Solucionar Problemas

### Error: "Conexión rechazada"

```
Causa: MariaDB no está corriendo
Solución: Inicia el servicio MariaDB en Windows
```

### Error: "Acceso denegado"

```
Causa: Contraseña incorrecta
Solución: Usa admin / admin123
```

### Imágenes no se cargan

```
Causa: Carpeta uploads no existe
Solución: Crear manualmente o reiniciar app
```

### Aplicación muy lenta

```
Causa: Demasiados datos
Solución: Optimizar índices en BD
```

---

## 📞 Shortcuts Útiles

| Acción | Ruta |
|--------|------|
| Dashboard | `/` o `/dashboard` |
| Login | `/login` |
| Usuarios | `/usuarios` |
| Productos | `/productos` |
| Categorías | `/categorias` |
| Clientes | `/clientes` |
| Ventas | `/ventas` |
| Catálogo Público | `/catalogo` |
| Logout | `/logout` |

---

## 🎓 Tips y Trucos

### Crear Múltiples Registros Rápido

1. Abre lista de elemento
2. Nuevo → Completa → Guardar
3. Se recarga automáticamente
4. Repite

### Buscar Dentro de la Tabla

Usa Ctrl+F del navegador en tablas

### Exportar Datos

Copia la tabla (Ctrl+A) y pega en Excel

### Organizar Sidebar

El menú se adapta a tu rol/perfil

---

## ⚙️ Configuraciones Importantes

### En `application.properties`

```properties
# Puerto (cambiar aquí si 8080 está ocupado)
server.port=8080

# Tamaño máximo de archivo
spring.servlet.multipart.max-file-size=10MB

# API de validación
app.validacion.api-url=https://miapi.cloud/panel
```

---

## 📚 Documentos Relacionados

- 📖 **README_BODEGA.md** - Descripción del proyecto
- 🔧 **SETUP.md** - Instalación completa
- 📐 **ARQUITECTURA.md** - Diseño técnico
- ✨ **FEATURES.md** - Características detalladas
- 🚀 **QUICKSTART.md** - Este documento

---

## 🎯 Siguientes Pasos

1. ✅ Instalar y ejecutar
2. ✅ Crear un usuario
3. ✅ Agregar categorías
4. ✅ Subir productos
5. ✅ Registrar clientes
6. ✅ Realizar ventas
7. ✅ Ver catálogo público
8. ✅ Personalizar según necesidad

---

**¡Listo para comenzar con Bodega Jesmar!**

Cualquier duda, consulta la documentación incluida.
