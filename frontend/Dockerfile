# --- Etapa de build ---
FROM node:24-alpine AS build
WORKDIR /app

# corepack ya viene con Node 24; activa la version de pnpm fijada en "packageManager"
# (package.json), evitando depender de una version instalada globalmente en la imagen.
RUN corepack enable

COPY package.json pnpm-lock.yaml ./
RUN corepack install && pnpm install --frozen-lockfile

COPY . .
RUN pnpm run build \
    # _headers/_redirects son formato propio de Cloudflare Pages: aqui nginx no los
    # interpreta (frontend/nginx.conf ya trae su propio SPA fallback y sus cabeceras de
    # seguridad), y _headers quedaria con los marcadores __API_ORIGIN__/__TILES_ORIGIN__
    # sin sustituir (eso solo ocurre en el build de CI/CD para Cloudflare Pages).
    && rm -f dist/_headers dist/_redirects

# --- Etapa de runtime ---
FROM nginx:1.30-alpine
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY security-headers.conf /etc/nginx/snippets/security-headers.conf
COPY --from=build /app/dist /usr/share/nginx/html

EXPOSE 80
