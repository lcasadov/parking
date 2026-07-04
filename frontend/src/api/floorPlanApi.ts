import { apiClient } from './apiClient';
import type {
  DeskPositionUpdate,
  FloorPlanResponse,
  RequestDeskResult,
} from '../types/floorPlan';

// Endpoints de la capability floor-plan (openspec/changes/init-floor-plan).
// Contrato propuesto sobre base /api/v1 (baseURL relativo del apiClient).
const FLOOR_PLAN = '/floor-plan';

// GET /floor-plan?date=YYYY-MM-DD: estado y posición de los puestos para una fecha.
export async function getFloorPlan(date: string): Promise<FloorPlanResponse> {
  const { data } = await apiClient.get<FloorPlanResponse>(FLOOR_PLAN, {
    params: { date },
  });
  return data;
}

// POST /floor-plan/desks/{deskId}/request: solicita un puesto libre para una fecha.
export async function requestDeskFromFloorPlan(
  deskId: number,
  date: string,
): Promise<RequestDeskResult> {
  const { data } = await apiClient.post<RequestDeskResult>(
    `${FLOOR_PLAN}/desks/${deskId}/request`,
    { date },
  );
  return data;
}

// PUT /floor-plan/desks/{deskId}/position: persiste coord_x/coord_y (0-100), solo ADMIN.
export async function updateDeskPosition(
  deskId: number,
  body: DeskPositionUpdate,
): Promise<void> {
  await apiClient.put(`${FLOOR_PLAN}/desks/${deskId}/position`, body);
}
