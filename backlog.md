# Backlog — parking (ALEATICA)

> Backlog autoritativo del proyecto. Se sincroniza con **GitHub Projects v2** (`lcasadov/parking`)
> mediante el agente `gh-projects-sync`. Jerarquía: **Épica** (Milestone) → **Historia de usuario**
> (`type:user-story`) → **Tarea** (`type:task`). Estados: `Backlog → In Progress → In Review → Done`.
> Prioridad MoSCoW: `must / should / could / wont`.
>
> Última actualización: **2026-07-12**.

---

## Épicas

| Épica | Estado | Notas |
|---|---|---|
| E1 · Autenticación local (Fase 1) | ✅ Done | Login/logout/me/change-password, bloqueo 5/15min, política de contraseña, Spring Session JDBC |
| E2 · Gestión de empleados | ✅ Done | CRUD (alta/edición/baja lógica/reactivar), reset de contraseña admin |
| E3 · Plazas de parking | ✅ Done | CRUD + activar/desactivar + configuración total |
| E4 · Puestos de oficina (desks) | ✅ Done | CRUD + activación dedicada (`PATCH /activation`) + categoría |
| E5 · Plano interactivo (floor-plan) | ✅ Done | Render + marcadores + posicionamiento + solicitud desde plano |
| E6 · Asignaciones fijas | ✅ Done | Set semanal por empleado, revocación por recurso |
| E7 · Solicitudes | ✅ Done | Alta (plaza/puesto), bandeja admin, aprobar/rechazar/cancelar |
| E8 · Liberaciones | ✅ Done | Voluntaria (empleado) + administrativa (admin) |
| E9 · Visitantes | ✅ Done | Fichas + reservas de visita |
| E10 · Disponibilidad y calendario | ✅ Done | Disponibilidad por fecha, calendario admin, «Mi Semana» |
| E11 · Auditoría y retención | ✅ Done | `audit_log`, `login_log`, purga programada (RGPD) |
| E12 · Notificaciones por email | ✅ Done | Envío best-effort + `email_outbox` con reintento |
| E13 · Exportaciones | ✅ Done | CSV/XLSX (empleados, solicitudes, auditoría, «mis datos» RGPD) |
| E14 · Design system / App shell | ✅ Done | Rediseño paridad mockups (#82), layout, navegación, sesión |
| E15 · SSO ALEATICA (Fase 2) | ⛔ Bloqueada | `init-auth-sso` a la espera de inputs de ALEATICA (JWT, `/ssocallback`, SLO) |

---

## Backlog activo (pendiente)

### Deuda técnica / hardening (de la auditoría doc↔código)

| ID | Tipo | Prioridad | Título | Estado |
|---|---|---|---|---|
| #87 | task | should | Re-sincronizar documentación con el código (bloque A) | 🔄 In Progress |
| — | task | should | Implementar rate limiting en `POST /auth/login` y `POST /requests` (documentado, no implementado) | Backlog |
| — | task | should | Cablear CORS de backend (`parking.cors.allowed-origins` es config huérfana) | Backlog |
| — | task | could | Búsqueda server-side en la bandeja de solicitudes (hoy client-side sobre la página cargada) | Backlog |
| — | task | could | Bandeja admin: pestañas Aprobadas/Rechazadas/Todas con datos (hoy «no disponible en esta vista») | Backlog |
| — | task | could | Unificar convención de día de la semana (0-6 vs 1-7 en i18n) | Backlog |
| — | task | could | Accesibilidad por teclado del plano interactivo | Backlog |

### Fase 2

| ID | Tipo | Prioridad | Título | Estado |
|---|---|---|---|---|
| — | user-story | wont (Fase 1) | SSO ALEATICA: callback JWT, autorización por `login`, Single Logout | ⛔ Bloqueada |

---

## Convenciones

- **Ramas**: `feat|fix|chore|test|docs/<área>/<nº-issue>-<slug>` (área: backend·frontend·devops·security·tester·docs).
- **Commits**: `tipo(área): descripción (#nº)`; PR con `Closes #nº`.
- **Cierre de ciclo**: al mergear, archivar el change OpenSpec (`/openspec-archive-change <slug>`) y mover el item del Project a `Done`.
