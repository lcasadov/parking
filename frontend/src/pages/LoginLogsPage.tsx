import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import type { UseQueryResult } from '@tanstack/react-query';
import { Button } from '../components/Button';
import { Input } from '../components/Input';
import { EmbeddablePageHeader } from '../components/EmbeddablePageHeader';
import { StatTile } from '../components/StatTile';
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
const COUNT_SIZE = 1;
const OK_RESULT: LoginResult = 'OK';
const DASH = '—';

// KPIs de accesos referidos a la ventana de fechas seleccionada (correctos vs
// fallidos). Consultas de recuento propias (size=1): total y OK; los fallidos se
// derivan. Reflejan solo el rango (no el filtro de resultado), dando una lectura
// de seguridad siempre útil. Subcomponente aislado (S3776).
function LoginStats({ from, to, enabled }: { from: string; to: string; enabled: boolean }) {
  const { t } = useTranslation();
  const base: LoginLogListParams = {
    page: 0,
    size: COUNT_SIZE,
    from: toIsoStart(from),
    to: toIsoEnd(to),
  };
  const totalQuery = useLoginLogsQuery(base, enabled);
  const okQuery = useLoginLogsQuery({ ...base, result: OK_RESULT }, enabled);
  const total = totalQuery.data?.totalElements;
  const ok = okQuery.data?.totalElements;
  if (total === undefined || ok === undefined) {
    return null;
  }
  const failed = Math.max(total - ok, 0);
  const unit = t('loginLogs.stats.unit');
  return (
    <div className="mgmt-stats">
      <StatTile
        dot="var(--ink-faint)"
        icon="login"
        label={t('loginLogs.stats.total')}
        value={total}
        unit={unit}
      />
      <StatTile
        dot="var(--accent)"
        icon="circle-check"
        label={t('loginLogs.stats.ok')}
        value={ok}
        unit={unit}
      />
      <StatTile
        dot="var(--busy)"
        icon="alert-triangle"
        label={t('loginLogs.stats.failed')}
        value={failed}
        unit={unit}
      />
    </div>
  );
}

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
      <div className="table-scroll table-cards-mobile">
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
                <td data-label={t('loginLogs.columns.occurredAt')}>
                  {formatDateTime(entry.occurredAt)}
                </td>
                <td data-label={t('loginLogs.columns.login')}>{entry.loginAttempted}</td>
                <td data-label={t('loginLogs.columns.result')}>
                  <StatusPill tone={entry.result === 'OK' ? 'occupied' : 'free'}>
                    {t(`loginLogs.result.${entry.result}`)}
                  </StatusPill>
                </td>
                <td data-label={t('loginLogs.columns.phase')}>
                  <span className="mono-chip">{t(`loginLogs.phase.${entry.phase}`)}</span>
                </td>
                <td data-label={t('loginLogs.columns.ip')}>{entry.ipAddress ?? DASH}</td>
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
export function LoginLogsPage({ embedded = false }: { embedded?: boolean } = {}) {
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
      <EmbeddablePageHeader
        embedded={embedded}
        eyebrow={t('loginLogs.eyebrow')}
        title={t('loginLogs.title')}
        description={t('loginLogs.description')}
      />

      <p className="form-hint">{t('loginLogs.retentionNote')}</p>

      <LoginStats from={from} to={to} enabled={windowValid} />

      <div className="filter-card">
        <div className="filter-card-head">
          <i className="ti ti-adjustments-horizontal" aria-hidden="true" />
          {t('common.filters')}
        </div>
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
      </div>

      <LoginLogsResults
        query={query}
        windowValid={windowValid}
        page={page}
        onPageChange={setPage}
      />
    </section>
  );
}
