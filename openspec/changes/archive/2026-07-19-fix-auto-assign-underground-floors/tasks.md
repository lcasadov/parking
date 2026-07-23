# Tasks — fix-auto-assign-underground-floors

## 1. Algoritmo
- [x] 1.1 Invertir la preferencia de planta en `RequestService.autoAssignParkingSpace`
      (garaje subterráneo: altos→planta -1/`1xxx`, empleados→planta -5/`5xxx`)
- [x] 1.2 Actualizar Javadoc de `floorPreferenceKey`/método para reflejar la planta física

## 2. Tests
- [x] 2.1 Actualizar tests unit de auto-asignación al nuevo comportamiento
- [x] 2.2 Actualizar ITs de auto-asignación (si los hay) al nuevo comportamiento
      (no hay ITs que aseveren la preferencia de planta por categoría; las ITs AUTOMATIC
      existentes son de puesto/DESK y no dependen de la inversión)
- [x] 2.3 `mvn clean verify` verde (cobertura ≥ umbral, 0 violations nuevas)

## 3. Spec
- [ ] 3.1 Requirement y escenarios de auto-asignación corregidos (delta MODIFIED)
