---
name: project_language_convention
description: parking project — business prose in Spanish, all code identifiers in English; README has the authoritative ES→EN nomenclature table
type: project
---

En el proyecto **parking** (gestión de plazas/puestos ALEATICA) la convención de idioma es:

- **Prosa funcional y de negocio → español** (idioma de ALEATICA y los usuarios).
- **Todo identificador de código → inglés**: entidades, clases, enumerados, campos, columnas y tablas.

La autoridad de nombres es la sección **"Nomenclatura del código (ES → EN)"** del `README.md`. Mapeos clave: `Plaza→ParkingSpace`, `Puesto→Desk`, `Solicitud→Request`, `AsignacionFija→FixedAssignment`, `Liberacion→Release`, `Visitante→Visitor`, `ReservaVisita→VisitorReservation`, `Empleado→Employee`. Enums: `RequestStatus` (PENDING/APPROVED/REJECTED/CANCELLED), `DeskCategory` (STANDARD/EXECUTIVE), `ReleaseType` (VOLUNTARY/ADMINISTRATIVE), `ResourceType` (PARKING/DESK), `Role` (ADMIN/EMPLOYEE).

**No traducir** (contrato externo): claims JWT (`username`, `client_sid`, `iss=SSOTTS`, `aud=parking`), endpoints `/ssocallback` y `/CloseSSOSessionID`, cookie `parking_SESSION`, tabla `SPRING_SESSION`, claves `parking.*`.

**Why:** el usuario pidió explícitamente que todo el modelo de datos/entidades/clases estuviera en inglés, manteniendo la documentación de negocio en español.

**How to apply:** al generar cualquier doc derivado (modelo-datos, openapi, PROJECT.md) o código, usar siempre los nombres en inglés de la tabla del README; redactar explicaciones en español. Verificar la tabla del README antes de inventar nombres, puede haber evolucionado.
