import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import type { UseQueryResult } from '@tanstack/react-query';
import { Button } from '../components/Button';
import { Input } from '../components/Input';
import { PageHeader } from '../components/PageHeader';
import { StatusPill } from '../components/StatusPill';
import { TableEmpty, TableError, TableSkeleton } from '../components/TableStates';
import { Toolbar } from '../components/Toolbar';
import { getStatus } from '../api/apiError';
import { useLoginLogsQuery } from '../hooks/useAudit';
import {
  LOGIN_RESULTS,
  type LoginLogListParams,
  type LoginResult,
  type PageLoginLogEntry,
} from '../types/audit';
import { formatDateTime, isValidWindow, toIsoEnd, toIsoStart } from '../utils/audit';

const PAGE_SIZE = 20;
const DASH = '—';

interface ResultsProps {
  query: UseQueryResult<PageLoginLogEntry>;
  windowValid: boolean;
  page: number;
  onPageChange: (next: number) => void;
}

// Zona de resultados: error de ventana / spinner / error de carga / tabla / paginacion.
function LoginLogsResults({ query, windowValid, page, onPageChange }: ResultsProps) {
  const { t } = useTranslation();
  const entries = query.data?.content ?? [];
  const totalPages = query.data?.totalPages ?? 0;
  const isWindowError = !windowValid || getStatus(query.error) === 400;

  if (isWindowError) {
    return (
      <p className="form-error" role="alert">
        {t('loginLogs.errors.window')}
      </p>
    );
  }
  if (query.isLoading) {
    return <TableSkeleton label={t('common.loading')} columns={5} />;
  }
  if (query.isError) {
    return (
      <TableError
        message={t('loginLogs.loadError')}
        retryLabel={t('common.retry')}
        onRetry={() => void query.refetch()}
      />
    );
  }
  if (entries.length === 0) {
    return <TableEmpty icon="login" message={t('loginLogs.empty')} />;
  }

  return (
    <>
      <div className="table-scroll">
        <table className="table">
          <thead>
            <tr className="table-header">
              <th scope="col">{t('loginLogs.columns.occurredAt')}</th>
              <th scope="col">{t('loginLogs.columns.login')}</th>
              <th scope="col">{t('loginLogs.columns.result')}</th>
              <th scope="col">{t('loginLogs.columns.phase')}</th>
              <th scope="col">{t('loginLogs.columns.ip')}</th>
            </tr>
          </thead>
          <tbody>
            {entries.map((entry) => (
              <tr key={entry.id} className="table-row">
                <td>{formatDateTime(entry.occurredAt)}</td>
                <td>{entry.loginAttempted}</td>
                <td>
                  <StatusPill tone={entry.result === 'OK' ? 'occupied' : 'free'}>
                    {t(`loginLogs.result.${entry.result}`)}
                  </StatusPill>
                </td>
                <td>{t(`loginLogs.phase.${entry.phase}`)}</td>
                <td>{entry.ipAddress ?? DASH}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      {totalPages > 1 ? (
        <nav className="pagination" aria-label={t('loginLogs.title')}>
          <Button
            variant="white"
            disabled={query.data?.first ?? true}
            onClick={() => onPageChange(page - 1)}
          >
            {t('loginLogs.pagination.previous')}
          </Button>
          <span className="pagination-info">
            {t('loginLogs.pagination.pageInfo', { page: page + 1, total: totalPages })}
          </span>
          <Button
            variant="white"
            disabled={query.data?.last ?? true}
            onClick={() => onPageChange(page + 1)}
          >
            {t('loginLogs.pagination.next')}
          </Button>
        </nav>
      ) : null}
    </>
  );
}

// Panel ADMIN de consulta de logs de login (GET /login-logs). tasks §4.2.
export function LoginLogsPage() {
  const { t } = useTranslation();
  const [result, setResult] = useState<LoginResult | ''>('');
  const [from, setFrom] = useState('');
  const [to, setTo] = useState('');
  const [page, setPage] = useState(0);

  const windowValid = isValidWindow(from, to);

  const params: LoginLogListParams = {
    page,
    size: PAGE_SIZE,
    result: result || undefined,
    from: toIsoStart(from),
    to: toIsoEnd(to),
  };

  const query = useLoginLogsQuery(params, windowValid);

  function onFilterChange(setter: (value: string) => void, value: string): void {
    setter(value);
    setPage(0);
  }

  return (
    <section className="login-logs-page" aria-label={t('loginLogs.title')}>
      <PageHeader
        eyebrow={t('loginLogs.eyebrow')}
        title={t('loginLogs.title')}
        description={t('loginLogs.description')}
      />

      <p className="form-hint">{t('loginLogs.retentionNote')}</p>

      <Toolbar ariaLabel={t('loginLogs.title')}>
        <div className="auth-field">
          <label className="field-label" htmlFor="login-logs-result">
            {t('loginLogs.filters.result')}
          </label>
          <select
            id="login-logs-result"
            className="field-input"
            value={result}
            onChange={(event) =>
              onFilterChange((v) => setResult(v as LoginResult | ''), event.target.value)
            }
          >
            <option value="">{t('loginLogs.filters.allResults')}</option>
            {LOGIN_RESULTS.map((value) => (
              <option key={value} value={value}>
                {t(`loginLogs.result.${value}`)}
              </option>
            ))}
          </select>
        </div>
        <Input
          id="login-logs-from"
          type="date"
          label={t('loginLogs.filters.from')}
          value={from}
          onChange={(event) => onFilterChange(setFrom, event.target.value)}
        />
        <Input
          id="login-logs-to"
          type="date"
          label={t('loginLogs.filters.to')}
          value={to}
          onChange={(event) => onFilterChange(setTo, event.target.value)}
        />
      </Toolbar>

      <LoginLogsResults
        query={query}
        windowValid={windowValid}
        page={page}
        onPageChange={setPage}
      />
    </section>
  );
}
