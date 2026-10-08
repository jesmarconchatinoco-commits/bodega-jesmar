# 🔧 GUÍA DE CONFIGURACIÓN - Bodega Jesmar

## Paso 1: Preparar el Entorno

### 1.1 Descargar e Instalar Requisitos

```powershell
# Verificar Java instalado
java -version

# Verificar Maven instalado
mvn -version

# Descargar e instalar MariaDB si no lo tienes:
# https://mariadb.org/download/
```

### 1.2 Iniciar Servicios

```powershell
# MariaDB debe estar corriendo en puerto 3306
# Verifica en: Services (Windows) o systemctl (Linux)
```

---

## Paso 2: Crear Base de Datos

### 2.1 Opción A: Usando HeidiSQL o MySQL Workbench

1. Abre HeidiSQL o MySQL Workbench
2. Conecta a localhost con usuario `root`
3. Abre el archivo `init-database.sql`
4. Ejecuta el script completo
5. Verifica que se creó la BD `desarrollo_web`

### 2.2 Opción B: Línea de comandos

```powershell
cd "c:\Program Files\MariaDB 10.5\bin"  # Ajusta la ruta según tu versión

# Conectar a MariaDB
mysql -u root

# Ejecutar script
mysql -u root < "c:\Users\Jesmar\Downloads\desarrollo_web\init-database.sql"

# Verificar
mysql -u admin -padmin123
USE desarrollo_web;
SHOW TABLES;
```

---

## Paso 3: Configurar la Aplicación

### 3.1 Editar application.properties

Edita: `src/main/resources/application.properties`

```properties
# Si usas contraseña diferente en MariaDB:
spring.datasource.username=tu_usuario
spring.datasource.password=tu_password

# Ruta de almacenamiento (crear si no existe):
app.upload.dir=uploads

# API de validación (opcional):
app.validacion.api-url=https://miapi.cloud/panel
```

### 3.2 Crear carpeta de uploads

```powershell
# En Windows (PowerShell)
cd "c:\Users\Jesmar\Downloads\desarrollo_web"
mkdir uploads
mkdir uploads/productos
mkdir uploads/categorias
mkdir uploads/clientes
mkdir uploads/perfiles
mkdir uploads/sliders
mkdir uploads/imagenes
```

O simplemente ejecuta la app - las carpetas se crean automáticamente.

---

## Paso 4: Compilar el Proyecto

```powershell
cd "c:\Users\Jesmar\Downloads\desarrollo_web"

# Limpiar y compilar
./mvnw.cmd clean install

# O solo compilar
./mvnw.cmd clean compile
```

**Espera pacientemente** - La primera vez descarga todas las dependencias.

### 4.1 Solucionar errores comunes

```
ERROR: Class not found 'MariaDBDriver'
→ Verifica que MariaDB JDBC esté en pom.xml

ERROR: Connection refused
→ MariaDB no está corriendo, inicia el servicio

ERROR: Access denied
→ Verifica credenciales en application.properties
```

---

## Paso 5: Ejecutar la Aplicación

### 5.1 Opción A: Maven (Recomendado)

```powershell
cd "c:\Users\Jesmar\Downloads\desarrollo_web"
./mvnw.cmd spring-boot:run
```

### 5.2 Opción B: IDE (Eclipse/IntelliJ)

1. Abre el proyecto en tu IDE
2. Clic derecho → Run As → Spring Boot App
3. O ejecuta `DesarrolloWebApplication.java`

### 5.3 Esperando inicio

Deberías ver en consola:
```
Tomcat started on port(s): 8080 (http)
Started DesarrolloWebApplication in X.XXX seconds
```

---

## Paso 6: Acceder a la Aplicación

### 6.1 Login

**URL**: http://localhost:8080

**Credenciales por defecto**:
- Usuario: `admin`
- Contraseña: `admin123`

### 6.2 Primera vez

- Completa el perfil si es necesario
- Ve a Dashboard para ver estadísticas
- Explora los módulos disponibles

---

## Paso 7: Verificar Componentes

### 7.1 Dashboard

Accede a: http://localhost:8080/dashboard

Deberías ver:
- Cards con números de usuarios, productos, categorías
- Botones de acceso rápido
- Información del sistema

### 7.2 Almacenamiento de Imágenes

Prueba:
1. Ve a Productos → Nuevo Producto
2. Sube una imagen
3. Guarda el producto
4. La imagen se guardará en `uploads/productos/`

### 7.3 Catálogo Público

Accede a: http://localhost:8080/catalogo

Deberías ver productos sin necesidad de login.

---

## Paso 8: Configuración Avanzada (Opcional)

### 8.1 Cambiar Puerto

En `application.properties`:
```properties
server.port=9000
```

### 8.2 Base de datos remota

```properties
spring.datasource.url=jdbc:mariadb://tu_servidor.com:3306/desarrollo_web
```

### 8.3 Logging

```properties
logging.level.root=INFO
logging.level.com.test.desarrollo_web=DEBUG
```

### 8.4 Desactivar caché Thymeleaf (desarrollo)

```properties
spring.thymeleaf.cache=false
```

---

## Problemas y Soluciones

### ❌ Error: "Failed to create ApplicationContext"

**Causa**: Base de datos no configurada o servicio MariaDB no corriendo

**Solución**:
```powershell
# Verificar MariaDB
Get-Service | Where-Object {$_.Name -like "*mariadb*"}

# Iniciar si está detenido
Start-Service MariaDB

# Verificar conexión
mysql -u admin -padmin123 -h localhost
```

### ❌ Error: "Ruta 'uploads' no existe"

**Causa**: Carpeta de almacenamiento no creada

**Solución**:
```powershell
mkdir uploads
# La app creará las subcarpetas automáticamente
```

### ❌ Imágenes no se visualizan

**Causa**: Ruta incorrecta o archivo no guardado

**Solución**:
1. Abre DevTools (F12)
2. Pestaña Network
3. Verifica que `/archivos/productos/...` retorna 200
4. Revisa que el archivo existe en `uploads/productos/`

### ❌ Error: "Access denied for user 'admin'@'localhost'"

**Causa**: Contraseña incorrecta

**Solución**:
```sql
-- Resetear contraseña
ALTER USER 'admin'@'localhost' IDENTIFIED BY 'admin123';
FLUSH PRIVILEGES;
```

### ❌ Error de compilación "Package no existe"

**Causa**: Dependencias no descargadas

**Solución**:
```powershell
./mvnw.cmd clean
./mvnw.cmd install -DskipTests
```

---

## Mantenimiento

### Respaldar Base de Datos

```powershell
# Crear backup
mysqldump -u admin -padmin123 desarrollo_web > backup.sql

# Restaurar backup
mysql -u admin -padmin123 desarrollo_web < backup.sql
```

### Limpiar Carpeta de Uploads

```powershell
# Listar archivos
Get-ChildItem -Recurse uploads

# Eliminar todo
Remove-Item -Recurse uploads/*
```

### Ver logs

Los logs se guardan en consola durante ejecución con:
```
logging.level.com.test.desarrollo_web=DEBUG
```

---

## 🚀 ¡Listo!

Tu aplicación Bodega Jesmar debería estar funcionando correctamente.

**Próximos pasos**:
- ✅ Crear usuarios adicionales
- ✅ Agregar productos y categorías
- ✅ Registrar clientes
- ✅ Hacer ventas
- ✅ Personalizar según necesidades

---

## 📞 Soporte

Si tienes problemas:
1. Revisa los logs en consola
2. Verifica que MariaDB esté corriendo
3. Confirma credenciales en `application.properties`
4. Limpia cache: `./mvnw.cmd clean`

**Documento: Bodega Jesmar 2026**
