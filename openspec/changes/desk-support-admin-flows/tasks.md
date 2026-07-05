# Tasks: desk-support-admin-flows

> Cablear la UI admin al soporte de puestos que el backend ya ofrece. Solo frontend; sin cambio de contrato. Verificación visual (mockups 04/05) + prueba en vivo del flujo puesto.

## 1. Bandeja de solicitudes pendientes (admin)
- [x] 1.1 Mostrar el tipo de recurso (Plaza/Puesto) por fila (columna o pill), leyendo `resourceType` de la solicitud.

## 2. Modal de aprobación adaptativo
- [x] 2.1 Leer `resourceType` de la solicitud; título "Solicitud de plaza"/"Solicitud de puesto".
- [x] 2.2 Para `DESK`: cargar puestos disponibles con `GET /availability?date=<requestedDate>&resourceType=DESK`; etiqueta "Puesto disponible". Para `PARKING`: plazas (camino actual).
- [x] 2.3 Enviar el id del recurso elegido en `parkingSpaceId` (transporta el resource_id genérico). Manejar 409 (no disponible) con el mensaje existente.
- [x] 2.4 i18n ES/EN de las etiquetas dependientes del tipo.

## 3. Asignación fija de puesto
- [x] 3.1 Selector de recurso (Plaza/Puesto) en la asignación fija; cambia la lista de recursos.
- [x] 3.2 Enviar `resourceType` en `PUT /fixed-assignments/employee/{id}`; permitir plaza fija y puesto fijo simultáneos.

## 4. Tipo en "Mis solicitudes" (empleado)
- [x] 4.1 Mostrar el tipo de recurso en el listado del empleado.

## 5. Tests + Quality Gate + verificación
- [x] 5.1 Tests (Vitest+RTL): aprobación de puesto ofrece puestos; de plaza ofrece plazas; asignación fija de puesto envía `resourceType`; tipo visible en listados.
- [x] 5.2 `npm run lint && npm test && npm run build` verdes, cobertura ≥80%.
- [ ] 5.3 Verificación en vivo: empleado solicita puesto → admin lo ve como "Puesto" y lo aprueba asignando un puesto (no una plaza). (pendiente: requiere backend en ejecución; cubierto por tests RTL con MSW.)
