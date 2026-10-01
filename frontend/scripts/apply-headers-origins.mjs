// Sustituye los marcadores __API_ORIGIN__/__TILES_ORIGIN__ de public/_headers (copiado tal
// cual a dist/ por Vite) con los origenes reales de VITE_API_BASE_URL/VITE_MAP_TILES_URL,
// para que la Content-Security-Policy de Cloudflare Pages permita las llamadas a la API y
// las teselas del mapa de ESE entorno (staging/produccion/preview usan dominios distintos).
// Parte del propio script "build" de package.json (corre despues de "vite build", cuando
// dist/_headers ya existe): Cloudflare Pages construye el sitio con su propia integracion
// de Git (build command = "pnpm run build", sin pasar por GitHub Actions), asi que la
// sustitucion tiene que vivir aqui -no en un workflow- para aplicar tanto ahi como en un
// build local. VITE_API_BASE_URL/VITE_MAP_TILES_URL se configuran como variables de build
// por entorno (Production/Preview) directamente en el dashboard de Cloudflare Pages. Si
// falta sustituir un marcador, este script termina con error y el build falla (Cloudflare
// no publica una CSP rota, que bloquearia hasta el login). Ver frontend/public/_headers y
// docs/EVALUACION_TECNICA.md §18 (N5).
import { readFileSync, writeFileSync } from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const headersPath = path.join(__dirname, '..', 'dist', '_headers');

// Cloudflare Pages define CF_PAGES=1 en sus builds. Ahi no existen los proxies same-origin
// /api/ ni /map-tiles/ de nginx: sin estas variables el bundle pediria las teselas a
// /map-tiles/ (que Pages resuelve con index.html, 200 text/html) y el mapa quedaria en
// blanco, asi que se falla el build en vez de publicar con los valores por defecto.
if (process.env.CF_PAGES) {
  const missing = ['VITE_API_BASE_URL', 'VITE_MAP_TILES_URL'].filter((name) => !process.env[name]);
  if (missing.length > 0) {
    throw new Error(
      `Build de Cloudflare Pages sin ${missing.join(' ni ')}: definelas en Settings -> Environment variables (Production y Preview).`,
    );
  }
}

function originOf(name, value, fallback) {
  const raw = value || fallback;
  try {
    return new URL(raw).origin;
  } catch {
    throw new Error(`${name}="${raw}" no es una URL valida; no se puede derivar su origen.`);
  }
}

const apiOrigin = originOf('VITE_API_BASE_URL', process.env.VITE_API_BASE_URL, 'http://localhost/api/v1');
const tilesOrigin = originOf(
  'VITE_MAP_TILES_URL',
  process.env.VITE_MAP_TILES_URL,
  'https://tile.openstreetmap.org/{z}/{x}/{y}.png',
);

let content = readFileSync(headersPath, 'utf8');
content = content.replaceAll('__API_ORIGIN__', apiOrigin).replaceAll('__TILES_ORIGIN__', tilesOrigin);

const leftover = content.match(/__[A-Z_]*_ORIGIN__/);
if (leftover) {
  throw new Error(`Quedo un marcador sin sustituir en dist/_headers: ${leftover[0]}`);
}

writeFileSync(headersPath, content);
console.log(`dist/_headers actualizado: connect-src/img-src -> ${apiOrigin}, ${tilesOrigin}`);
