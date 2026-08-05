import { useState, type ReactNode } from 'react';
import { useTranslation } from 'react-i18next';
import { Button } from '../components/Button';
import { Legend } from '../components/Legend';
import { PageFrame } from '../components/PageFrame';
import { ParkingSpaceFormModal } from '../components/ParkingSpaceFormModal';
import { SearchBox } from '../components/SearchBox';
import { StatusPill } from '../components/StatusPill';
import { TableEmpty, TableError, TableSkeleton } from '../components/TableStates';
import { emitApiErrorToast } from '../api/events';
import { deactivationErrorKey } from '../utils/resourceDeactivation';
import { useParkingSpacesQuery, useUpdateParkingSpace } from '../hooks/useParkingSpaces';
import type { ParkingSpace } from '../types/parkingSpace';

const PAGE_SIZE = 20;
const COUNT_SIZE = 1;

type ActiveFilter = 'all' | 'active' | 'inactive';

// Color del punto por estado en los chips de filtro (mismo mapa que los KPIs).
const ESTADO_DOT: Record<ActiveFilter, string> = {
  all: 'var(--ink-faint)',
  active: 'var(--accent)',
  inactive: 'var(--rel)',
};
const ESTADO_FILTERS: ActiveFilter[] = ['all', 'active', 'inactive'];

// Recuentos por estado para los contadores de los chips (all/active/inactive); el de
// inactivas se deriva de total − activas. `undefined` mientras aún no hay datos.
function estadoCounts(
  total: number | undefined,
  active: number | undefined,
): Record<ActiveFilter, number | undefined> {
  const inactive =
    total !== undefined && active !== undefined ? Math.max(total - active, 0) : undefined;
  return { all: total, active, inactive };
}

// Traduce el filtro de la UI al parametro `active` del contrato.
function filterToActive(filter: ActiveFilter): boolean | undefined {
  if (filter === 'active') {
    return true;
  }
  if (filter === 'inactive') {
    return false;
  }
  return undefined;
}

// Vista de gestion de plazas (ADMIN): configuracion del total + tabla paginada
// con filtro activa/inactiva.
const ALL_FLOORS = 'all';
type FloorFilter = number | typeof ALL_FLOORS;

// Traduce el filtro de planta al parametro `floor` del contrato.
function floorToParam(filter: FloorFilter): number | undefined {
  return filter === ALL_FLOORS ? undefined : filter;
}

// Opciones de planta derivadas de las plazas cargadas; garantiza que la planta
// seleccionada permanezca disponible aunque el filtro reduzca la lista.
function buildFloorOptions(spaces: ParkingSpace[], selected: FloorFilter): number[] {
  const floors = new Set<number>(spaces.map((space) => space.floor));
  if (selected !== ALL_FLOORS) {
    floors.add(selected);
  }
  return Array.from(floors).sort((a, b) => a - b);
}

// `embedded`: cuando se monta dentro de la sección "Recursos", no pinta su propia
// cabecera (el contenedor aporta el título de sección + el conmutador); solo la
// acción de crear en una barra compacta, para evitar títulos duplicados.
export function ParkingSpacesPage({ tabsSwitch }: { tabsSwitch?: ReactNode } = {}) {
  const { t } = useTranslation();
  const [filter, setFilter] = useState<ActiveFilter>('all');
  const [floor, setFloor] = useState<FloorFilter>(ALL_FLOORS);
  const [q, setQ] = useState('');
  const [page, setPage] = useState(0);
  const [formSpace, setFormSpace] = useState<ParkingSpace | null>(null);
  const [isFormOpen, setIsFormOpen] = useState(false);

  const query = useParkingSpacesQuery({
    page,
    size: PAGE_SIZE,
    active: filterToActive(filter),
    floor: floorToParam(floor),
  });
  // Recuentos GLOBALES de inventario (independientes del filtro visible) para los
  // contadores de los chips de estado, que sustituyen a la antigua tira de KPIs.
  const totalCountQuery = useParkingSpacesQuery({ page: 0, size: COUNT_SIZE });
  const activeCountQuery = useParkingSpacesQuery({ page: 0, size: COUNT_SIZE, active: true });
  const estadoCount = estadoCounts(
    totalCountQuery.data?.totalElements,
    activeCountQuery.data?.totalElements,
  );
  const updateMutation = useUpdateParkingSpace();

  function handleFilter(value: ActiveFilter): void {
    setFilter(value);
    setPage(0);
  }

  function handleFloor(value: string): void {
    setFloor(value === ALL_FLOORS ? ALL_FLOORS : Number(value));
    setPage(0);
  }

  // Activa/desactiva una plaza (paridad con la vista de puestos). Reenvia el
  // numero actual porque el contrato de PUT exige el cuerpo completo.
  function toggleActivation(space: ParkingSpace): void {
    updateMutation.mutate(
      { id: space.id, body: { number: space.number, active: !space.active } },
      {
        onError: (error) =>
          emitApiErrorToast(deactivationErrorKey(error, 'parkingSpaces')),
      },
    );
  }

  function openCreate(): void {
    setFormSpace(null);
    setIsFormOpen(true);
  }

  function openEdit(space: ParkingSpace): void {
    setFormSpace(space);
    setIsFormOpen(true);
  }

  function closeForm(): void {
    setIsFormOpen(false);
    setFormSpace(null);
  }

  const spaces = query.data?.content ?? [];
  // Busqueda local por etiqueta: el contrato de /parking-spaces no expone `q`, asi
  // que el filtro actua sobre la pagina cargada.
  const search = q.trim().toLowerCase();
  const visibleSpaces = search
    ? spaces.filter((space) => space.label.toLowerCase().includes(search))
    : spaces;
  const ready = !query.isLoading && !query.isError;
  const totalPages = query.data?.totalPages ?? 0;
  const isFirst = query.data?.first ?? true;
  const isLast = query.data?.last ?? true;

  const floorOptions = buildFloorOptions(spaces, floor);
  const floorSelectValue = floor === ALL_FLOORS ? ALL_FLOORS : String(floor);

  return (
    <PageFrame
      eyebrow={t('resources.eyebrow')}
      title={t('resources.title')}
      bodyLabel={t('parkingSpaces.title')}
      resourceSelector={tabsSwitch}
      toolbar={
        <Button variant="green" icon="plus" onClick={openCreate}>
          {t('parkingSpaces.new')}
        </Button>
      }
      subbar={
        <div className="mgmt-filters">
          <SearchBox
            label={t('parkingSpaces.searchLabel')}
            placeholder={t('parkingSpaces.searchPlaceholder')}
            value={q}
            onValueChange={setQ}
          />
          <div className="chip-filters" role="group" aria-label={t('parkingSpaces.filterLabel')}>
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
                  {t(`parkingSpaces.filter.${value}`)}
                  {count !== undefined ? <span className="cf-count">{count}</span> : null}
                </button>
              );
            })}
          </div>
          <select
            id="parking-spaces-floor"
            className="field-input mgmt-floor-select"
            value={floorSelectValue}
            aria-label={t('parkingSpaces.filterFloorLabel')}
            onChange={(event) => handleFloor(event.target.value)}
          >
            <option value={ALL_FLOORS}>{t('parkingSpaces.floorFilter.all')}</option>
            {floorOptions.map((value) => (
              <option key={value} value={String(value)}>
                {t('parkingSpaces.floorOption', { floor: value })}
              </option>
            ))}
          </select>
        </div>
      }
      footer={
        totalPages > 1 ? (
          <>
            <span className="pagination-info">
              {t('parkingSpaces.pagination.pageInfo', { page: page + 1, total: totalPages })}
            </span>
            <div className="pf-footer-nav">
              <Button variant="white" disabled={isFirst} onClick={() => setPage((p) => p - 1)}>
                {t('parkingSpaces.pagination.previous')}
              </Button>
              <Button variant="white" disabled={isLast} onClick={() => setPage((p) => p + 1)}>
                {t('parkingSpaces.pagination.next')}
              </Button>
            </div>
          </>
        ) : undefined
      }
    >
      {query.isLoading ? <TableSkeleton label={t('common.loading')} columns={4} /> : null}

      {query.isError ? (
        <TableError
          message={t('parkingSpaces.loadError')}
          retryLabel={t('common.retry')}
          onRetry={() => void query.refetch()}
        />
      ) : null}

      {ready ? (
        visibleSpaces.length === 0 ? (
          <TableEmpty
            icon="parking"
            message={t('parkingSpaces.empty')}
            action={
              <Button variant="green" icon="plus" onClick={openCreate}>
                {t('parkingSpaces.new')}
              </Button>
            }
          />
        ) : (
          <div className="table-scroll table-cards-mobile">
            <table className="table">
              <thead>
                <tr className="table-header">
                  <th scope="col">{t('parkingSpaces.columns.label')}</th>
                  <th scope="col">{t('parkingSpaces.columns.floor')}</th>
                  <th scope="col">{t('parkingSpaces.columns.status')}</th>
                  <th scope="col">{t('parkingSpaces.columns.actions')}</th>
                </tr>
              </thead>
              <tbody>
                {visibleSpaces.map((space) => (
                  <tr key={space.id} className="table-row">
                    <td data-label={t('parkingSpaces.columns.label')}>{space.label}</td>
                    <td data-label={t('parkingSpaces.columns.floor')}>
                      {t('parkingSpaces.floorValue', { floor: space.floor })}
                    </td>
                    <td data-label={t('parkingSpaces.columns.status')}>
                      <StatusPill tone={space.active ? 'occupied' : 'free'}>
                        {t(
                          space.active
                            ? 'parkingSpaces.status.active'
                            : 'parkingSpaces.status.inactive',
                        )}
                      </StatusPill>
                    </td>
                    <td className="table-actions">
                      <Button
                        variant="white"
                        icon="pencil"
                        aria-label={t('parkingSpaces.actions.edit')}
                        onClick={() => openEdit(space)}
                      >
                        {t('parkingSpaces.actions.edit')}
                      </Button>
                      <Button
                        variant={space.active ? 'red' : 'green'}
                        icon={space.active ? 'circle-off' : 'circle-check'}
                        disabled={updateMutation.isPending}
                        onClick={() => toggleActivation(space)}
                      >
                        {t(
                          space.active
                            ? 'parkingSpaces.actions.deactivate'
                            : 'parkingSpaces.actions.activate',
                        )}
                      </Button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
            <Legend
              items={[
                { color: 'var(--green)', label: t('parkingSpaces.status.active') },
                { color: 'var(--text-muted)', label: t('parkingSpaces.status.inactive') },
              ]}
            />
          </div>
        )
      ) : null}

      {isFormOpen ? (
        <ParkingSpaceFormModal space={formSpace} onClose={closeForm} onSaved={closeForm} />
      ) : null}
    </PageFrame>
  );
}
