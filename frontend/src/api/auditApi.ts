import { apiClient } from './apiClient';
import type {
  AuditListParams,
  LoginLogListParams,
  PageAuditLogEntry,
  PageLoginLogEntry,
} from '../types/audit';

// Endpoints de Audit / LoginLog segun docs/openapi.yaml. baseURL relativo del apiClient.

const AUDIT = '/audit';
const LOGIN_LOGS = '/login-logs';

// Serializa los parametros de /audit omitiendo los indefinidos/vacios.
function buildAuditParams(params: AuditListParams): Record<string, string | number> {
  const query: Record<string, string | number> = {};
  if (params.page !== undefined) {
    query.page = params.page;
  }
  if (params.size !== undefined) {
    query.size = params.size;
  }
  if (params.actorEmployeeId !== undefined) {
    query.actorEmployeeId = params.actorEmployeeId;
  }
  if (params.action !== undefined && params.action !== '') {
    query.action = params.action;
  }
  if (params.from !== undefined && params.from !== '') {
    query.from = params.from;
  }
  if (params.to !== undefined && params.to !== '') {
    query.to = params.to;
  }
  return query;
}

// Serializa los parametros de /login-logs omitiendo los indefinidos/vacios.
function buildLoginLogParams(params: LoginLogListParams): Record<string, string | number> {
  const query: Record<string, string | number> = {};
  if (params.page !== undefined) {
    query.page = params.page;
  }
  if (params.size !== undefined) {
    query.size = params.size;
  }
  if (params.result !== undefined) {
    query.result = params.result;
  }
  if (params.from !== undefined && params.from !== '') {
    query.from = params.from;
  }
  if (params.to !== undefined && params.to !== '') {
    query.to = params.to;
  }
  return query;
}

export async function listAudit(params: AuditListParams = {}): Promise<PageAuditLogEntry> {
  const { data } = await apiClient.get<PageAuditLogEntry>(AUDIT, {
    params: buildAuditParams(params),
  });
  return data;
}

export async function listLoginLogs(
  params: LoginLogListParams = {},
): Promise<PageLoginLogEntry> {
  const { data } = await apiClient.get<PageLoginLogEntry>(LOGIN_LOGS, {
    params: buildLoginLogParams(params),
  });
  return data;
}
