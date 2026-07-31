import { RESOURCE_ICON } from '../utils/resourceIcon';
import { useState, type ReactNode } from 'react';
import { useTranslation } from 'react-i18next';
import { Button } from '../components/Button';
import { DeskCategoryBadge } from '../components/DeskCategoryBadge';
import { DeskFormModal } from '../components/DeskFormModal';
import { Legend } from '../components/Legend';
import { PageFrame } from '../components/PageFrame';
import { SearchBox } from '../components/SearchBox';
import { StatusPill } from '../components/StatusPill';
import { TableEmpty, TableError, TableSkeleton } from '../components/TableStates';
import { emitApiErrorToast } from '../api/events';
import { deactivationErrorKey } from '../utils/resourceDeactivation';
import { useDesksQuery, useSetDeskActivation } from '../hooks/useDesks';
import type { Desk } from '../types/desk';

// Sin paginación: se cargan todos los puestos en un único scroll (no hay tantos).
const LIST_SIZE = 500;
const COUNT_SIZE = 1;

type ActiveFilter = 'all' | 'active' | 'inactive';

// Color del punto por estado en los chips de filtro (mismo mapa que los KPIs).
const ESTADO_DOT: Record<ActiveFilter, string> = {
  all: 'var(--ink-faint)',
  active: 'var(--accent)',
  inactive: 'var(--rel)',
};
const ESTADO_FILTERS: ActiveFilter[] = ['all', 'active', 'inactive'];

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
// `embedded`: montada dentro de "Recursos", sin su propia cabecera (ver ParkingSpacesPage).
export function DesksPage({ tabsSwitch }: { tabsSwitch?: ReactNode } = {}) {
  const { t } = useTranslation();
  const [filter, setFilter] = useState<ActiveFilter>('all');
  const [q, setQ] = useState('');
  const [formDesk, setFormDesk] = useState<Desk | null>(null);
  const [isFormOpen, setIsFormOpen] = useState(false);

  const query = useDesksQuery({
    page: 0,
    size: LIST_SIZE,
    active: filterToActive(filter),
  });
  // Recuentos GLOBALES para los contadores de los chips de estado (sustituyen a la
  // antigua tira de KPIs): independientes del filtro visible.
  const totalCountQuery = useDesksQuery({ page: 0, size: COUNT_SIZE });
  const activeCountQuery = useDesksQuery({ page: 0, size: COUNT_SIZE, active: true });
  const totalCount = totalCountQuery.data?.totalElements;
  const activeCount = activeCountQuery.data?.totalElements;
  const estadoCount: Record<ActiveFilter, number | undefined> = {
    all: totalCount,
    active: activeCount,
    inactive:
      totalCount !== undefined && activeCount !== undefined
        ? Math.max(totalCount - activeCount, 0)
        : undefined,
  };
  const activationMutation = useSetDeskActivation();

  function handleFilter(value: ActiveFilter): void {
    setFilter(value);
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
      { onError: (error) => emitApiErrorToast(deactivationErrorKey(error, 'desks')) },
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

  return (
    <PageFrame
      eyebrow={t('resources.eyebrow')}
      title={t('resources.title')}
      bodyLabel={t('desks.title')}
      resourceSelector={tabsSwitch}
      toolbar={
        <Button variant="green" icon="plus" onClick={openCreate}>
          {t('desks.new')}
        </Button>
      }
      subbar={
        <div className="mgmt-filters">
          <SearchBox
            label={t('desks.searchLabel')}
            placeholder={t('desks.searchPlaceholder')}
            value={q}
            onValueChange={setQ}
          />
          <div className="chip-filters" role="group" aria-label={t('desks.filterLabel')}>
            {ESTADO_FILTERS.map((value) => {
              const isActive = filter === value;
              const count = estadoCount[value];
              return (
                <button
                  key={value}
                  type="button"
                  className={`chip-filter${isActive ? ' is-active' : ''}`}
                  aria-pressed={isActive}
                  onClick={() => handleFilter(value)}
                >
                  <span className="cf-dot" style={{ background: ESTADO_DOT[value] }} aria-hidden="true" />
                  {t(`desks.filter.${value}`)}
                  {count !== undefined ? <span className="cf-count">{count}</span> : null}
                </button>
              );
            })}
          </div>
        </div>
      }
    >
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
          icon={RESOURCE_ICON.DESK}
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

      {isFormOpen ? (
        <DeskFormModal desk={formDesk} onClose={closeForm} onSaved={closeForm} />
      ) : null}
    </PageFrame>
  );
}
