# Proposal: frontend-bootstrap

## Why
Dejar el **frontend** en un estado en el que los siguientes changes
funcionales puedan construir pantallas sin reconfigurar nada. Equivalente
a `bootstrap-mvp` pero para la SPA. **No** añade pantallas de gestión:
solo arranque + autenticación de Fase 1 + layouts vacíos.

## What Changes
- Scaffold **React 18 + Vite 5** con **design system propio** (CSS propio + variables + Tabler Icons; **sin Tailwind, sin MUI, sin shadcn**), reproduciendo los tokens de `docs/mockups/styles.css` (y `docs/design-system.md` cuando exista).
- **i18n** (react-i18next, ES por defecto + EN) y **modo oscuro** (toggle manual, `.dark`), ambos persistidos en `localStorage`.
- **Cliente Axios** con `withCredentials: true` + interceptores (401/403/5xx).
- **Estado**: **TanStack Query** (estado de servidor) + **React Context** (auth/tema/idioma).
- `AuthProvider` + `useAuth` + `ProtectedRoute` consumiendo `GET /auth/me`.
- Pantallas: **Login** (Fase 1) y **Cambio de contraseña obligatorio** (tras reset).
- Interceptor `401` → **modal "Sesión expirada"** → vuelta a `/login`.
- `AdminLayout` y `EmployeeLayout` **vacíos** (sidebar + header + `<Outlet/>`), con toggles de tema/idioma y logout.
- Job de **CI** para frontend (lint + build + test con cobertura).

## Capabilities afectadas (delta mínimo)
- `auth-local` — **ADDED**: scenarios de UI (login OK/KO, cambio obligatorio tras reset, sesión expirada). El backend de auth ya existe (su change funcional).

## Impact
- Dir `frontend/`; nuevo job en `.github/workflows/ci.yml`; var `VITE_API_URL`.
- Identidad git `lcasadov`; rama `feat/frontend/<issue>-frontend-bootstrap`.

## Out of scope
- Pantallas de gestión (employees, parking-spaces, requests, visitors, calendario, plano, exportaciones).
- SSO Fase 2 (solo placeholder).
- Hosting/deploy del frontend (change posterior).
