import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { Button } from '../components/Button';
import { ConfigureParkingCard } from '../components/ConfigureParkingCard';
import { ParkingSpaceFormModal } from '../components/ParkingSpaceFormModal';
import { Spinner } from '../components/Spinner';
import { useParkingSpacesQuery } from '../hooks/useParkingSpaces';
import type { ParkingSpace } from '../types/parkingSpace';

const PAGE_SIZE = 20;

type ActiveFilter = 'all' | 'active' | 'inactive';

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
export function ParkingSpacesPage() {
  const { t } = useTranslation();
  const [filter, setFilter] = useState<ActiveFilter>('all');
  const [page, setPage] = useState(0);
  const [formSpace, setFormSpace] = useState<ParkingSpace | null>(null);
  const [isFormOpen, setIsFormOpen] = useState(false);

  const query = useParkingSpacesQuery({
    page,
    size: PAGE_SIZE,
    active: filterToActive(filter),
  });

  function handleFilter(value: ActiveFilter): void {
    setFilter(value);
    setPage(0);
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
  const totalPages = query.data?.totalPages ?? 0;
  const isFirst = query.data?.first ?? true;
  const isLast = query.data?.last ?? true;

  return (
    <section className="parking-spaces-page" aria-labelledby="parking-spaces-title">
      <header className="page-header">
        <h1 id="parking-spaces-title" className="section-title">
          {t('parkingSpaces.title')}
        </h1>
        <div className="page-actions">
          <Button variant="green" icon="plus" onClick={openCreate}>
            {t('parkingSpaces.new')}
          </Button>
        </div>
      </header>

      <ConfigureParkingCard />

      <div className="toolbar">
        <label className="field-label" htmlFor="parking-spaces-filter">
          {t('parkingSpaces.filterLabel')}
        </label>
        <select
          id="parking-spaces-filter"
          className="field-input"
          value={filter}
          onChange={(event) => handleFilter(event.target.value as ActiveFilter)}
        >
          <option value="all">{t('parkingSpaces.filter.all')}</option>
          <option value="active">{t('parkingSpaces.filter.active')}</option>
          <option value="inactive">{t('parkingSpaces.filter.inactive')}</option>
        </select>
      </div>

      {query.isLoading ? <Spinner /> : null}

      {query.isError ? (
        <p className="form-error" role="alert">
          {t('parkingSpaces.loadError')}
        </p>
      ) : null}

      {!query.isLoading && !query.isError ? (
        <div className="table-scroll">
          <table className="table">
            <thead>
              <tr className="table-header">
                <th scope="col">{t('parkingSpaces.columns.label')}</th>
                <th scope="col">{t('parkingSpaces.columns.status')}</th>
                <th scope="col">{t('parkingSpaces.columns.actions')}</th>
              </tr>
            </thead>
            <tbody>
              {spaces.length === 0 ? (
                <tr>
                  <td colSpan={3} className="table-empty">
                    {t('parkingSpaces.empty')}
                  </td>
                </tr>
              ) : (
                spaces.map((space) => (
                  <tr key={space.id} className="table-row">
                    <td>{space.label}</td>
                    <td>
                      <span className={`pill ${space.active ? 'pill-green' : 'pill-gray'}`}>
                        {t(space.active ? 'parkingSpaces.status.active' : 'parkingSpaces.status.inactive')}
                      </span>
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
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
      ) : null}

      {totalPages > 1 ? (
        <nav className="pagination" aria-label={t('parkingSpaces.title')}>
          <Button variant="white" disabled={isFirst} onClick={() => setPage((p) => p - 1)}>
            {t('parkingSpaces.pagination.previous')}
          </Button>
          <span className="pagination-info">
            {t('parkingSpaces.pagination.pageInfo', { page: page + 1, total: totalPages })}
          </span>
          <Button variant="white" disabled={isLast} onClick={() => setPage((p) => p + 1)}>
            {t('parkingSpaces.pagination.next')}
          </Button>
        </nav>
      ) : null}

      {isFormOpen ? (
        <ParkingSpaceFormModal space={formSpace} onClose={closeForm} onSaved={closeForm} />
      ) : null}
    </section>
  );
}
