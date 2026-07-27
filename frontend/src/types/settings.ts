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
  // Si se admiten reservas en fin de semana (sábado/domingo). Por defecto false:
  // el empleado no puede reservar esos días y se ocultan sus tarjetas.
  weekendReservable?: boolean;
  updatedById?: number | null;
  updatedAt?: string | null;
}

// UpdateApprovalModeRequest: schema #/components/schemas/UpdateApprovalModeRequest.
export interface UpdateApprovalModeRequest {
  approvalMode: ApprovalMode;
}
