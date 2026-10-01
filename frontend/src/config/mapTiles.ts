// Tiles servidos same-origin via /map-tiles/ (proxy de nginx) por defecto: algunas redes
// moviles/corporativas bloquean CDNs de terceros directo. En despliegues sin ese proxy
// (Cloudflare Pages) VITE_MAP_TILES_URL es obligatoria y apunta directo al proveedor; sin
// ella, Pages responde index.html (200 text/html) a cada tesela y el mapa queda en blanco.
export const MAP_TILES_URL = import.meta.env.VITE_MAP_TILES_URL || '/map-tiles/{z}/{x}/{y}.png'

export const MAP_ATTRIBUTION =
  '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors'
