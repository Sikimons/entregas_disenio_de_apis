# Ruta · Gestion de entregas

Aplicacion independiente con frontend React, backend Java 17 / Spring Boot y
PostgreSQL. Gestiona sus propias facturas, productos, PIN, fotos, coordenadas,
conductores e historial. El paquete Java es `com.ruta.deliverypin`.

## Iniciar con datos de prueba

```powershell
docker compose -f docker-compose.mock.yml up -d --build
```

- Aplicacion: http://localhost:15180
- API: http://localhost:18091
- Administrador: `admin` / `admin123`
- Conductor existente: `conductor` / `conductor123`

En una instalacion nueva, crea el conductor desde **Equipo**. El administrador
se crea automaticamente. `demo/invoices.json` carga las 59 facturas originales
solo si no hay facturas en la base. Incluye las 50 facturas aleatorias con 314
lineas de supermercado en total; los PIN se conservan en los CSV de `demo/`.
El volumen `middleware_data` conserva usuarios, entregas, fotos y facturas.
Los reinicios no restablecen datos.

## Uso

- **Panorama:** mapa, resultados y actividad del equipo.
- **Facturas:** buscar, crear un borrador, publicar/generar PIN y exportar CSV.
- **Equipo:** crear cuentas, activar/desactivar y eliminar usuarios.
- **Conductor:** buscar factura, verificar productos, capturar foto y confirmar
  con PIN y GPS; tambien permite reportar incidencias y consultar historial.

Solo los administradores pueden consultar los PIN. La busqueda del conductor
muestra hasta 20 resultados; utiliza el numero completo para encontrar una factura.
En el navegador permite el acceso a ubicacion/camara desde `localhost`.

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
Para generar otro lote de supermercado y su CSV:

```powershell
python scripts/generate_invoices.py --count 50
```

El lote nuevo se agrega sin borrar el anterior; su CSV se guarda en
`demo/nuevo_lote_facturas_y_pines.csv`. Configura `RUTA_API`, `RUTA_ADMIN` y
`RUTA_PASSWORD` si cambias los datos de acceso locales.

El backend usa `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USERNAME`, `DB_PASSWORD`,
`JWT_SECRET`, `CORS_ALLOWED_ORIGINS`, `ADMIN_USERNAME` y `ADMIN_PASSWORD`.
`APP_DEMO_ENABLED` es `false` por defecto. Para cargar datos de demostracion,
habilitalo y configura `APP_DEMO_FILE` con la ruta del JSON.

El Compose habitual (`docker-compose.yml`) conserva los puertos 5180/8091 y
lee `backend/.env`. No combines ambos Compose. Configura las claves y el origen
de la aplicacion para ese entorno. El frontend usa `/api` en el mismo origen.

```powershell
docker compose -f docker-compose.mock.yml down
```

Este comando apaga los servicios conservando sus datos. Los respaldos locales
de la migracion estan en `output/backups/` y no se versionan.
