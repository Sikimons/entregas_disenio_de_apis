## [1.0.2](https://github.com/ups-master/ruta-delivery/compare/v1.0.1...v1.0.2) (2026-09-28)

### Bug Fixes

* **cd:** reordenar build-images antes de release, y saltar deploy sin variables ([89c3cc9](https://github.com/ups-master/ruta-delivery/commit/89c3cc97a2cf8ad9bd23b8c87d1ed8a1894c19ee))

## [1.0.1](https://github.com/ups-master/ruta-delivery/compare/v1.0.0...v1.0.1) (2026-09-27)

### Bug Fixes

* **cd:** resolver 'Cannot infer ref from detached HEAD' en publish-images ([17e3ee1](https://github.com/ups-master/ruta-delivery/commit/17e3ee1c944ef5006334e4d96ed8ebe59b15868a))

## 1.0.0 (2026-09-27)

### Features

* add new factura and pin entries to nuevo_lote_facturas_y_pines.csv ([00fef64](https://github.com/ups-master/ruta-delivery/commit/00fef64a9710a5a0c2589f27700cb6b02ad8cfd7))
* add new factura and pin entries to nuevo_lote_facturas_y_pines.csv ([138953f](https://github.com/ups-master/ruta-delivery/commit/138953f470bd15b46647dfc4a5db7af777a7b2a8))
* add Vite configuration for PWA support, invoice generation script, and delivery verification script ([a04198d](https://github.com/ups-master/ruta-delivery/commit/a04198d6c83b4c2d9f00a9debb7cce448d2312d6))
* **backend:** completar casos de uso y puertos hexagonales de dominio ([c58e092](https://github.com/ups-master/ruta-delivery/commit/c58e0922f2a3fe78dde6e01596622916c4524e6a))
* **backend:** completar la migracion a timestamptz (V4) ([a23a3e8](https://github.com/ups-master/ruta-delivery/commit/a23a3e83eb1a8225a3e85e1ebb6a00718763bf7b))
* **backend:** limite de confirmaciones por conductor e Idempotency-Key ([ce39062](https://github.com/ups-master/ruta-delivery/commit/ce3906299ac692754d72eb252ce6179391863bf6))
* **backend:** migrar facturas y costo operativo a JPA con Flyway ([3df61db](https://github.com/ups-master/ruta-delivery/commit/3df61db5d51652ebf8b523a29ff89f9f55ec6098))
* **backend:** paginar GET /api/v1/admin/invoices ([99647a2](https://github.com/ups-master/ruta-delivery/commit/99647a2dfe2f97bad8f4894ebf598cb7ab059be3))
* **backend:** paginar GET /api/v1/admin/users ([f9ed97d](https://github.com/ups-master/ruta-delivery/commit/f9ed97d2f4a6ac57c64e0176d152f37c1d8e7c48))
* **backend:** registrar quien crea y publica cada factura ([b4796d4](https://github.com/ups-master/ruta-delivery/commit/b4796d4451bb8ee1419b126a79d34167aeb7825e))
* **backend:** resiliencia, rate limiting de login y endurecimiento de arranque ([81d60d7](https://github.com/ups-master/ruta-delivery/commit/81d60d7c52c54c7178f91791a1bc54ff8ecf9293))
* **backend:** unificar el contrato de error en ErrorResponse ([d54198c](https://github.com/ups-master/ruta-delivery/commit/d54198cae88cbcfb4390cf1c44e78fba23732104))
* **backend:** versionar la API en /api/v1 y unificar el formato de error ([08bc276](https://github.com/ups-master/ruta-delivery/commit/08bc276b3fcf94353f9bffef913fee6354282741))
* **frontend:** cabeceras de seguridad, CSP y despliegue estático ([468d0eb](https://github.com/ups-master/ruta-delivery/commit/468d0eb15cdc058139cf6ed7260057fa649fdc25))
* **frontend:** migrar la SPA de JavaScript a TypeScript estricto ([effe378](https://github.com/ups-master/ruta-delivery/commit/effe37883b666fc36bcbc5b6b0d9d26e6b50824a))
* **frontend:** paginar admin/invoices y admin/users, tipar errores y extraer useAsyncData ([24d22bc](https://github.com/ups-master/ruta-delivery/commit/24d22bcf8038cd1d3a08f1cbcd6b42ec16788b2a))
* integrate Swagger UI and OpenAPI documentation for API endpoints ([8d470f7](https://github.com/ups-master/ruta-delivery/commit/8d470f7e599eb7f4b2f50e4ea416c82912ff79c0))
* **security:** JWT en cookie HttpOnly con CSRF, en vez de localStorage ([5392026](https://github.com/ups-master/ruta-delivery/commit/53920268c30f26e45670257c824db07226083278))
* update nginx configuration for map tiles and improve tile layer attribution in admin and driver history pages ([41dcec1](https://github.com/ups-master/ruta-delivery/commit/41dcec1ccf6f9bb9ca91183ffcc197047785d4dc))
* update README with detailed deployment instructions and add nuevo_lote_facturas_y_pines.csv ([8e2facb](https://github.com/ups-master/ruta-delivery/commit/8e2facba52028d6374556137b2778278a3cffdf1))

### Bug Fixes

* add docker-compose.yml to .gitignore to prevent accidental commits ([eee9732](https://github.com/ups-master/ruta-delivery/commit/eee97324d7ccfb44f13a967c260bf1066727c305))
* **backend:** unificar tamano de pagina por defecto en /admin/users a 20 ([ef0502f](https://github.com/ups-master/ruta-delivery/commit/ef0502fbda60baecf5a2a56463063654a625b76a))
* **frontend:** anunciar errores y estado de GPS a lectores de pantalla ([7c0ab5b](https://github.com/ups-master/ruta-delivery/commit/7c0ab5b0a91d21977ad7cd65039927f0d252f97c))
* **load-tests:** adaptar los escenarios de k6 a la cookie de sesion ([ea30bae](https://github.com/ups-master/ruta-delivery/commit/ea30bae469efe214a610789d8df4dece801a9968))
* **release:** agregar conventional-changelog-conventionalcommits como dependencia explicita ([1703527](https://github.com/ups-master/ruta-delivery/commit/17035270b1620ec0a7ca4d9402af8b128d892971))
* **release:** usar RELEASE_TOKEN (PAT de admin) para el push del changelog y el back-merge ([6887a49](https://github.com/ups-master/ruta-delivery/commit/6887a49c046afa147ec77a5ae6c0ca9538b13b46))
* **scripts:** adaptar verify_delivery.py a la cookie de sesion + CSRF ([0556975](https://github.com/ups-master/ruta-delivery/commit/0556975761089858e307c7faa3a39f091f2f5082))

### Performance Improvements

* **frontend:** code-split por ruta y separar vendors del bundle ([62629a8](https://github.com/ups-master/ruta-delivery/commit/62629a8ed9cdcd7a0aab01850443cb2c6f2dc957))

### Reverts

* Revert "docs: reemplazar diagramas SVG por versiones actualizadas" ([2cf10de](https://github.com/ups-master/ruta-delivery/commit/2cf10debbd891c48392a305beb778432b041bc62))
