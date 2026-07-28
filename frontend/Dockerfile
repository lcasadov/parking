# syntax=docker/dockerfile:1
# =====================================================================
# Frontend parking — imagen multi-stage.
#   Stage build  : compila la SPA (Vite) con Node 20.
#   Stage runtime: nginx sirve /dist y hace reverse-proxy de /parking-api
#                  hacia el contenedor `backend` (same-origin, sin CORS).
# =====================================================================

# ---------- Stage 1: build ----------
FROM node:20-alpine AS build
WORKDIR /app

# Cache de dependencias: primero manifiestos, luego el resto del codigo.
COPY package.json package-lock.json ./
RUN npm ci

COPY . .

# Variables VITE_ (build-time): Vite las hornea en el bundle al compilar. Llegan como
# build-args (desde el .env del servidor, ver docker-compose.app.yml) y se materializan en
# un .env.production que `vite build` carga en modo producción. Sin esto, en producción el
# token de Mapbox y la clave pública VAPID (Web Push) quedarían vacíos.
ARG VITE_API_URL=""
ARG VITE_MAPBOX_TOKEN=""
ARG VITE_VAPID_PUBLIC_KEY=""
RUN printf 'VITE_API_URL=%s\nVITE_MAPBOX_TOKEN=%s\nVITE_VAPID_PUBLIC_KEY=%s\n' \
      "$VITE_API_URL" "$VITE_MAPBOX_TOKEN" "$VITE_VAPID_PUBLIC_KEY" > .env.production

# El script "build" ya ejecuta `tsc --noEmit && vite build` (ver package.json).
RUN npm run build

# ---------- Stage 2: runtime ----------
FROM nginx:1.27-alpine AS runtime

# Config propia: SPA fallback + reverse-proxy de la API.
COPY nginx.conf /etc/nginx/conf.d/default.conf

# Artefactos estaticos de la SPA.
COPY --from=build /app/dist /usr/share/nginx/html

EXPOSE 80

# Usa 127.0.0.1 (no `localhost`): dentro del contenedor `localhost` resuelve
# primero a IPv6 ::1, pero nginx escucha solo en IPv4 (`listen 80;`), lo que
# hacia fallar el healthcheck (connection refused) aunque la app respondia.
HEALTHCHECK --interval=15s --timeout=3s --start-period=10s --retries=5 \
  CMD ["sh", "-c", "wget -qO- http://127.0.0.1/ >/dev/null 2>&1 || exit 1"]

CMD ["nginx", "-g", "daemon off;"]
