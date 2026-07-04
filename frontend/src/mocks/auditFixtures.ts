import type {
  AuditLogEntry,
  LoginLogEntry,
  PageAuditLogEntry,
  PageLoginLogEntry,
} from '../types/audit';

// Entradas de auditoria de ejemplo (contrato #/components/schemas/AuditLogEntry).
export const auditEmployeeCreated: AuditLogEntry = {
  id: 1,
  actorEmployeeId: 1,
  action: 'EMPLOYEE_CREATED',
  entityType: 'Employee',
  entityId: 10,
  details: '{"login":"aandersson"}',
  occurredAt: '2026-03-01T09:15:00Z',
};

export const auditRequestApproved: AuditLogEntry = {
  id: 2,
  actorEmployeeId: 1,
  action: 'REQUEST_APPROVED',
  entityType: 'Request',
  entityId: 55,
  details: '{"parkingSpaceId":3}',
  occurredAt: '2026-03-02T11:30:00Z',
};

// Accion del sistema: actor nulo.
export const auditRetentionPurge: AuditLogEntry = {
  id: 3,
  actorEmployeeId: null,
  action: 'RETENTION_PURGE',
  entityType: 'AuditLog',
  entityId: null,
  details: null,
  occurredAt: '2026-03-03T02:00:00Z',
};

export function auditPageOf(content: AuditLogEntry[]): PageAuditLogEntry {
  return {
    content,
    totalElements: content.length,
    totalPages: 1,
    size: 20,
    number: 0,
    first: true,
    last: true,
  };
}

export const defaultAuditPage: PageAuditLogEntry = auditPageOf([
  auditEmployeeCreated,
  auditRequestApproved,
  auditRetentionPurge,
]);

// Entradas de login de ejemplo (contrato #/components/schemas/LoginLogEntry).
export const loginOk: LoginLogEntry = {
  id: 1,
  loginAttempted: 'admin',
  employeeId: 1,
  result: 'OK',
  phase: 'PHASE_1',
  ipAddress: '10.0.0.1',
  userAgent: 'Mozilla/5.0',
  occurredAt: '2026-03-01T08:00:00Z',
};

export const loginInvalid: LoginLogEntry = {
  id: 2,
  loginAttempted: 'hacker',
  employeeId: null,
  result: 'INVALID_CREDENTIALS',
  phase: 'PHASE_1',
  ipAddress: '203.0.113.9',
  userAgent: 'curl/8.0',
  occurredAt: '2026-03-01T08:05:00Z',
};

export const loginInactive: LoginLogEntry = {
  id: 3,
  loginAttempted: 'olduser',
  employeeId: 2,
  result: 'INACTIVE',
  phase: 'PHASE_1',
  ipAddress: null,
  userAgent: null,
  occurredAt: '2026-03-01T08:10:00Z',
};

export function loginLogPageOf(content: LoginLogEntry[]): PageLoginLogEntry {
  return {
    content,
    totalElements: content.length,
    totalPages: 1,
    size: 20,
    number: 0,
    first: true,
    last: true,
  };
}

export const defaultLoginLogPage: PageLoginLogEntry = loginLogPageOf([
  loginOk,
  loginInvalid,
  loginInactive,
]);
