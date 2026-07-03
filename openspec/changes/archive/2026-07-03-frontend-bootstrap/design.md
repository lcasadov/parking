# Design: frontend-bootstrap

## Context
Arranque de la SPA **React 18 + Vite 5**, servida same-origin por Tomcat
junto a la API (sin reverse proxy). **Design system
propio** (CSS propio + variables + Tabler Icons), reproduciendo los tokens
de `docs/mockups/styles.css`. Objetivo: esqueleto navegable con auth de
Fase 1 sobre el que construir pantallas en TDD.

**Sin CORS, ni en dev ni en prod.** El cliente usa siempre un `baseURL`
**relativo** (`/parking-api/api/v1`). En desarrollo, el **dev-proxy de Vite**
reenvía `/parking-api` al backend (`http://localhost:8080`), de modo que el
navegador siempre habla con el mismo origen (`localhost:5173`) y las cookies
de sesión (`SameSite`) funcionan sin configuración CORS. En producción la SPA
se sirve same-origin desde Tomcat, por lo que el mismo `baseURL` relativo
resuelve sin cambios.

## Goals (verificables)
Desde una máquina limpia:
1. `npm install` && `npm run dev` levanta en `http://localhost:5173`; el dev-proxy reenvía `/parking-api` a `http://localhost:8080`.
2. Sin sesión, cualquier ruta protegida redirige a `/login`.
3. Login correcto → cookie `parking_SESSION` + redirección a `/admin` o `/employee` según rol.
4. Login incorrecto → error inline (sin navegar).
5. Empleado con `passwordMustChange=true` → redirigido a `/change-password`.
6. Toggle de **idioma** (ES/EN) cambia textos y persiste en `localStorage`.
7. Toggle de **modo oscuro** añade `body.theme-dark` y persiste.
8. Respuesta `401` en cualquier llamada → **modal "Sesión expirada"** → `/login`.
9. `npm run build` sin warnings; `npm test` (Vitest) en verde con cobertura ≥80%.

> **Tres puertas de QA.** El gate de **A2 (este change)** es **mockeado**:
> tests con MSW + `npm run build` + `npm run lint` + CI verde. El **login
> e2e real (Playwright contra backend)** NO entra en A2 → es la **Puerta 3 de
> integración**, posterior al merge con `init-auth-local`. Los criterios de
> aceptación de `tasks.md` no exigen login real en A2.

## Decisions
- **Vite 5** (no CRA): build rápido, dev server con HMR.
- **Design system propio** (no Tailwind, no MUI, no shadcn): CSS propio con variables (light + dark) reproduciendo `docs/mockups/styles.css`; iconos **Tabler Icons**. Cuando exista `docs/design-system.md` (Prompt 6), es la autoridad de tokens/componentes.
- **React Router** para enrutado; rutas en inglés (`/login`, `/change-password`, `/admin`, `/employee`).
- **Axios** con `withCredentials: true`, `baseURL` **relativo** (`/parking-api/api/v1`, idéntico en dev y prod), e interceptores (401 → evento de sesión expirada; 403 → toast; 5xx → toast genérico). `VITE_API_URL` queda como **override opcional** (vacío por defecto): si se define, prefija el `baseURL` relativo.
- **Dev-proxy de Vite (sin CORS)**: `vite.config` → `server.proxy: { '/parking-api': { target: 'http://localhost:8080', changeOrigin: true } }`. El navegador siempre habla con `localhost:5173`; Vite reenvía al backend. Cookies de sesión funcionan sin cabeceras CORS.
- **Node 22** (dev y CI).
- **Estado**: **TanStack Query** para estado de servidor (cache, refetch, mutations) + **React Context** para auth (`useAuth`), tema y idioma. Sin Redux.
- **react-i18next** (ES por defecto, EN), namespace `common` + por módulo; idioma persistido.
- **Vitest + React Testing Library** (no Jest): mismo runtime que Vite. Mock de red con **MSW** (handlers de `/auth/*`), no mock de Axios.
- **Auth**: `AuthProvider` hace `GET /auth/me` al montar; `ProtectedRoute` exige sesión y (opcional) rol; respeta `passwordMustChange`.

## Risks
- `docs/design-system.md` ya existe y es la **autoridad** de tokens/componentes; `docs/mockups/styles.css` es la implementación de referencia. Ambos alineados.
- Cookies same-origin en dev y prod: cubierto por `withCredentials` + `baseURL` relativo + dev-proxy de Vite (sin CORS en ningún entorno).

## Migration Plan
N/A — frontend sin base de datos. La "migración" es la creación del scaffolding `frontend/`.
