# PROJECT.md — Documento de Requisitos de Producto (PRD)

> **Producto:** parking — Gestión de Plazas de Parking y Puestos de Oficina ALEATICA
> **Tipo de documento:** PRD ejecutivo (el "qué" y el "por qué").
> **Audiencia:** sponsor, RRHH, IT manager y demás stakeholders no técnicos.
> **Alcance de este documento:** visión, problema, alcance, métricas y riesgos. El detalle funcional fino (reglas, modelo, flujos) vive en el `README.md` y en las specs de OpenSpec. Aquí no se describe el "cómo".
> **Idioma:** español. Los términos técnicos se definen al introducirse o remiten al glosario del `README.md`.

---

## 1. Resumen ejecutivo

parking es una aplicación web corporativa que permite a los empleados de ALEATICA solicitar, liberar y consultar **recursos reservables** —plazas de parking y puestos de oficina— para fechas concretas, y a uno o varios administradores configurarlos, asignarlos de forma fija y resolver las solicitudes. Resuelve la gestión manual y conflictiva de un recurso escaso (plazas/puestos) aportando reglas claras, visibilidad y trazabilidad.

El producto se entrega en **dos fases de autenticación**: la **Fase 1** (en desarrollo) funciona de forma autónoma con login local propio; la **Fase 2** (pendiente) integra el inicio de sesión con la landing corporativa de ALEATICA (SSO), manteniendo un mecanismo de acceso de contingencia.

---

## 2. Problema y motivación

### Situación actual
La asignación de plazas y puestos se gestiona hoy de forma **informal**, sin un sistema único: combinación de acuerdos verbales, correos sueltos y, presumiblemente, una hoja de cálculo mantenida a mano. _La forma exacta del proceso actual está pendiente de confirmar con RRHH/Servicios generales (ver sección 13)._

### Dolores concretos
- **Conflictos por plazas:** dos personas creen tener derecho al mismo recurso el mismo día, sin un árbitro claro.
- **Falta de visibilidad:** nadie sabe en tiempo real qué plazas/puestos están libres un día concreto, por lo que quedan recursos infrautilizados mientras otros empleados se quedan sin sitio.
- **Imposibilidad de planificar:** el empleado no puede anticipar si tendrá plaza, y el administrador no tiene una cola ordenada de peticiones que resolver.
- **Cero trazabilidad:** no hay registro de quién pidió, quién aprobó ni por qué se rechazó algo, lo que dificulta resolver disputas y cumplir obligaciones de auditoría.
- **Liberación no comunicada:** cuando un titular de plaza fija no acude, su plaza queda "bloqueada" porque nadie sabe que está libre.

### Coste de no hacer nada
- Recurso corporativo **infrautilizado** (plazas vacías que no se reasignan) frente a empleados sin sitio.
- Tiempo de gestión recurrente del administrador en tareas manuales y mediación de conflictos.
- Fricción y percepción de **arbitrariedad** entre la plantilla.
- Exposición ante auditoría y RGPD por ausencia de registro estructurado de decisiones.

---

## 3. Visión del producto

> **Visión:** que cada plaza y cada puesto disponible encuentre a quien lo necesita, de forma justa, transparente y sin esfuerzo de gestión.

### Estado objetivo (el día a día cuando parking funcione)
- El **empleado** entra desde el móvil o el navegador, ve qué hay libre para el día que necesita, lo solicita en segundos y libera su recurso fijo cuando no va a usarlo.
- El **administrador** ve una cola ordenada de solicitudes pendientes, aprueba o rechaza con un clic, configura plazas y puestos, y dispone de un histórico completo y exportable.
- La **organización** dispone de un recurso mejor aprovechado, decisiones trazables y cumplimiento de auditoría/RGPD por diseño.

---

## 4. Stakeholders y usuarios

| Rol | Quién | Qué necesita de parking |
|---|---|---|
| Sponsor | _[pendiente]_ | Visibilidad de uso y retorno de la inversión |
| Admin de parking | RRHH / Servicios generales | Configurar plazas y puestos, asignar fijos y resolver solicitudes |
| Empleado | Plantilla ALEATICA | Solicitar/liberar plaza o puesto fácilmente |
| Empleado no corporativo | Plantilla de otras localizaciones (sin EntraID) | Mismo flujo que el empleado |
| Visitante | Externo a ALEATICA | No accede a la aplicación; su reserva la gestiona el admin |
| Equipo SSO ALEATICA | IT corporativo | Coordinación e integración de la Fase 2 |

> **Nota:** "EntraID" es el servicio de identidad corporativa de Microsoft usado por ALEATICA para autenticar a los empleados corporativos. Los empleados de localizaciones sin EntraID se autenticarán con credenciales locales de la landing en Fase 2.

---

## 5. Alcance del MVP

Capacidades que entran en la primera versión funcional completa (Fase 1), descritas como outcomes ejecutivos:

1. **Gestión de empleados:** alta, edición, baja lógica, reactivación y reset de contraseña.
2. **Configuración del parking:** alta individual y configuración masiva del número total de plazas.
3. **Configuración de puestos de oficina:** gestión de los 65 puestos y su categoría (estándar / dirección), con plano interactivo. _(Alcance ampliado; ver sección 7.)_
4. **Asignación fija** indefinida de un recurso a un empleado por día(s) de la semana, revocable o modificable en cualquier momento.
5. **Solicitud puntual** de plaza y/o puesto para una fecha concreta, con flujo de **aprobación / rechazo** por el administrador.
6. **Liberación voluntaria** (por el titular) y **administrativa** (por el admin ante inasistencia) de recursos fijos.
7. **Reservas para visitantes externos**, con ficha de visitante reutilizable gestionada por el admin.
8. **Notificaciones por email** en los eventos clave (nueva solicitud, aprobación, rechazo, revocación de asignación).
9. **Auditoría y exportación:** registro de todas las acciones relevantes y exportación de históricos a CSV/XLSX.
10. **Vistas de disponibilidad:** calendario semanal completo para el admin y vista personal "Mi Semana" para el empleado.
11. **Internacionalización ES/EN y modo oscuro** desde el inicio.

> El MVP nace centrado en **plazas de parking**; los **puestos de oficina** son un alcance ampliado que reutiliza las mismas reglas (ver sección 7 y el roadmap del `README.md`).

---

## 6. Fuera de alcance del MVP

- Aplicación **móvil nativa** (la web es responsive, mobile-first; sin apps de tienda).
- **Métricas analíticas avanzadas:** dashboards de ocupación, business intelligence, informes de tendencia.
- **Calendario laboral** con festivos (se puede solicitar cualquier día del año).
- **SMS / notificaciones push** (solo email).
- **Integraciones con sistemas externos** (RRHH, control de acceso físico, barreras de parking).
- **Sistema de cola o prioridades automáticas** para la aprobación (el admin decide; el orden FIFO es solo informativo).
- **Provisioning automático de usuarios** desde el directorio corporativo (el alta de empleados es siempre manual).

---

## 7. Fases del producto

| Fase | Contenido | Estado |
|---|---|---|
| Fase 1 | Funcionalidad completa con login local propio | 🟢 En desarrollo |
| Fase 2 | Migración a SSO ALEATICA + fallback de emergencia | 🔵 Pendiente |
| Roadmap futuro | Métricas analíticas, calendario laboral, móvil nativo | 🟡 No planificado |

### Detalle de fases
- **Fase 1 — Login local:** parking gestiona sus propias credenciales (usuario y contraseña) de forma temporal. Permite poner el producto en producción sin depender de la integración corporativa. Incluye toda la funcionalidad de negocio (plazas, puestos, solicitudes, liberaciones, visitantes, notificaciones, auditoría).
- **Fase 2 — SSO ALEATICA:** parking se publica detrás de la landing corporativa, que pasa a **autenticar** al usuario; parking solo **autoriza** (comprueba que el empleado existe y está activo, y determina su rol). Se conserva un **fallback de emergencia**: capacidad latente de login local, desactivada por defecto, activable solo si la landing no está disponible.
- **Roadmap futuro:** capacidades no comprometidas que podrían abordarse una vez estabilizado el producto.

> **Alcance de puestos de oficina:** el plano interactivo y la gestión de los 65 puestos se incorporan como una ampliación que generaliza el modelo de plazas. No tiene fase de autenticación propia: aplica a ambas fases.

---

## 8. Métricas de éxito

Indicadores cuantitativos para evaluar si parking cumple su propósito (los valores objetivo se revisarán tras un periodo de estabilización):

- **Solicitudes resueltas en menos de 24 horas:** ≥ 90%.
- **Ocupación efectiva de recursos vs configurados:** mide la eficiencia del sistema de liberación (objetivo a fijar con el sponsor). Un valor alto indica que los recursos liberados se reaprovechan.
- **Tiempo medio del admin gestionando solicitudes:** < 10 minutos/día.
- **Errores reportados por usuarios:** < 5/mes tras la estabilización.
- **Adopción de la liberación:** > 80% de los empleados con recurso fijo usan la app para liberar cuando no acuden, en lugar de no comunicarlo.

> El objetivo numérico de la métrica de ocupación queda **_[pendiente]_** de acordar con el sponsor (ver sección 13).

---

## 9. Restricciones y supuestos

### Restricciones
- **Stack tecnológico obligatorio:** Java 21 LTS + Spring Boot 3.3 (backend), React 18 (frontend), SQL Server 2022 (base de datos), Tomcat 10.1 (servidor de aplicaciones).
- **Despliegue corporativo:** infraestructura propia de ALEATICA, no cloud público externo.
- **Cumplimiento RGPD** obligatorio (ver sección 11).
- **Repositorio:** inicialmente en GitHub, con migración prevista a Azure DevOps al cerrar la fase de arranque.
- **Revisión de cambios:** reviewer único de las Pull Requests hasta que se amplíe el equipo.
- **Idioma de usuario:** soporte obligatorio español e inglés.

### Supuestos
- ALEATICA **proveerá la clave de firma del JWT** y las URLs definitivas antes de iniciar la Fase 2.
- El número total de empleados con derecho a recurso es **manejable** (estimación de partida: menos de 500, **sin confirmar** — ver sección 13).
- El número de plazas físicas y de puestos (65) es **estable y conocido**.
- La entrega de credenciales temporales al empleado en Fase 1 se hace por canal directo (no por email).

---

## 10. Riesgos

| Riesgo | Probabilidad | Impacto | Mitigación |
|---|---|---|---|
| Retraso de ALEATICA con los datos del SSO | Media | Alto | La Fase 1 funciona de forma autónoma; la Fase 2 se desbloquea cuando lleguen los datos |
| Cambios en la política RGPD durante el desarrollo | Baja | Medio | Auditoría y retención ya contempladas por diseño |
| Adopción baja entre los empleados | Media | Alto | UX simple, mobile-first y onboarding asistido por el admin |
| Concurrencia: dos solicitudes para el mismo recurso y fecha | Alta | Bajo | Restricciones de unicidad en base de datos + respuesta de conflicto controlada |
| Proceso actual no documentado dificulta la migración inicial de datos | Media | Medio | Levantar el inventario real de plazas/asignaciones con RRHH antes del arranque |

---

## 11. Cumplimiento normativo

- **RGPD:** parking trata datos personales de empleados (nombre, apellidos, email, teléfono móvil, matrícula) y de visitantes (nombre, apellidos, documento de identidad, matrícula).
  - **Retención:** los datos históricos (solicitudes cerradas, liberaciones, reservas de visitante y registros de auditoría) se purgan automáticamente a los **2 años**. Los datos de entidades vivas no se purgan mientras estén activas.
  - **Derechos de acceso y supresión:** gestionados a través del administrador (baja lógica de empleados, anulación de reservas, etc.).
  - **Minimización:** solo se recogen los datos necesarios para la asignación y el contacto.
- **Auditoría:** todas las acciones relevantes del administrador y del empleado quedan registradas en el registro de auditoría (`audit_log`). Los intentos de inicio de sesión (correctos y fallidos) se registran de forma separada en el registro de logins (`login_log`) para no contaminar la auditoría funcional.

> Estos dos registros son tablas internas de la aplicación; aquí se mencionan solo para evidenciar la trazabilidad. Su estructura técnica se detalla fuera de este documento.

---

## 12. Dependencias externas

- **Landing ALEATICA:** portal corporativo que autenticará a los usuarios en la Fase 2.
- **Servicio SSO de ALEATICA** (`consultaporlogin`): web service de consulta cuya especificación exacta está **pendiente** de recibir.
- **Correo (SMTP):** en LOCAL, DES y PRE se usa **Ethereal** (buzón de pruebas que no entrega correos reales); en PRO, **SMTP corporativo** de ALEATICA.
- **Infraestructura SQL Server 2022** corporativa.
- **Servidores Tomcat 10.1** corporativos para el despliegue.

---

## 13. Arquitectura de alto nivel (resumen)

> Resumen ejecutivo. El diseño detallado y los diagramas (contexto, contenedores, componentes, secuencia y despliegue, en formato Mermaid) están en `docs/architecture.md`.

parking se construye como un **monolito modular** con frontend **SPA React 18** y backend **Java 21 LTS + Spring Boot 3.3** empaquetado como **WAR sobre Tomcat 10.1**, con **SQL Server 2022** para datos, sesiones y auditoría. El cliente y el servidor se comunican por **REST sobre HTTPS** con sesión server-side por cookie.

- **Estilo:** monolito modular cuyos módulos coinciden 1:1 con el roadmap funcional (auth, employees, parking, assignments, requests, releases, availability, visitors, desks/floor-plan, notifications, audit, export). Cada módulo aplica **arquitectura hexagonal (puertos y adaptadores)**, separando el núcleo de dominio de la infraestructura (ver `docs/architecture.md`, secciones 3 y 6).
- **Autenticación:** Fase 1 login local; Fase 2 SSO delegado en la landing ALEATICA (parking solo autoriza). Sesión con Spring Session JDBC para permitir Single Logout.
- **Concurrencia:** las colisiones (p. ej. aprobar la misma plaza/fecha) se resuelven en la base de datos mediante índices de unicidad parciales, devolviendo un conflicto controlado.
- **Despliegue:** on-premise corporativo; Tomcat sirve la SPA y la API en el mismo origen (sin reverse proxy dedicado) sobre SQL Server, entornos DES/PRE/PRO.

**Valoración del stack:** adecuado para un entorno corporativo on-premise con SQL Server y Tomcat ya impuestos como estándar; se recomienda **mantenerlo**. El backend fija **Java 21 LTS** por soporte a largo plazo. El análisis completo de ventajas/desventajas y alternativas está en `docs/architecture.md`, sección 15.

---

## 14. Pendientes

Información no disponible al redactar este PRD, a completar con los stakeholders:

1. **Sponsor ejecutivo:** identidad y expectativas concretas de retorno.
2. **Proceso actual de gestión de plazas/puestos:** cómo se hace hoy exactamente (hoja de cálculo, correos, acuerdos verbales) para dimensionar la migración inicial de datos.
3. **Cifra real de plantilla** con derecho a recurso (la estimación de < 500 no está confirmada).
4. **Número total de plazas físicas** a configurar.
5. **Objetivo numérico** de la métrica de ocupación efectiva.
6. **Clave/secreto de firma del JWT** para la Fase 2.
7. **URLs definitivas** de los entornos PRE y PRO de la landing (Fase 2).
8. **Especificación del servicio** `consultaporlogin` (Fase 2).
9. **Plazos/fechas objetivo** de cada fase (no definidos).
10. **Mockups de UI** que proporcionará ALEATICA antes de la implementación de pantallas.

---

## Anexo A — Configuración técnica (para agentes y orquestación)

> Metadatos operativos consumidos por `CLAUDE.md` y los agentes (`backend-architect`, `frontend-engineer`, `api-tester`, `database-optimizer`, `verification-specialist`, `reality-checker`, etc.). No forma parte del PRD ejecutivo; vive aquí porque toda la cadena de agentes lee `docs/PROJECT.md`.

### Repositorio y ramas
| Clave | Valor |
|---|---|
| `REPO_ROOT` | `c:\proyectos\parking` |
| `BASE_BRANCH` | `develop` |
| `GITHUB_REMOTE` | `https://github.com/lcasadov/parking.git` |
| `GITHUB_ORG` | `lcasadov` (cuenta de usuario, no organización) |
| `GITHUB_REPO` | `parking` |
| `GITHUB_PROJECT_NUMBER` | `_[pendiente]_` (nº del Project v2, si existe) |

### OpenSpec y documentación
| Clave | Valor |
|---|---|
| `OPENSPEC_PATH` | `openspec/` |
| `OPENSPEC_API_PATH` | `docs/openapi.yaml` |
| Modelo de datos | `docs/data-model.md` |
| Diseño de seguridad | `docs/security-design.md` |
| Arquitectura | `docs/architecture.md` |
| Estrategia de testing | `docs/TESTING-STRATEGY.md` |
| Estándares de código | `docs/SONAR-STANDARDS.md` |
| Catálogo de pantallas / flujos | `docs/ui-screens.md`, `docs/ux-flows.md` |

### Estructura de código
| Clave | Valor |
|---|---|
| `BACKEND_DIR` | `backend/` |
| `FRONTEND_DIR` | `frontend/` |
| `DATABASE_DIR` | `database/` (seeds y scripts) |

### Stack y comandos
| Aspecto | Valor |
|---|---|
| Backend | Java 21 LTS · Spring Boot 3.3 · WAR sobre Tomcat 10.1 |
| ORM / persistencia | Spring Data JPA · Hibernate 6.5 · Flyway 10 |
| Motor de BD | Microsoft SQL Server 2022 |
| Frontend | React 18 · Vite · Vitest + RTL · react-i18next · TanStack Query + Context |
| E2E | Playwright |
| Build backend | `mvn clean verify` (tests + JaCoCo) · `mvn spring-boot:run -Dspring-boot.run.profiles=des` · `mvn clean package -Pprod` |
| Build frontend | `npm run lint && npm test && npm run build` |
| Entornos / perfiles | `des`, `pre`, `pro` |

### API y seguridad
| Aspecto | Valor |
|---|---|
| Base URL local | `http://localhost:8080/parking-api/api/v1` |
| Base URL PRO | `https://parking.aleatica.com/parking-api/api/v1` |
| Endpoints SSO (fuera de `/api/v1`) | `/parking-api/ssocallback`, `/parking-api/CloseSSOSessionID` |
| Autenticación | Cookie de sesión `parking_SESSION` (Spring Session JDBC); Fase 1 local, Fase 2 SSO |
| Roles | `ADMIN`, `EMPLOYEE` |
| Forma de error | `ApiError { error, message, fields, timestamp }` |
| CORS | Solo en DES (`http://localhost:5173`); en PRE/PRO mismo origen (sin CORS) |

### Identidad git
Todas las operaciones `git`/`gh` se hacen con el usuario **`lcasadov`** (propietario de `lcasadov/parking`, ya autenticado en `gh` vía keyring). **No se usa bot.** ⚠️ Para Projects v2 (`gh project`/GraphQL) el token de `lcasadov` necesita el scope `project`: si falta, `gh auth refresh -s project`.

### Agentes disponibles
El **orquestador es la sesión principal** (guiada por `CLAUDE.md`), no un subagente. Subagentes especializados en `.claude/agents/` (12): `backend-architect` · `frontend-engineer` · `devops-engineer` · `tester-tdd` · `test-strategist` · `test-runner` · `verification-specialist` · `reality-checker` · `api-tester` · `security-auditor` · `database-optimizer` · `gh-projects-sync`.

> El repositorio ya existe (`lcasadov/parking`). ⚠️ Pendiente solo `GITHUB_PROJECT_NUMBER` (nº del Project v2 en GitHub); hasta tenerlo, `gh-projects-sync` puede gestionar Issues pero no el board de Projects.
