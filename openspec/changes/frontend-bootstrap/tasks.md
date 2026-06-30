# Tasks: frontend-bootstrap

> **Orden TDD estricto (Red → Green → Refactor).** Primero los tests con Vitest + RTL + **MSW** (deben fallar), luego la implementación mínima para que pasen, luego el refactor. Ninguna implementación sin un test rojo previo. (Coherente con `docs/TESTING-STRATEGY.md`.)
>
> **Decisiones del explore (aplicadas):** (1) **dev-proxy de Vite + `baseURL` relativo (`/parking-api/api/v1`), sin CORS** en ningún entorno; `VITE_API_URL` es override opcional vacío por defecto. (2) Mocks de red con **MSW**. (3) **Node 22** (dev y CI). (4) **3 puertas de QA**: el gate de A2 es **mockeado** (tests MSW + build + lint + CI); el **login e2e real (Playwright)** NO entra en A2 → es la Puerta 3 de integración tras mergear con `init-auth-local`.

## 1. Tests primero — RED (deben fallar antes de implementar)
- [ ] 1.1 `AuthProvider`: `should_expose_user_when_me_returns_200` y `should_expose_null_when_me_returns_401` (mock de `GET /auth/me`).
- [ ] 1.2 `ProtectedRoute`: `should_redirect_to_login_when_no_session`, `should_redirect_to_change_password_when_password_must_change`, `should_render_children_when_authenticated`.
- [ ] 1.3 `LoginPage`: `should_render_form`, `should_redirect_by_role_on_success`, `should_show_inline_error_when_credentials_invalid` (mock 401).
- [ ] 1.4 `ChangePasswordPage`: `should_validate_policy_before_submit` y `should_submit_when_policy_met` (mock `POST /auth/change-password`).
- [ ] 1.5 Interceptor Axios: `should_emit_session_expired_when_response_401` → la modal "Sesión expirada" se muestra.
- [ ] 1.6 i18n: `should_switch_texts_when_language_toggled` y persistencia en `localStorage`.
- [ ] 1.7 Tema: `should_add_dark_class_and_persist_when_theme_toggled`.

## 2. Implementación — GREEN (lo mínimo para que los tests pasen)
- [ ] 2.1 Init **Vite 5 + React 18** (`frontend/`), `package.json`, ESLint + Prettier + `.editorconfig`; `vitest.config` con cobertura (≥80%).
- [ ] 2.2 **Design system propio**: `styles/` con variables (light + dark) reproduciendo `docs/mockups/styles.css`; **Tabler Icons**; **sin Tailwind/PostCSS/MUI/shadcn**.
- [ ] 2.3 React Router: rutas `/login`, `/change-password`, `/admin`, `/employee` (layouts vacíos con `<Outlet/>`).
- [ ] 2.4 Cliente **Axios** (`apiClient`): `withCredentials`, `baseURL` **relativo** `/parking-api/api/v1` (override opcional `VITE_API_URL`), interceptores 401 (evento sesión expirada)/403/5xx (toast). `vite.config` con **dev-proxy** `'/parking-api' → http://localhost:8080` (sin CORS).
- [ ] 2.5 **TanStack Query** (`QueryClientProvider`) + `AuthProvider`/`useAuth` (Context) consumiendo `GET /auth/me`.
- [ ] 2.6 `ProtectedRoute` (sesión + rol opcional + respeto de `passwordMustChange`).
- [ ] 2.7 Componentes base: `Button`, `Input`, `Modal`, `Toast`, `Spinner` (accesibles, del design system).
- [ ] 2.8 `AppHeader` (logo + curva SVG corporativa + toggles tema/idioma + logout) y `Sidebar` base.
- [ ] 2.9 `AdminLayout` / `EmployeeLayout` (header + sidebar + `<Outlet/>`), vacíos.
- [ ] 2.10 **i18n** react-i18next (ES/EN, `common` + persistencia) y `ThemeProvider` (persistencia `.dark`).
- [ ] 2.11 `LoginPage` (`POST /auth/login`, redirección por rol, error inline).
- [ ] 2.12 `ChangePasswordPage` (validación de política cliente + `POST /auth/change-password`).
- [ ] 2.13 `SessionExpiredModal` suscrita al evento del interceptor 401 → vuelve a `/login`.
- [ ] 2.14 CI: job frontend en `.github/workflows/ci.yml` (Node 22, `npm ci`, `lint`, `build`, `test --coverage`).

## 3. Refactor
- [ ] 3.1 Con los tests en verde: extraer hooks/componentes (complejidad < 15), eliminar duplicación, aplicar `docs/SONAR-STANDARDS.md` (frontend: `const`/`let`, sin `eval`), sin cambiar comportamiento.

## Criterios de aceptación (Puerta A2 — mockeada)
- [ ] Los objetivos verificables del `design.md` que no requieren backend real se cumplen en máquina limpia (rutas/redirecciones, i18n, tema, interceptor 401→modal, build, tests).
- [ ] Gate **mockeado**: `npm run lint` (0 errores) · `npm run build` (sin warnings) · `npm test --coverage` (≥80% líneas / ≥75% branches) con **MSW** · CI verde.
- [ ] El **login e2e real (Playwright)** NO es requisito de A2 → se valida en la **Puerta 3 de integración** tras mergear con `init-auth-local`.
- [ ] Delta de `specs/auth-local` listo para `archive`.
