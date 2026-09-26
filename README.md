# Ruta · Gestion de entregas

Aplicacion independiente con frontend React, backend Java 17 / Spring Boot y
PostgreSQL. Gestiona sus propias facturas, productos, PIN, fotos, coordenadas,
conductores e historial. El paquete Java es `com.ruta.deliverypin`.

## Desplegar con data mock

Necesitas Docker Desktop abierto. Para generar lotes adicionales tambien necesitas
Python 3; la carga inicial no requiere Python ni ejecutar un script manualmente.

### 1. Levantar los servicios

Abre PowerShell en la carpeta del proyecto y ejecuta:

```powershell
cd C:\Users\fabia\Desktop\middleware_account_move_delivery_pin
docker compose -f docker-compose.mock.yml up -d --build
```

Este comando levanta PostgreSQL, el backend y el frontend. El archivo Compose
ya activa `APP_DEMO_ENABLED=true` y monta `demo/invoices.json` para la carga inicial.
Espera a que termine el arranque del backend. Puedes consultar el estado y los logs:

```powershell
docker compose -f docker-compose.mock.yml ps
docker compose -f docker-compose.mock.yml logs --tail 50 backend
```

### 2. Consultar las facturas y los PIN

- Aplicacion: http://localhost:15180
- API: http://localhost:18091
- Administrador: `admin` / `admin123`
- Conductor existente: `conductor` / `conductor123`

Entra como administrador y abre **Facturas** para consultar los datos y los PIN.
En una instalacion nueva, crea el conductor desde **Equipo**. El administrador
se crea automaticamente. `demo/invoices.json` carga las 59 facturas originales
solo si no hay facturas en la base. El conjunto contiene 314 lineas de productos
e incluye las 50 facturas aleatorias de supermercado. Sus numeros y PIN estan en
[`demo/facturas_y_pines.csv`](demo/facturas_y_pines.csv).
El volumen `middleware_data` conserva usuarios, entregas, fotos y facturas.
Si ya tienes facturas cargadas, volver a levantar los servicios no las reemplaza
ni vuelve a importar el JSON.

### 3. Generar otras 50 facturas (opcional)

Con los servicios encendidos, ejecuta desde la misma carpeta:

```powershell
python scripts/generate_invoices.py --count 50
```

El script crea y publica 50 facturas adicionales con productos de supermercado,
numeros aleatorios como `001-104-0000001234` y PIN de seis digitos. Se agregan a las
facturas existentes. Actualiza la pantalla **Facturas** para verlas.

El CSV del nuevo lote se guarda en `demo/nuevo_lote_facturas_y_pines.csv`.
Cada ejecucion reemplaza ese CSV, pero conserva las facturas anteriores en la base.
Puedes cambiar `--count 50` por la cantidad deseada, entre 1 y 1000.
El script usa por defecto la API `http://localhost:18091` y `admin` / `admin123`;
configura `RUTA_API`, `RUTA_ADMIN` y `RUTA_PASSWORD` si cambias esos valores.

### 4. Apagar el entorno conservando los datos

```powershell
docker compose -f docker-compose.mock.yml down
```

Para volver a iniciarlo, repite el comando del paso 1. Las facturas, los PIN y las
entregas permanecen guardados en el volumen de PostgreSQL.

## Uso

- **Panorama:** mapa, resultados y actividad del equipo.
- **Facturas:** buscar, crear un borrador, publicar/generar PIN y exportar CSV.
- **Equipo:** crear cuentas, activar/desactivar y eliminar usuarios.
- **Conductor:** buscar factura, verificar productos, capturar foto y confirmar
  con PIN y GPS; tambien permite reportar incidencias y consultar historial.

Solo los administradores pueden consultar los PIN. La busqueda del conductor
muestra hasta 20 resultados; utiliza el numero completo para encontrar una factura.
En el navegador permite el acceso a ubicacion/camara desde `localhost`.

## Swagger UI y OpenAPI

Con el entorno mock encendido, abre:

- **Swagger UI:** http://localhost:18091/swagger-ui/index.html
- **OpenAPI JSON:** http://localhost:18091/v3/api-docs
- **OpenAPI YAML:** http://localhost:18091/v3/api-docs.yaml

Si acabas de incorporar esta configuracion, reconstruye el backend:

```powershell
docker compose -f docker-compose.mock.yml up -d --build backend
```

Para probar los endpoints desde Swagger:

1. Abre **Autenticacion → POST /api/auth/login → Try it out**.
2. Ingresa `admin` / `admin123` (o las credenciales de tu conductor) y pulsa **Execute**.
3. Copia el campo `token` de la respuesta.
4. Pulsa **Authorize**, pega solo el token, sin escribir `Bearer`, y confirma.
5. Abre cualquier endpoint permitido para tu rol y usa **Try it out → Execute**.

La documentacion se puede abrir sin iniciar sesion; las operaciones de negocio
mantienen la autenticacion JWT y sus permisos. Las llamadas desde Swagger usan
la base del entorno actual y pueden crear o modificar datos.
Con `docker-compose.yml`, usa el puerto `8091` en las URL anteriores.

## API de facturas (administrador)

- `GET /api/admin/invoices?q=`: consultar facturas, estados y PIN.
- `POST /api/admin/invoices`: crear borrador.
- `POST /api/admin/invoices/{id}/publish`: publicar; genera un PIN de seis digitos
  si se requiere y no existe, y conserva un PIN generado anteriormente.

Ejemplo de creacion:

```json
{
  "number": "001-104-0000001234",
  "partnerName": "Cliente de prueba",
  "deliveryAddress": "Calle de prueba, Guayaquil",
  "latitude": -2.170998,
  "longitude": -79.922359,
  "requiresPin": true,
  "products": [{"description": "Arroz blanco - funda 1 kg", "quantity": 2}]
}
```

La confirmacion y la foto/historial se guardan en una unica transaccion. Se rechazan
facturas inexistentes, borradores, facturas sin PIN, PIN incorrectos y entregas
repetidas. Las confirmaciones simultaneas se serializan mediante bloqueo de fila.

## Desarrollo y despliegue

Para verificar el flujo completo sin reiniciar las facturas existentes:

```powershell
python scripts/verify_delivery.py
```

Las pruebas crean sus propios pedidos con prefijo `VERIFY/`. Comprueban permisos,
preservacion de datos, publicacion, PIN, concurrencia, foto, GPS e incidencias.

El backend usa `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USERNAME`, `DB_PASSWORD`,
`JWT_SECRET`, `CORS_ALLOWED_ORIGINS`, `ADMIN_USERNAME` y `ADMIN_PASSWORD`.
`APP_DEMO_ENABLED` es `false` por defecto. Para cargar datos de demostracion,
habilitalo y configura `APP_DEMO_FILE` con la ruta del JSON.

El Compose habitual (`docker-compose.yml`) conserva los puertos 5180/8091 y
lee `backend/.env`. No combines ambos Compose. Configura las claves y el origen
de la aplicacion para ese entorno. El frontend usa `/api` en el mismo origen.

Los respaldos locales de la migracion estan en `output/backups/` y no se versionan.
