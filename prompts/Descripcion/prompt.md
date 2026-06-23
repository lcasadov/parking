# Misión

Genera el documento `docs/PROJECT.md` como(Product Requirements Document) para
parking. Es un PRD **ejecutivo y corto** orientado a stakeholders no
técnicos (sponsor, RRHH, IT manager). El detalle funcional fino vivirá
en OpenSpec — aquí solo el "qué" y el "por qué", nunca el "cómo".

# Entradas

- `README.md` adjunto (descripción funcional completa).


# Contrato del documento

- Longitud objetivo: **300-500 líneas máximo**.
- Idioma: español.
- Sin código, sin DDL, sin endpoints específicos.
- Foco en outcomes y métricas, no en implementación.
- Lenguaje accesible: si introduces un término técnico, defínelo o
  remite al glosario del README.

# Secciones obligatorias

## 1. Resumen ejecutivo
3-5 líneas: qué es parking, para quién, qué problema resuelve, en qué
estado está (Fase 1 con login local, Fase 2 con SSO ALEATICA).

## 2. Problema y motivación
- Situación actual (gestión informal / hoja de cálculo / lo que sea).
- Dolores concretos: conflictos por plazas, falta de visibilidad,
  imposibilidad de planificar.
- Coste de no hacer nada.

## 3. Visión del producto
- Frase de visión en una línea.
- Estado objetivo: cómo se ve el día a día cuando parking funcione.

## 4. Stakeholders y usuarios

| Rol | Quién | Qué necesita de parking |
|---|---|---|
| Sponsor | _[pendiente]_ | Visibilidad de uso, ROI |
| Admin de parking | RRHH / Servicios generales | Configurar plazas, asignar y resolver solicitudes |
| Empleado | Plantilla ALEATICA | Solicitar/liberar plaza fácilmente |
| Empleado no corporativo | Plantilla otras localizaciones | Mismo flujo que empleado |
| Visitante | Externo | No accede; lo gestiona el admin |
| Equipo SSO ALEATICA | IT | Coordinación Fase 2 |

## 5. Alcance del MVP
Lista numerada de capacidades del MVP — extraídas de las características
del README, agrupadas en bullets ejecutivos (no por endpoint):

- Gestión de empleados (alta/baja/edición/reset password).
- Configuración del parking (plazas).
- Asignación fija indefinida por día de la semana.
- Solicitud puntual de plaza con flujo aprobación/rechazo.
- Liberación voluntaria y administrativa.
- Reservas para visitantes externos.
- Notificaciones por email en eventos clave.
- Auditoría y exportación.
- Internacionalización ES/EN y modo oscuro.

## 6. Fuera de alcance del MVP
- Aplicación móvil nativa.
- Métricas analíticas avanzadas (dashboards de ocupación, BI).
- Calendario laboral con festivos.
- SMS / push notifications.
- Integraciones con sistemas externos (RRHH, control de acceso físico).
- Sistema de cola/prioridades automáticas para aprobación.

## 7. Fases del producto

| Fase | Contenido | Estado |
|---|---|---|
| Fase 1 | Funcionalidad completa con login local | 🟢 En desarrollo |
| Fase 2 | Migración a SSO ALEATICA + fallback de emergencia | 🔵 Pendiente |
| Roadmap futuro | Métricas, calendario laboral, móvil nativo | 🟡 No planificado |

## 8. Métricas de éxito

Indicadores cuantitativos para evaluar si parking está cumpliendo su
propósito:

- **% solicitudes resueltas en menos de 24 horas**: ≥ 90%.
- **% ocupación efectiva de plazas vs configuradas** (mide eficiencia
  del sistema de liberación).
- **Tiempo medio del admin gestionando solicitudes**: < 10 min/día.
- **Errores reportados por usuarios**: < 5/mes tras estabilización.
- **Adopción**: > 80% de empleados con plaza fija usando la app para
  liberar en lugar de no comunicar.

## 9. Restricciones y supuestos

**Restricciones**:
- Tecnológicas (stack obligatorio): Java 22 + Spring Boot 3.3, React 18,
  SQL Server 2022, Tomcat 10.1.
- Despliegue corporativo (no cloud externo).
- Cumplimiento RGPD obligatorio.
- Repositorio inicialmente en GitHub, migración a Azure DevOps.
- Reviewer único de PRs hasta que se amplíe el equipo.

**Supuestos**:
- ALEATICA proveerá la clave de firma del JWT antes de Fase 2.
- El número total de empleados con derecho a plaza es manejable
  (estimación: < 500).
- El número de plazas físicas es estable y conocido.

## 10. Riesgos

| Riesgo | Probabilidad | Impacto | Mitigación |
|---|---|---|---|
| Retraso de ALEATICA con datos SSO | Media | Alto | Fase 1 funciona autónoma; Fase 2 se desbloquea cuando llegue |
| Cambios en política RGPD durante desarrollo | Baja | Medio | Auditoría y retención ya contempladas |
| Adopción baja entre empleados | Media | Alto | UX simple y onboarding asistido por el admin |
| Concurrencia: dos solicitudes para la misma plaza | Alta | Bajo | Constraints de BD + 409 Conflict |

## 11. Cumplimiento normativo

- **RGPD**: datos personales tratados (nombre, apellidos, email,
  teléfono, matrícula, DNI de visitantes). Retención 2 años de datos
  históricos. Derecho de acceso y supresión gestionado vía admin.
- **Auditoría**: todas las acciones del admin se registran en
  `AUDIT_LOG`. Logins en `LOGIN_LOG`.

## 12. Dependencias externas

- Landing ALEATICA (Fase 2).
- SSO WS ALEATICA (`consultaporlogin` — spec pendiente).
- SMTP corporativo (Fase 2; en Fase 1 usamos Ethereal).
- Infraestructura SQL Server 2022 corporativa.
- Tomcat 10.1 en servidores corporativos.

## 13. Pendientes
Lista numerada de información que dejaste sin completar.

# Restricciones de generación

- No inventes nombres de sponsor, no inventes cifras de plantilla, no
  inventes plazos.
- Si no hay información, escribe `_[pendiente]_` y añádelo a la sección
  13.
- Sin emojis fuera de los previstos (🟢 🔵 🟡 en la tabla de fases).
