// Tipos de la capability floor-plan (openspec/changes/init-floor-plan).
// Contrato propuesto (aún no en docs/openapi.yaml): base /api/v1, cookie de sesión.
import type { DeskCategory } from './desk';

// Estado de un puesto para una fecha, relativo al empleado autenticado.
// FREE (libre) · ASSIGNED (asignado a un tercero) · REQUESTED (solicitud pendiente)
// · MINE (mi puesto) · RELEASED (liberado, disponible para solicitud).
export type DeskState = 'FREE' | 'ASSIGNED' | 'REQUESTED' | 'MINE' | 'RELEASED';

// Un puesto proyectado sobre el plano. coordX/coordY son porcentajes 0-100 del
// ancho/alto de la imagen; null cuando el admin aún no lo ha posicionado.
export interface FloorPlanDesk {
  deskId: number;
  deskNumber: number;
  category: DeskCategory;
  coordX: number | null;
  coordY: number | null;
  state: DeskState;
  // Nombre del titular cuando el puesto está ocupado/asignado (opcional; el
  // contrato base no lo incluye todavía). Cuando llega, el pin de ocupante y el
  // tooltip muestran sus iniciales y su nombre; en su ausencia se degradan a una
  // etiqueta genérica. Puramente informativo: no altera ninguna lógica.
  occupantName?: string | null;
}

// GET /floor-plan?date={ISO} → estado y posición de los 65 puestos para una fecha.
export interface FloorPlanResponse {
  desks: FloorPlanDesk[];
}

// PUT /floor-plan/desks/{deskId}/position → persiste coordenadas (0-100), solo ADMIN.
export interface DeskPositionUpdate {
  coordX: number;
  coordY: number;
}

// POST /floor-plan/desks/{deskId}/request → crea la solicitud del puesto.
export interface RequestDeskResult {
  requestId: number;
  state: DeskState;
}
