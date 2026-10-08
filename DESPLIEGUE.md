# Versión candidata — Bodega Jesmar

## 1. Identidad

| Dato | Valor |
|---|---|
| Etiqueta | `v1.0.0-candidato` |
| Plataforma | GitHub + contenedor Docker (Render) |
| Ambiente de comprobación | Local, Windows, Laragon MySQL en `127.0.0.1:3306` |
| Fecha | 2026-10-08 |
| Runtime | Java 21, Spring Boot 4.0.7 |
| Base de datos | MySQL 8.4.3, base `sistema_ventas`, usuario `bodega_app` |
| Dependencias | Spring Web, Security, Data JPA, Thymeleaf, Mail, MySQL Connector/J |
| Construcción | `.\mvnw.cmd -B -DskipTests package` |
| Inicio local | `java -jar target\desarrollo_web-0.0.1-SNAPSHOT.jar` |
| Inicio en la nube | la plataforma ejecuta el `Dockerfile` y publica el puerto `PORT` |

La construcción local del 2026-10-08 terminó en **BUILD SUCCESS**.

## 2. Configuración sin secretos

Las claves no están en Git. Se leen de variables de entorno o, en esta PC, de `src/main/resources/application-local.properties` (está en `.gitignore`).

| Variable | Uso |
|---|---|
| `DB_URL` | Dirección JDBC de MySQL |
| `DB_USERNAME` | Usuario de la base |
| `DB_PASSWORD` | Contraseña de la base |
| `MAIL_USERNAME` / `MAIL_PASSWORD` | Correo del código 2FA |
| `API_PERU_TOKEN` | Token de validación |
| `APP_VERSION` | Texto que muestra `/version` |

La plantilla vacía está en `application-local.properties.example`.

## 3. Versión desplegada

Endpoint público: `GET /version`

Comprobado en local:

```json
{"aplicacion":"sistema-ventas-jesmar","version":"1.0.0-candidato","etiqueta":"v1.0.0-candidato","runtime":"Java 21","fecha":"2026-10-08"}
```

En la nube la misma ruta queda en `https://<servicio>/version`.

`GET /salud` responde si la base de datos está conectada, sin mostrar claves.

## 4. Evidencia de construcción, arranque y logs

Construcción: `.\mvnw.cmd -B -DskipTests package` → BUILD SUCCESS.

Arranque del jar, extracto del log:

```text
HikariPool-1 - Start completed.
Tomcat started on port 8080 (http)
Started DesarrolloWebApplication in 36.212 seconds
```

No apareció un fallo de conexión a MySQL ni un fallo al abrir el puerto.

## 5. Pruebas de humo

Ejecutadas con `scripts\pruebas-humo.ps1` contra `http://localhost:8080` el 2026-10-08. Resultado: 5 de 5.

| Prueba | Resultado | Evidencia |
|---|---|---|
| Disponibilidad | PASA | `GET /version` respondió 200 con la versión candidata |
| Flujo crítico | PASA | `GET /login` respondió 200 y muestra usuario y contraseña |
| Base de datos | PASA | `GET /salud` respondió `baseDatos: conectada` |
| Autenticación | PASA | `GET /` sin sesión respondió 401 |
| Error relevante | PASA | Usuario inexistente vuelve a `/login?error=true` |

## 6. Activación y reversión

**Se activa** esta versión solo si las cinco pruebas pasan y `/version` muestra `1.0.0-candidato`.

**Reversión:** si la versión nueva falla, detener ese proceso y volver a la etiqueta estable anterior.

```text
git checkout v1.0.0-estable
.\mvnw.cmd -B -DskipTests package
java -jar target\desarrollo_web-0.0.1-SNAPSHOT.jar
```

En Render, elegir el deploy anterior en el panel y pulsar Rollback. Esta entrega es la primera candidata: no reemplaza una versión estable previa hasta que el profesor la acepte. Mientras tanto, la etiqueta de trabajo es `v1.0.0-candidato`.
