# Prompt 1

Rol: Eres un experimentado Product Owner experto en diseño y asignación del árbol de Epics-->Features-->Stories-->Task/Bugs

Objetivo: Analiza toda la documentación disponible @prompts/descripcion/descripcion.md y @readme.md y, basándote sólo y exclusivamente en esta información, genera las historias de usuario (Identifica las  más importantes) considerando diferentes tipos de usuarios y sus necesidades.
Genera de la misma manera los criterios de priorización que consideras más importantes

Enfócate en funcionalidades core que permitan validar el concepto de la aplicación.

Usa INVEST como checklist para evaluar la calidad de una user story antes de que una story entre al sprint y Acceptance Criteria: el formato Given/When/Then usando Behavior-Driven Development (BDD).

Ten en cuenta que vamos a usar GitHub Projects + Copilot para toda la gestión de tareas y sprints.

Outputs a generar: actualiza el @readme.md genera la documentación relativa al punto 6 Historias de usuario e incluye los diagramas  necesarios en formato Mermaid

No inventes lo que no sepas. Si necesitas solucionar dudas, pregúntame antes de realizar nada.

# Respuesta Prompt 1
Secciones 7 y 8 generadas. El README pasa de **6.892 → 8.700 líneas**. Aquí el resumen de lo entregado:

---

### ✅ Sección 7 — Tickets de trabajo

| Apartado | Contenido |
|---|---|
| **7.1** | Estructura de tickets (tipos, campos GitHub Issue, template) |
| **7.2** | Flujo Kanban GitHub Projects + Definición de Done global |
| **7.3** | **25 tickets detallados** distribuidos en 5 sprints — cada uno con contexto, checklist técnico y criterios de aceptación |
| **7.4** | Diagrama de dependencias por módulo (Mermaid) |
| **7.5** | Tabla resumen: todos los tickets con módulo, sprint, US relacionada y prioridad |

Tipos de tickets generados: `CHORE-001/002/003`, `FEAT-001…021`, `TEST-001` → trazabilidad completa US ↔ Ticket.

---

### ✅ Sección 8 — Pull Requests

| Apartado | Contenido |
|---|---|
| **8.1** | Estrategia de ramas: `main` / `develop` / `feature/*` con reglas de protección |
| **8.2** | Convención Conventional Commits con ejemplos reales del proyecto |
| **8.3** | Template `.github/pull_request_template.md` listo para copiar |
| **8.4** | Proceso de revisión con diagrama de secuencia Mermaid |
| **8.5** | Reglas de revisión: arquitectura hexagonal, seguridad, rendimiento y calidad de tests |
| **8.6** | Pipeline CI completo en YAML (`ci.yml`) con jobs `backend`, `frontend` y `e2e` |
| **8.7** | Ejemplos de PRs reales: Feature, Fix y Chore |
| **8.8** | Métricas de calidad objetivo (JaCoCo ≥ 80%, Playwright < 5 min, etc.) |

# Prompt 2

Rol: Eres un experimentado Product Owner experto en diseño y asignación del árbol de Epics-->Features-->Stories-->Task/Bugs

Objetivo: Analiza toda la documentación disponible @prompts/descripcion/descripcion.md, @readme.md y backlog.md y, basándote sólo y exclusivamente en esta información, estimar y priorizar cada historia de usuario en base a los Criterios de Priorización Impacto en el usuario y valor del negocio, Urgencia basada en tendencias del mercado y feedback de usuarios, Complejidad y esfuerzo estimado de implementación, riesgos y dependencias entre tareas:
- Story points según complejidad técnica
- Dependencias entre historias
- Riesgos técnicos identificados
- Orden de implementación recomendado
- REvisa los criterios de aceptación y explicalos. Si consideras que falta algo incluyelo.


Outputs a generar: actualiza el @backlog.md genera la documentación relativa al punto 1 Historias de Usuario e incluye los subpuntos que consideres necesarios los diagramas  necesarios en formato Mermaid. Centrate en el punto 1 exclusivamente.

No inventes lo que no sepas. Si necesitas solucionar dudas, pregúntame antes de realizar nada.

# Respuesta Prompt 2
Todo correcto. El resultado final del `backlog.md`:

---

**Sección 1 reescrita — resumen de cambios:**

| Nuevo | Qué se añadió |
|---|---|
| **1.2 Escala SP** | Tabla Fibonacci 1–13 + 4 **anchor stories** con justificación explícita |
| **1.4 Dependencias** | Diagrama Mermaid `graph TD` con 22 aristas de bloqueo + colores MUST/SHOULD/COULD |
| **1.5 Riesgos** | Tabla de 8 riesgos con nivel 🔴/🟠/🟡 y mitigación concreta por historia |
| **1.7 Orden implementación** | Tabla sprint a sprint con justificación del POR QUÉ de ese orden |

**Historias mejoradas (criterios de aceptación):**

| Historia | Qué faltaba y se añadió |
|---|---|
| **Todas** | `**Estimación: X SP — [justificación]**` en cada historia |
| **Todas** | INVEST table donde faltaba (US-005, 010, 012, 013, 015, 018, 020, 021, 023, 024, 025) |
| **US-005** | Scenario: desactivar usuario con reservas activas |
| **US-012** | Scenario: navegación entre semanas; mis reservas destacadas en otro color |
| **US-013** | Scenario: paginación del historial |
| **US-015** | 3 nuevos scenarios: filtrar pendientes, filtrar por fechas, reintentar pago REJECTED |
| **US-018** | Scenario: OTP incorrecto → contador de intentos |
| **US-020** | Scenario: bot edita mensaje existente (no publica uno nuevo) al unirse participante |
| **US-021** | Scenarios: desactivar desde icono, paginación de tabla |
| **US-022** | Envío de contraseña temporal también por **email** (estaba solo Telegram) |
| **US-023** | Scenarios: gráficas semanal/mensual/anual explícitas; carga < 3s |
| **US-024** | Scenarios: precio negativo rechazado; plazo 0h no permitido |
| **US-025** | Scenarios: participantes notificados cuando titular cancela; admin notificado al crear reserva |

# Prompt 3

Rol: Eres un experimentado Tech Lead  experto en gestión de tareas.

Objetivo: A partir de las historias de usuario creadas en @backlog.md, revisa los tickets de trabajo para que se cumplan estas especificaciones:

Da todo el detalle requerido para desarrollar la tarea de inicio a fin teniendo en cuenta las buenas prácticas al respecto.

Cada ticket debe incluir: story point de referencia, descripción técnica, que agente lo debe desarrollar (front, back o devops, tester, infraestrutura,etc..), criterios de aceptación, definición de hecho, estimación en story points.

Outputs a generar: actualiza el @backlog.md genera la documentación relativa al punto 2 Tickets de trabajo modifica los que consideres necesarios. Centrate en el punto 2 exclusivamente.

# Respuesta Prompt 3
La Sección 2 está completa. Aquí el resumen de lo que se generó:

---

**25 tickets reescritos** (TICKET-001 a TICKET-025), todos con el nuevo formato completo:

| Campo nuevo | Descripción |
|---|---|
| **Tabla de metadatos** | US relacionada · SP referencia · **Agente** · **SP estimados** · Sprint · Rama · Labels |
| **Descripción técnica** | 2-4 frases explicando qué construir, qué capas afecta y por qué, con referencias a clases concretas |
| **Tareas técnicas** | Checklist detallado de inicio a fin |
| **Criterios de aceptación** | Checkboxes verificables, no frases genéricas |
| **Definición de hecho (DoD)** | Adaptada por tipo de ticket |

---

**Agentes asignados por tipo de trabajo:**

| Agente | Tickets |
|---|---|
| **Infra/DevOps** | TICKET-001 (scaffold), 002 (DB schema), 025 (observabilidad) |
| **Backend** | TICKET-003, 004, 007, 008, 010, 011, 013, 014, 023 |
| **Frontend** | TICKET-005 (calendario) |
| **Full-stack** | TICKET-006, 009, 012, 015, 016, 017, 018, 019, 020, 021, 022 |
| **Tester** | TICKET-024 (E2E flujos críticos) |

---

**DoD diferenciada por tipo:**
- **Backend**: cobertura ≥ 80%, tests `@SpringBootTest`+`MockMvc`, OpenAPI actualizada, auditoría si modifica datos
- **Frontend**: Vitest + RTL ≥ 80%, responsive 375px/1280px, sin console.error en flujo nominal
- **Full-stack**: ambos conjuntos anteriores
- **Infra/DevOps**: sin secrets hardcoded, documentación actualizada, CI verde
- **Tester**: Docker aislado, job `e2e` en CI, report Playwright como artefacto

# Prompt 4
Excelente. Ahora en cada ticket:

Revisa o mejora  las tareas específicas paso a paso
- Tecnologías y librerías exactas a usar
- Archivos y estructura de código a crear
- Tests requeridos con ejemplos
- Criterios de performance y calidad
- Que un desarrollador junior pueda seguir las instrucciones sin bloqueos.
- Dependencias entre tickets y orden de ejecución
- Puntos de integración entre frontend/backend/database
- Criterios de revisión de código
- Proceso de testing conjunto
- Definition of Done para cada ticket

# Respuesta Prompt 4
Voy a leer algunos tickets representativos para entender el estado actual antes de planificar la mejora.

Tengo el diagnóstico claro. Lo que falta en cada ticket:

Rutas exactas de ficheros y paquetes
Versiones de librerías
Ejemplos de código de tests
Criterios de performance medibles
Orden numerado paso a paso
Lanzo el agente para reescribir la Sección 2 completa con todo ese detalle.
El `backlog.md` está completo con las 3 secciones. El trabajo de la sesión anterior está finalizado. El archivo tiene 5,495 líneas con:
- **Sección 1**: 25 historias de usuario con INVEST, BDD, mapa de dependencias, riesgos, roadmap
- **Sección 2**: 25 tickets con formato Tech Lead + 4 subsecciones junior-friendly
- **Sección 3**: Estrategia de Pull Requests

Dime cómo quieres continuar. Las siguientes opciones lógicas serían:
1. **Generar el código base** del proyecto (scaffolding del TICKET-001)
2. **Revisar/mejorar la Sección 3** de Pull Requests con el mismo nivel de detalle junior-friendly
3. **Otro prompt de planificación** (arquitectura, ADRs, etc.)