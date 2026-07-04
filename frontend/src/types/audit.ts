// Tipos derivados del contrato docs/openapi.yaml (Audit / LoginLog).

// LoginResult: schema #/components/schemas/LoginResult.
export type LoginResult = 'OK' | 'INVALID_CREDENTIALS' | 'LOCKED' | 'INACTIVE' | 'NO_ACCESS' | 'FALLBACK_OK';

// LoginPhase: schema #/components/schemas/LoginPhase.
export type LoginPhase = 'PHASE_1' | 'PHASE_2' | 'FALLBACK';

// Valores del enum LoginResult para poblar el filtro (orden del contrato).
export const LOGIN_RESULTS: readonly LoginResult[] = [
  'OK',
  'INVALID_CREDENTIALS',
  'LOCKED',
  'INACTIVE',
  'NO_ACCESS',
  'FALLBACK_OK',
];

// AuditLogEntry: schema #/components/schemas/AuditLogEntry.
export interface AuditLogEntry {
  id: number;
  actorEmployeeId?: number | null;
  action: string;
  entityType: string;
  entityId?: number | null;
  details?: string | null;
  occurredAt: string;
}

// LoginLogEntry: schema #/components/schemas/LoginLogEntry.
export interface LoginLogEntry {
  id: number;
  loginAttempted: string;
  employeeId?: number | null;
  result: LoginResult;
  phase: LoginPhase;
  ipAddress?: string | null;
  userAgent?: string | null;
  occurredAt: string;
}

// PageAuditLogEntry: schema #/components/schemas/PageAuditLogEntry (PageMeta + content).
export interface PageAuditLogEntry {
  content: AuditLogEntry[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
  first: boolean;
  last: boolean;
}

// PageLoginLogEntry: schema #/components/schemas/PageLoginLogEntry.
export interface PageLoginLogEntry {
  content: LoginLogEntry[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
  first: boolean;
  last: boolean;
}

// Parametros de listado de /audit: PageParam, SizeParam, actorEmployeeId, action, from, to.
export interface AuditListParams {
  page?: number;
  size?: number;
  actorEmployeeId?: number;
  action?: string;
  from?: string;
  to?: string;
}

// Parametros de listado de /login-logs: PageParam, SizeParam, result, from, to.
export interface LoginLogListParams {
  page?: number;
  size?: number;
  result?: LoginResult;
  from?: string;
  to?: string;
}
