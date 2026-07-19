# fix-auto-assign-underground-floors

## Why
La auto-asignación de plaza en modo AUTOMATIC invierte la preferencia de planta.
El aparcamiento es un GARAJE BAJO TIERRA: la plaza `1xxx` está en la planta **-1**
(la más alta, cercana a la superficie) y la `5xxx` en la planta **-5** (la más baja/
profunda). El algoritmo calcula `planta = número/1000` y trata el número mayor (5xxx)
como "planta más alta", por lo que asigna a los cargos altos la planta -5 (la peor) y
a los empleados la -1 (la mejor) — al revés de lo debido. El spec y sus escenarios
comparten el mismo error de interpretación.

## What Changes
- Se corrige `RequestService.autoAssignParkingSpace` para que la preferencia de planta
  respete la planta física subterránea: cargos altos → planta más alta (planta -1, `1xxx`);
  resto de empleados → planta más baja (planta -5, `5xxx`). Dentro de la planta, menor número.
- Se corrigen el requirement y los escenarios del spec `requests` (auto-asignación).
- Se actualizan los tests (unit + IT) al comportamiento correcto.

## Impact
- Solo lógica de auto-asignación de PLAZA en modo AUTOMATIC. No afecta a PUESTO, ni a la
  aprobación/rechazo manual, ni a otros flujos.
- Affected specs: `requests` (requirement de auto-asignación por categoría y planta).
- Affected code: `request/application/RequestService.java` + sus tests.

## Out of scope
- Cambios de UI/presentación (van en la rama del rediseño).
- Cambiar el criterio dentro de la planta (se mantiene menor número).
