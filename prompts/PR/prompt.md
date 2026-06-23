# Prompt 1

Rol: Eres un experimentado DevOps Engineer con experiencia en Git workflows

Objetivo: Analiza toda la documentación disponible @backlog.md y especialmente el punto 3 y, basándote en esta información, Actualiza lo necesario respecto a:

Template estándar de PR con checklist
Proceso de review y aprobación
Criterios de merge (tests, coverage, approvals)
Estrategia de branching para el equipo
Automatización con GitHub Actions
Asegúrate de que mantenga calidad de código y facilite colaboración.

Outputs a generar: actualiza el @backlog.md genera la documentación relativa al punto 3 Pull Requests e incluye los diagramas necesarios en formato Mermaid.
Ten en cuenta que la forma de trabajo será con un orquestador de IA que creará el branch que se requiera y lanzar las tareas a los agentes necesarios para llevar a cabo la tarea. Este orquestador tendrá su token para poder hacer commit de las ramas y PR´s. Pero el merge y la revisión se tiene que llevar a cabo por otro usuario.

No inventes lo que no sepas. Si necesitas solucionar dudas, pregúntame antes de realizar nada.

# Prompt 1 Preguntas

Antes de actualizar nada, necesito resolver 5 dudas concretas que cambian el diseño del workflow:

---

**1. Usuarios humanos revisores**
¿Cuántas personas humanas van a hacer review y merge? ¿Una sola (tú), o habrá varios miembros de equipo con roles distintos (ej: tech lead + dev)?
Respuesta 1: Una Yo
---

**2. Cuenta del orquestador de IA en GitHub**
¿El orquestador usará una **GitHub App** (recomendado) o una **cuenta bot con PAT** (Personal Access Token)? Esto afecta cómo se configura el token en Actions y si los commits aparecen como `bot` o como un usuario normal.
Respuesta 2: Usamos un token de una cuenta orquestadoria de Github, al que se le invita al proyecto
---

**3. Permisos del orquestador**
¿El orquestador puede hacer **push a `develop` directamente** para commits intermedios, o **solo puede trabajar en feature branches** y crear PRs hacia `develop`? (La segunda opción es más segura.)
Respuesta 3: El orquestado no puede hacer push a develop. Solo puede trabajar en feature branches y crear una PR hacia develop. 
---

**4. PRs en estado Draft**
¿Quieres que los PRs creados por el orquestador nazcan como **Draft** (borrador) y solo pasen a "Ready for Review" cuando el agente haya terminado todo el trabajo del ticket? Así el revisor humano no ve PRs incompletos.
Respuesta 4 : Si
---

**5. Entorno de staging**
La documentación menciona un staging auto-deploy desde `develop`. ¿Existe realmente ese entorno en vuestra infraestructura, o es On-Premise sin staging dedicado? Afecta qué pasos incluir en el pipeline post-merge.
Respuesta 5: la rama develop se despliega en el entorno de desarrollo. Cuando se genere una versión para Staging se generará una realease una vez validada esa release se mergea en main que es la rama que pasa a producción

# Prompt 1 Respuesta

Todo correcto. La Sección 3 pasa de ~330 líneas a **1.272 líneas** (de la 5945 a la 7217). Aquí el resumen de lo que hay en cada punto:

---

### Qué contiene la nueva Sección 3

**3.1 Modelo de Branching**
Diagrama `gitGraph` con el flujo completo `develop → feature → develop → release → main (tag)`. Tabla con quién puede crear cada rama, a dónde puede hacer push el orquestador (solo feature branches, nunca develop/main) y qué deploy dispara cada rama.

**3.2 Ciclo de vida de un Ticket**
`sequenceDiagram` con los 4 participantes: **ORC** (orquestador) → **AGT** (agente IA) → **CI** (GitHub Actions) → **DEV** (tú). Incluye los loops de CI fallido y de cambios solicitados en review. El PR nace como Draft y solo pasa a Ready for Review cuando CI está verde y el agente ha terminado.

**3.3 Proceso de Release**
`gitGraph` del flujo `release/v1.0.0 → main (tag) + → develop`. Tabla de 14 pasos indicando en cada uno si lo ejecuta el orquestador o tú.

**3.4 Convención de Commits y Ramas**
Regla especial para trazabilidad: los commits de agentes incluyen `Agent: TICKET-XXX` en el footer. Ejemplos reales con scopes del proyecto (`reservas`, `otp`, `redsys`, `ci`).

**3.5 Template de Pull Request**
Fichero `.github/pull_request_template.md` completo, con checkbox explícito de *"PR generado por IA / por humano"* y sección de **Notas para el revisor** (decisiones técnicas, zonas de riesgo).

**3.6 Criterios de Merge**
12 gates automáticos con herramienta, umbral y si son bloqueantes (E2E excluido del gate de PR). Gates humanos referenciando las secciones 2.8 y 2.10 del propio backlog.

**3.7 Branch Protection**
Tablas de configuración exacta para `main` y `develop` en GitHub. Fichero `/CODEOWNERS` completo con reglas específicas para migraciones, security config y workflows de CI.

**3.8 GitHub Actions — 5 workflows completos**
- `ci-pr.yml` — lint + tests + cobertura en cada PR (paralelo back/front)
- `deploy-dev.yml` — build Docker + SSH deploy a DEV tras merge a develop
- `deploy-staging.yml` — idem para staging desde `release/*`
- `deploy-prod.yml` — producción con `environment: production` (aprobación manual en GitHub Environments) + creación de tag automático
- `pr-checks.yml` — 4 jobs: validación título Conventional Commits, referencia a ticket, auto-assign revisor cuando el PR sale de Draft, auto-labels por tipo

**3.9 Ejemplos de PRs reales**
TICKET-007 (el más complejo, núcleo del sistema), FIX OTP race condition, y PR de release v1.0.0.

**3.10 Métricas y Acuerdos del equipo**
10 métricas con herramienta y frecuencia. 10 acuerdos explícitos sobre trabajo humano+IA: el orquestador no puede aprobar ni mergear sus propios PRs, límite de 500 líneas de diff, prohibición de `any` sin justificación, etc.