# Design: frontend-bootstrap

## Context
Arranque de la SPA **React 18 + Vite 5**, servida same-origin por Tomcat
junto a la API (sin reverse proxy; CORS solo en DES). **Design system
propio** (CSS propio + variables + Tabler Icons), reproduciendo los tokens
de `docs/mockups/styles.css`. Objetivo: esqueleto navegable con auth de
Fase 1 sobre el que construir pantallas en TDD.

## Goals (verificables)
Desde una máquina limpia:
1. `npm install` && `npm run dev` levanta en `http://localhost:5173`.
2. Sin sesión, cualquier ruta protegida redirige a `/login`.
3. Login correcto → cookie `parking_SESSION` + redirección a `/admin` o `/employee` según rol.
4. Login incorrecto → error inline (sin navegar).
5. Empleado con `passwordMustChange=true` → redirigido a `/change-password`.
6. Toggle de **idioma** (ES/EN) cambia textos y persiste en `localStorage`.
7. Toggle de **modo oscuro** añade `.dark` y persiste.
8. Respuesta `401` en cualquier llamada → **modal "Sesión expirada"** → `/login`.
9. `npm run build` sin warnings; `npm test` (Vitest) en verde con cobertura.

## Decisions
- **Vite 5** (no CRA): build rápido, dev server con HMR.
- **Design system propio** (no Tailwind, no MUI, no shadcn): CSS propio con variables (light + dark) reproduciendo `docs/mockups/styles.css`; iconos **Tabler Icons**. Cuando exista `docs/design-system.md` (Prompt 6), es la autoridad de tokens/componentes.
- **React Router** para enrutado; rutas en inglés (`/login`, `/change-password`, `/admin`, `/employee`).
- **Axios** con `withCredentials: true`, `baseURL = VITE_API_URL`, e interceptores (401 → evento de sesión expirada; 403 → toast; 5xx → toast genérico).
- **Estado**: **TanStack Query** para estado de servidor (cache, refetch, mutations) + **React Context** para auth (`useAuth`), tema y idioma. Sin Redux.
- **react-i18next** (ES por defecto, EN), namespace `common` + por módulo; idioma persistido.
- **Vitest + React Testing Library** (no Jest): mismo runtime que Vite. Mock de red con MSW o mock de Axios.
- **Auth**: `AuthProvider` hace `GET /auth/me` al montar; `ProtectedRoute` exige sesión y (opcional) rol; respeta `passwordMustChange`.

## Risks
- `docs/design-system.md` aún **no existe** (Prompt 6 pendiente): se usa `docs/mockups/styles.css` como fuente provisional de tokens; al generar el doc, alinear.
- Same-origin en prod vs CORS en DES: el cliente debe funcionar con cookies en ambos; cubierto por `withCredentials` + `VITE_API_URL`.

## Migration Plan
N/A — frontend sin base de datos. La "migración" es la creación del scaffolding `frontend/`.
