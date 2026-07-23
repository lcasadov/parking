# Tasks — release-occupied-resource

## 1. Backend — cancelación administrativa de solicitud aprobada
- [x] 1.1 Endpoint `POST /requests/{id}/admin-cancel` (solo ADMIN) con motivo obligatorio
- [x] 1.2 `RequestService.adminCancel(id, adminLogin, reason)`: exige APPROVED + fecha futura;
      transiciona a CANCELLED, libera el recurso, audita (reutiliza la liberación por cancelación)
- [x] 1.3 Errores: 404 si no existe; 409/400 si no es APPROVED o es pasada; RBAC 403 a EMPLOYEE
- [x] 1.4 Tests unit + IT (admin cancela APPROVED futura → libera; estados inválidos; RBAC)

## 2. Frontend — empleado (Mi semana)
- [x] 2.1 "Liberar" del día: cancela la solicitud propia del día (APPROVED futura / PENDING) o,
      si el recurso es de asignación fija, crea Release. Habilitado cuando hay recurso liberable
- [x] 2.2 Soportar PUESTO además de plaza en el modal/acción de liberar
      (la ruta de cancelación es agnóstica al tipo de recurso; my-week es PARKING en backend)
- [x] 2.3 Tests frontend

## 3. Frontend — admin (Liberar por fecha)
- [x] 3.1 "Liberar" sobre recurso ocupado por solicitud → `POST /requests/{id}/admin-cancel` (motivo)
- [x] 3.2 Corregir etiqueta "Plaza"→"Recurso" (muestra plaza o puesto) en el modal admin
- [x] 3.3 Tests frontend

## 4. Verificación
- [x] 4.1 `mvn clean verify` + `npm run lint && build && test` verdes
- [x] 4.2 Delta MODIFIED/ADDED del spec requests
