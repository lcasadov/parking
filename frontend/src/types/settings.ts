// Tipos derivados del contrato docs/openapi.yaml (SystemSettings / ApprovalMode).

// ApprovalMode: schema #/components/schemas/ApprovalMode. Modo global de aprobacion
// de solicitudes. MANUAL -> la solicitud nace PENDING; AUTOMATIC -> nace APPROVED.
export type ApprovalMode = 'MANUAL' | 'AUTOMATIC';

// SystemSettings: schema #/components/schemas/SystemSettings.
export interface SystemSettings {
  approvalMode: ApprovalMode;
  // Dirección del parking configurada por el admin: destino del botón "Ir al
  // parking" (Google Maps) en Mi Semana. Opcional/nullable (puede no estar puesta).
  parkingAddress?: string | null;
  // Coordenadas del punto exacto del parking fijado en el mapa (Mapbox). Si están
  // presentes, "Ir al parking" navega a ellas; si no, cae a la dirección postal.
  parkingLat?: number | null;
  parkingLng?: number | null;
  // Si se admiten reservas en fin de semana (sábado/domingo). Por defecto false:
  // el empleado no puede reservar esos días y se ocultan sus tarjetas.
  weekendReservable?: boolean;
  // Interruptores globales de canal de notificación (change push-notifications).
  emailNotificationsEnabled?: boolean;
  pushNotificationsEnabled?: boolean;
  updatedById?: number | null;
  updatedAt?: string | null;
}

// UpdateApprovalModeRequest: schema #/components/schemas/UpdateApprovalModeRequest.
export interface UpdateApprovalModeRequest {
  approvalMode: ApprovalMode;
}

// Ubicación del parking legible por cualquier empleado (GET /settings/parking-address):
// dirección postal + coordenadas opcionales del punto exacto fijado en el mapa.
export interface ParkingLocation {
  address: string | null;
  lat: number | null;
  lng: number | null;
}
