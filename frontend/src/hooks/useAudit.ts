import { useQuery, type UseQueryResult } from '@tanstack/react-query';
import { listAudit, listLoginLogs } from '../api/auditApi';
import type {
  AuditListParams,
  LoginLogListParams,
  PageAuditLogEntry,
  PageLoginLogEntry,
} from '../types/audit';

// Claves raiz de cache (S1192: sin literales repetidos).
const AUDIT_KEY = 'audit';
const LOGIN_LOGS_KEY = 'login-logs';

export function auditQueryKey(params: AuditListParams): (string | AuditListParams)[] {
  return [AUDIT_KEY, params];
}

export function loginLogsQueryKey(
  params: LoginLogListParams,
): (string | LoginLogListParams)[] {
  return [LOGIN_LOGS_KEY, params];
}

export function useAuditQuery(
  params: AuditListParams,
  enabled = true,
): UseQueryResult<PageAuditLogEntry> {
  return useQuery({
    queryKey: auditQueryKey(params),
    queryFn: () => listAudit(params),
    placeholderData: (previous) => previous,
    enabled,
  });
}

export function useLoginLogsQuery(
  params: LoginLogListParams,
  enabled = true,
): UseQueryResult<PageLoginLogEntry> {
  return useQuery({
    queryKey: loginLogsQueryKey(params),
    queryFn: () => listLoginLogs(params),
    placeholderData: (previous) => previous,
    enabled,
  });
}
