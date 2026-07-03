# parking · frontend

SPA **React 18 + Vite 5** del proyecto parking (ALEATICA). Design system propio
(CSS + variables + Tabler Icons; sin Tailwind/MUI/shadcn). Estado de servidor con
TanStack Query; auth/tema/idioma con React Context. i18n ES/EN (react-i18next).

## Requisitos

- **Node 22+**

## Scripts

```bash
npm install          # instalar dependencias
npm run dev          # dev server en http://localhost:5173
npm run build        # typecheck + build de produccion
npm run lint         # ESLint (0 errores requerido)
npm test             # tests (Vitest + RTL + MSW)
npm run test:coverage
```

## Red / API (sin CORS)

El cliente Axios usa un **`baseURL` relativo**: `/parking-api/api/v1`, idéntico en
desarrollo y producción.

- **Desarrollo**: el **dev-proxy de Vite** reenvía `/parking-api` →
  `http://localhost:8080`. El navegador habla siempre con `localhost:5173`, de
  modo que las cookies de sesión (`parking_SESSION`) funcionan sin CORS.
- **Producción**: la SPA se sirve same-origin desde Tomcat; el mismo `baseURL`
  relativo resuelve sin cambios.
- `VITE_API_URL` es un **override opcional** (vacío por defecto). Solo se define
  para apuntar a un host distinto. Ver `.env.example`.

## Estructura

```
src/
  api/          apiClient (Axios + interceptores 401/403/5xx), authApi, events
  auth/         AuthProvider, useAuth, ProtectedRoute, passwordPolicy
  components/   Button, Input, Modal, Toast, Spinner, AppHeader, Sidebar,
                SessionExpiredModal, LanguageToggle, ThemeToggle, BrandCurve, AuthShell
  layouts/      AdminLayout, EmployeeLayout (vacíos, con <Outlet/>)
  pages/        LoginPage, ChangePasswordPage
  routes/       AppRoutes, paths
  theme/        ThemeProvider, themeContext (modo oscuro, body.theme-dark)
  i18n/         configuración + locales es/en
  styles/       tokens.css (light + dark), base.css, components.css
  mocks/        MSW handlers + fixtures (tests)
  test/         renderWithProviders (wrapper común de tests)
```

## Autenticación (Fase 1)

- `AuthProvider` consulta `GET /auth/me` al montar (`CurrentUser`).
- `ProtectedRoute`: exige sesión, respeta `passwordMustChange` (→ `/change-password`)
  y rol opcional (`ADMIN`/`EMPLOYEE`).
- `LoginPage`: `POST /auth/login` → redirección por rol; error inline en 401.
- `ChangePasswordPage`: valida la política en cliente + `POST /auth/change-password`.
- Interceptor `401` → evento → `SessionExpiredModal` → vuelta a `/login`.
