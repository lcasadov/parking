import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import type { UseQueryResult } from '@tanstack/react-query';
import { Button } from '../components/Button';
import { ExportMenu } from '../components/ExportMenu';
import { Input } from '../components/Input';
import { Spinner } from '../components/Spinner';
import { getStatus } from '../api/apiError';
import { EXPORT_PATHS } from '../api/exportApi';
import { useAuditQuery } from '../hooks/useAudit';
import type { AuditListParams, AuditLogEntry, PageAuditLogEntry } from '../types/audit';
import { auditPillClass, formatDateTime, isValidWindow, toIsoEnd, toIsoStart } from '../utils/audit';

const PAGE_SIZE = 20;
const DASH = '—';

// Etiqueta de la entidad afectada (tipo + id opcional del contrato, entityId nullable).
function entityLabel(entry: AuditLogEntry): string {
  const hasId = entry.entityId !== null && entry.entityId !== undefined;
  return hasId ? `${entry.entityType} #${entry.entityId}` : entry.entityType;
}

interface ResultsProps {
  query: UseQueryResult<PageAuditLogEntry>;
  windowValid: boolean;
  page: number;
  onPageChange: (next: number) => void;
}

// Zona de resultados: error de ventana / spinner / error de carga / tabla / paginacion.
function AuditResults({ query, windowValid, page, onPageChange }: ResultsProps) {
  const { t } = useTranslation();
  const entries = query.data?.content ?? [];
  const totalPages = query.data?.totalPages ?? 0;
  const isWindowError = !windowValid || getStatus(query.error) === 400;

  if (isWindowError) {
    return (
      <p className="form-error" role="alert">
        {t('audit.errors.window')}
      </p>
    );
  }
  if (query.isLoading) {
    return <Spinner />;
  }
  if (query.isError) {
    return (
      <p className="form-error" role="alert">
        {t('audit.loadError')}
      </p>
    );
  }

  return (
    <>
      <div className="table-scroll">
        <table className="table">
          <thead>
            <tr className="table-header">
              <th scope="col">{t('audit.columns.occurredAt')}</th>
              <th scope="col">{t('audit.columns.actor')}</th>
              <th scope="col">{t('audit.columns.action')}</th>
              <th scope="col">{t('audit.columns.entity')}</th>
              <th scope="col">{t('audit.columns.details')}</th>
            </tr>
          </thead>
          <tbody>
            {entries.length === 0 ? (
              <tr>
                <td colSpan={5} className="table-empty">
                  {t('audit.empty')}
                </td>
              </tr>
            ) : (
              entries.map((entry) => (
                <tr key={entry.id} className="table-row">
                  <td>{formatDateTime(entry.occurredAt)}</td>
                  <td>{entry.actorEmployeeId ?? t('audit.systemActor')}</td>
                  <td>
                    <span className={`pill ${auditPillClass(entry.action)}`}>{entry.action}</span>
                  </td>
                  <td>{entityLabel(entry)}</td>
                  <td>{entry.details ?? DASH}</td>
                </tr>
              ))
            )}
          </tbody>
        </table>
      </div>

      {totalPages > 1 ? (
        <nav className="pagination" aria-label={t('audit.title')}>
          <Button
            variant="white"
            disabled={query.data?.first ?? true}
            onClick={() => onPageChange(page - 1)}
          >
            {t('audit.pagination.previous')}
          </Button>
          <span className="pagination-info">
            {t('audit.pagination.pageInfo', { page: page + 1, total: totalPages })}
          </span>
          <Button
            variant="white"
            disabled={query.data?.last ?? true}
            onClick={() => onPageChange(page + 1)}
          >
            {t('audit.pagination.next')}
          </Button>
        </nav>
      ) : null}
    </>
  );
}

// Panel ADMIN de consulta de auditoria funcional (GET /audit). tasks §4.1.
export function AuditPage() {
  const { t } = useTranslation();
  const [actorId, setActorId] = useState('');
  const [action, setAction] = useState('');
  const [from, setFrom] = useState('');
  const [to, setTo] = useState('');
  const [page, setPage] = useState(0);

  const windowValid = isValidWindow(from, to);

  const params: AuditListParams = {
    page,
    size: PAGE_SIZE,
    actorEmployeeId: actorId ? Number(actorId) : undefined,
    action: action || undefined,
    from: toIsoStart(from),
    to: toIsoEnd(to),
  };

  const query = useAuditQuery(params, windowValid);

  function onFilterChange(setter: (value: string) => void, value: string): void {
    setter(value);
    setPage(0);
  }

  return (
    <section className="audit-page" aria-labelledby="audit-title">
      <header className="page-header">
        <h1 id="audit-title" className="section-title">
          {t('audit.title')}
        </h1>
        <div className="page-actions">
          <ExportMenu path={EXPORT_PATHS.audit} fallbackBase="audit" requiredRole="ADMIN" />
        </div>
      </header>

      <p className="form-hint">{t('audit.retentionNote')}</p>

      <div className="toolbar audit-filters" role="search">
        <Input
          id="audit-actor"
          type="number"
          min={1}
          label={t('audit.filters.actor')}
          value={actorId}
          onChange={(event) => onFilterChange(setActorId, event.target.value)}
        />
        <Input
          id="audit-action"
          type="text"
          label={t('audit.filters.action')}
          value={action}
          onChange={(event) => onFilterChange(setAction, event.target.value)}
        />
        <Input
          id="audit-from"
          type="date"
          label={t('audit.filters.from')}
          value={from}
          onChange={(event) => onFilterChange(setFrom, event.target.value)}
        />
        <Input
          id="audit-to"
          type="date"
          label={t('audit.filters.to')}
          value={to}
          onChange={(event) => onFilterChange(setTo, event.target.value)}
        />
      </div>

      <AuditResults query={query} windowValid={windowValid} page={page} onPageChange={setPage} />
    </section>
  );
}
