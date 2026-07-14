// Tipos derivados del contrato docs/openapi.yaml (SystemSettings / ApprovalMode).

// ApprovalMode: schema #/components/schemas/ApprovalMode. Modo global de aprobacion
// de solicitudes. MANUAL -> la solicitud nace PENDING; AUTOMATIC -> nace APPROVED.
export type ApprovalMode = 'MANUAL' | 'AUTOMATIC';

// SystemSettings: schema #/components/schemas/SystemSettings.
export interface SystemSettings {
  approvalMode: ApprovalMode;
  updatedById?: number | null;
  updatedAt?: string | null;
}

// UpdateApprovalModeRequest: schema #/components/schemas/UpdateApprovalModeRequest.
export interface UpdateApprovalModeRequest {
  approvalMode: ApprovalMode;
}
