# Tasks — admin-release-by-employee-week

## 1. Backend
- [x] 1.1 Endpoint listar empleados seleccionables para liberación (ADMIN+AGENCIA), solo lectura
- [x] 1.2 Endpoint ocupación de un empleado por semana (plaza+puesto, origen fija/solicitud, requestId)
- [x] 1.3 Permitir AGENCIA en `POST /requests/{id}/admin-cancel` (hasAnyRole ADMIN, AGENCIA)
- [x] 1.4 Tests unit + IT (ocupación semanal; RBAC ADMIN+AGENCIA; EMPLOYEE 403)

## 2. Frontend
- [x] 2.1 Rediseñar "Nueva liberación" (admin y agencia): selector empleado + navegador de semana
- [x] 2.2 Lista de reservas del empleado por semana (plaza+puesto) con multi-selección (checkbox)
- [x] 2.3 Motivo del lote + "Liberar": bucle por reserva según origen (release admin / admin-cancel)
- [x] 2.4 Feedback de resultado (liberadas / errores parciales); tests frontend

## 3. Verificación
- [x] 3.1 `npm run lint && build && test` verdes (frontend); backend `mvn clean verify` fuera de esta tarea
- [x] 3.2 Delta specs releases (ADDED) + requests (MODIFIED admin-cancel con AGENCIA)
