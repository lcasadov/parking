import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { Button } from '../components/Button';
import { DeskCategoryBadge } from '../components/DeskCategoryBadge';
import { DeskFormModal } from '../components/DeskFormModal';
import { Legend } from '../components/Legend';
import { PageHeader } from '../components/PageHeader';
import { SearchBox } from '../components/SearchBox';
import { StatTile } from '../components/StatTile';
import { StatusPill } from '../components/StatusPill';
import { TableEmpty, TableError, TableSkeleton } from '../components/TableStates';
import { Toolbar } from '../components/Toolbar';
import { emitApiErrorToast } from '../api/events';
import { useDesksQuery, useSetDeskActivation } from '../hooks/useDesks';
import type { Desk } from '../types/desk';

const PAGE_SIZE = 20;
const COUNT_SIZE = 1;

// Fila de KPIs de inventario de puestos (total / activos / inactivos). Consultas
// de recuento propias (size=1) sobre el mismo endpoint, reflejan el inventario
// GLOBAL con independencia del filtro visible. Subcomponente aislado (S3776).
function DeskStats() {
  const { t } = useTranslation();
  const totalQuery = useDesksQuery({ page: 0, size: COUNT_SIZE });
  const activeQuery = useDesksQuery({ page: 0, size: COUNT_SIZE, active: true });
  const total = totalQuery.data?.totalElements;
  const active = activeQuery.data?.totalElements;
  if (total === undefined || active === undefined) {
    return null;
  }
  const inactive = Math.max(total - active, 0);
  const unit = t('desks.stats.unit');
  return (
    <div className="mgmt-stats">
      <StatTile
        dot="var(--ink-faint)"
        icon="armchair"
        label={t('desks.stats.total')}
        value={total}
        unit={unit}
      />
      <StatTile
        dot="var(--accent)"
        icon="circle-check"
        label={t('desks.stats.active')}
        value={active}
        unit={unit}
        sub={t('desks.stats.ofTotal', { total })}
      />
      <StatTile
        dot="var(--rel)"
        icon="circle-off"
        label={t('desks.stats.inactive')}
        value={inactive}
        unit={unit}
      />
    </div>
  );
}

type ActiveFilter = 'all' | 'active' | 'inactive';

// Traduce el filtro de la UI al parámetro `active` del contrato.
function filterToActive(filter: ActiveFilter): boolean | undefined {
  if (filter === 'active') {
    return true;
  }
  if (filter === 'inactive') {
    return false;
  }
  return undefined;
}

// Vista de gestión de puestos (ADMIN): tabla paginada con filtro activo/inactivo,
// alta/edición y activación/desactivación. Distingue EXECUTIVE visualmente
// (init-desks §4.1/§4.3). El plano interactivo llega en floor-plan.
export function DesksPage() {
  const { t } = useTranslation();
  const [filter, setFilter] = useState<ActiveFilter>('all');
  const [q, setQ] = useState('');
  const [page, setPage] = useState(0);
  const [formDesk, setFormDesk] = useState<Desk | null>(null);
  const [isFormOpen, setIsFormOpen] = useState(false);

  const query = useDesksQuery({
    page,
    size: PAGE_SIZE,
    active: filterToActive(filter),
  });
  const activationMutation = useSetDeskActivation();

  function handleFilter(value: ActiveFilter): void {
    setFilter(value);
    setPage(0);
  }

  function openCreate(): void {
    setFormDesk(null);
    setIsFormOpen(true);
  }

  function openEdit(desk: Desk): void {
    setFormDesk(desk);
    setIsFormOpen(true);
  }

  function closeForm(): void {
    setIsFormOpen(false);
    setFormDesk(null);
  }

  function toggleActivation(desk: Desk): void {
    // La activación usa su endpoint dedicado (PATCH /desks/{id}/activation); el de
    // actualización (PUT) NO modifica `active` (bug #83).
    activationMutation.mutate(
      { id: desk.id, active: !desk.active },
      { onError: () => emitApiErrorToast('desks.errors.toggle') },
    );
  }

  const desks = query.data?.content ?? [];
  // Busqueda local por numero: el contrato de /desks no expone `q`, asi que el
  // filtro actua sobre la pagina cargada.
  const search = q.trim();
  const visibleDesks = search
    ? desks.filter((desk) => String(desk.number).includes(search))
    : desks;
  const ready = !query.isLoading && !query.isError;
  const totalPages = query.data?.totalPages ?? 0;
  const isFirst = query.data?.first ?? true;
  const isLast = query.data?.last ?? true;

  return (
    <section className="desks-page" aria-label={t('desks.title')}>
      <PageHeader
        eyebrow={t('desks.eyebrow')}
        title={t('desks.title')}
        description={t('desks.description')}
        actions={
          <Button variant="green" icon="plus" onClick={openCreate}>
            {t('desks.new')}
          </Button>
        }
      />

      <DeskStats />

      <div className="filter-card">
        <div className="filter-card-head">
          <i className="ti ti-adjustments-horizontal" aria-hidden="true" />
          {t('common.filters')}
        </div>
        <Toolbar ariaLabel={t('desks.searchLabel')}>
          <SearchBox
            label={t('desks.searchLabel')}
            placeholder={t('desks.searchPlaceholder')}
            value={q}
            onValueChange={setQ}
          />
          <label className="field-label" htmlFor="desks-filter">
            {t('desks.filterLabel')}
          </label>
          <select
            id="desks-filter"
            className="field-input"
            value={filter}
            onChange={(event) => handleFilter(event.target.value as ActiveFilter)}
          >
            <option value="all">{t('desks.filter.all')}</option>
            <option value="active">{t('desks.filter.active')}</option>
            <option value="inactive">{t('desks.filter.inactive')}</option>
          </select>
        </Toolbar>
      </div>

      {query.isLoading ? <TableSkeleton label={t('common.loading')} columns={4} /> : null}

      {query.isError ? (
        <TableError
          message={t('desks.loadError')}
          retryLabel={t('common.retry')}
          onRetry={() => void query.refetch()}
        />
      ) : null}

      {ready && visibleDesks.length === 0 ? (
        <TableEmpty
          icon="armchair"
          message={t('desks.empty')}
          action={
            <Button variant="green" icon="plus" onClick={openCreate}>
              {t('desks.new')}
            </Button>
          }
        />
      ) : null}

      {ready && visibleDesks.length > 0 ? (
        <div className="table-scroll table-cards-mobile">
          <table className="table">
            <thead>
              <tr className="table-header">
                <th scope="col">{t('desks.columns.number')}</th>
                <th scope="col">{t('desks.columns.category')}</th>
                <th scope="col">{t('desks.columns.status')}</th>
                <th scope="col">{t('desks.columns.actions')}</th>
              </tr>
            </thead>
            <tbody>
              {visibleDesks.map((desk) => (
                <tr key={desk.id} className="table-row">
                  <td data-label={t('desks.columns.number')}>{desk.number}</td>
                  <td data-label={t('desks.columns.category')}>
                    <DeskCategoryBadge category={desk.category} />
                  </td>
                  <td data-label={t('desks.columns.status')}>
                    <StatusPill tone={desk.active ? 'occupied' : 'free'}>
                      {t(desk.active ? 'desks.status.active' : 'desks.status.inactive')}
                    </StatusPill>
                  </td>
                  <td className="table-actions">
                    <Button
                      variant="white"
                      icon="pencil"
                      aria-label={t('desks.actions.edit')}
                      onClick={() => openEdit(desk)}
                    >
                      {t('desks.actions.edit')}
                    </Button>
                    <Button
                      variant={desk.active ? 'red' : 'green'}
                      icon={desk.active ? 'circle-off' : 'circle-check'}
                      disabled={activationMutation.isPending}
                      onClick={() => toggleActivation(desk)}
                    >
                      {t(desk.active ? 'desks.actions.deactivate' : 'desks.actions.activate')}
                    </Button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
          <Legend
            items={[
              { color: 'var(--green)', label: t('desks.status.active') },
              { color: 'var(--text-muted)', label: t('desks.status.inactive') },
            ]}
          />
        </div>
      ) : null}

      {totalPages > 1 ? (
        <nav className="pagination" aria-label={t('desks.title')}>
          <Button variant="white" disabled={isFirst} onClick={() => setPage((p) => p - 1)}>
            {t('desks.pagination.previous')}
          </Button>
          <span className="pagination-info">
            {t('desks.pagination.pageInfo', { page: page + 1, total: totalPages })}
          </span>
          <Button variant="white" disabled={isLast} onClick={() => setPage((p) => p + 1)}>
            {t('desks.pagination.next')}
          </Button>
        </nav>
      ) : null}

      {isFormOpen ? (
        <DeskFormModal desk={formDesk} onClose={closeForm} onSaved={closeForm} />
      ) : null}
    </section>
  );
}
