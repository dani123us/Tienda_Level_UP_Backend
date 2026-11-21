# Tienda Level-Up Gamer – Backend

API REST de la Tienda Level-Up Gamer (DSY1104 Desarrollo Fullstack II – Evaluación Parcial 3).
Hecha con **Spring Boot 3.5**, **Spring Data JPA**, **MySQL**, **Spring Security + JWT** y documentada con **Swagger**.

Frontend (React): https://github.com/dani123us/Tienda_Level_UP

## Requisitos

- Java 17 o superior
- MySQL 8 (opcional: también se puede correr con H2 en memoria)
- No hace falta instalar Maven: el proyecto trae Maven Wrapper (`mvnw` / `mvnw.cmd`)

## Cómo ejecutar

### Con MySQL (perfil por defecto)

La base de datos `levelup_db` se crea sola la primera vez (`createDatabaseIfNotExist=true`).

```powershell
cd Tienda_Level_UP_Backend
$env:DB_USUARIO="root"          # opcional, por defecto root
$env:DB_PASSWORD="tu_clave"
.\mvnw.cmd spring-boot:run
```

En CMD: `set DB_PASSWORD=tu_clave` y luego `mvnw spring-boot:run`.

### Sin MySQL (H2 en memoria)

```powershell
$env:PERFIL="h2"
.\mvnw.cmd spring-boot:run
```

Los datos de H2 se pierden al detener la aplicación (se vuelven a cargar los datos iniciales).

### Pruebas

```powershell
.\mvnw.cmd test
```

32 pruebas: unitarias (descuentos, pago simulado, checkout con Mockito) e integración con MockMvc + H2
(login, 401, 403, roles, refresh token, checkout pagado y rechazado).

## Swagger

- UI: http://localhost:8080/swagger-ui.html
- JSON OpenAPI: http://localhost:8080/v3/api-docs

Para probar endpoints privados: ejecutar `POST /api/v1/auth/login`, copiar el `accessToken` y pegarlo en **Authorize**.

## Usuarios de prueba

| Rol | Correo | Contraseña |
|---|---|---|
| Administrador | admin@duoc.cl | admin123 |
| Vendedor | vendedor@duoc.cl | vendedor123 |
| Cliente | cliente@gmail.com | cliente123 |

**Tarjeta de prueba:** cualquier número que termine en `0000` es rechazado por la pasarela simulada (sirve para mostrar el pago fallido).
**Cupones:** `LEVELUP10` (10%) y `GAMER15` (15%). Los correos `@duoc.cl` y `@profesor.duoc.cl` tienen 20% extra.

## Modelo de datos

```
USUARIO 1──* BOLETA 1──* DETALLE_BOLETA *──1 PRODUCTO *──1 CATEGORIA
```

| Tabla | Campos principales |
|---|---|
| usuario | id, run (único), nombre, apellidos, correo (único), password (BCrypt), rol, fecha_nacimiento, region, comuna, direccion, telefono |
| categoria | id, nombre (único) |
| producto | id, codigo (único), nombre, descripcion, precio, stock, stock_critico, imagen, icono, destacado, categoria_id |
| boleta | id, fecha, estado (PAGADA/RECHAZADA), usuario_id, datos de despacho, metodo_pago, ultimos_digitos_tarjeta, motivo_rechazo, cupon, subtotal, porcentaje_descuento, descuento, total |
| detalle_boleta | id, boleta_id, producto_id (nullable), codigo_producto, nombre_producto, cantidad, precio_unitario, subtotal |

El detalle guarda una copia del nombre, código y precio del producto, así la boleta no cambia si después se edita o elimina el producto.

## Endpoints (`/api/v1`)

| Método | Ruta | Descripción | Acceso |
|---|---|---|---|
| POST | `/auth/login` | Login: devuelve `accessToken`, `refreshToken`, `expiraEnSegundos` y `usuario` | Público |
| POST | `/auth/registro` | Registro de cliente (devuelve tokens) | Público |
| POST | `/auth/refresh` | Renueva el par de tokens (rotación) | Público (requiere refresh válido) |
| GET | `/auth/me` | Usuario logueado | Autenticado |
| GET | `/productos?categoria=` | Lista de productos | Público |
| GET | `/productos/{id}` | Detalle de producto | Público |
| POST | `/productos` | Crear producto | Administrador |
| PUT | `/productos/{id}` | Editar producto | Administrador |
| DELETE | `/productos/{id}` | Eliminar producto | Administrador |
| GET | `/categorias`, `/categorias/{id}` | Listar / detalle | Público |
| POST, PUT, DELETE | `/categorias[/{id}]` | CRUD de categorías | Administrador |
| GET | `/usuarios`, `/usuarios/{id}` | Listar / detalle | Administrador |
| POST, PUT, DELETE | `/usuarios[/{id}]` | CRUD de usuarios | Administrador |
| POST | `/boletas` | Checkout (crear boleta y pagar) | Cliente, Administrador |
| GET | `/boletas/mias` | Mis compras | Autenticado |
| GET | `/boletas` | Lista de órdenes | Administrador, Vendedor |
| GET | `/boletas/{id}` | Detalle de una orden | Administrador, Vendedor, o el cliente dueño |
| DELETE | `/boletas/{id}` | Eliminar boleta | Administrador |

Códigos: 200, 201, 204, 400 (validación, con mapa `campos`), 401 (sin token o vencido), 403 (sin permiso), 404, 409 (regla de negocio).
Todos los errores usan el mismo formato:

```json
{ "fecha": "...", "estado": 409, "error": "Conflict", "mensaje": "Stock insuficiente para ...", "ruta": "/api/v1/boletas", "campos": null }
```

## Seguridad

- API **stateless**: no hay sesión en el servidor, el token viaja en `Authorization: Bearer <token>`.
- Tokens firmados con **HS256**. Token de acceso: 15 minutos. Refresh token: 7 días.
  Cada token lleva el tipo (`acceso` o `refresh`): un refresh no sirve como token de acceso.
- `JwtAuthFilter` valida el token y **recarga el usuario desde la BD** en cada petición (un cambio de rol se aplica al tiro).
- Reglas por rol con `@PreAuthorize` en cada método de los controladores.
- Contraseñas cifradas con **BCrypt**. Las respuestas nunca incluyen la contraseña (se usan DTOs).
- CORS habilitado solo para `http://localhost:5173` (configurable con `CORS_ORIGENES`).
- La clave JWT se puede cambiar con la variable de entorno `JWT_SECRETO`.

## Lógica de negocio del checkout (`POST /boletas`)

Todo dentro de una transacción:

1. Agrupa los productos repetidos del carrito.
2. Valida que cada producto exista (404) y que haya stock (409 con el nombre y el stock disponible).
3. Calcula el subtotal con los **precios de la base de datos** (no confía en el precio del frontend).
4. Aplica descuentos: 20% Duoc + cupón (cupón inválido → 409).
5. Procesa el pago simulado (tarjeta terminada en `0000` → rechazado).
6. Pagado: descuenta el stock. Rechazado: guarda el motivo y **no** toca el stock.
7. Responde `201` con la boleta y su `estado`.

## Estructura

```
cl.duoc.levelup
├── config/       OpenApiConfig (Swagger), DataInitializer (datos iniciales)
├── controller/   Controladores REST /api/v1
├── dto/          Records de entrada y salida
├── exception/    Excepciones y GlobalExceptionHandler
├── model/        Entidades JPA y enums
├── repository/   Repositorios Spring Data JPA
├── security/     JwtService, JwtAuthFilter, UsuarioDetailsService, SecurityConfig
└── service/      Lógica de negocio
```
