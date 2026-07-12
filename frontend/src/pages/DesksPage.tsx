import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { Button } from '../components/Button';
import { DeskCategoryBadge } from '../components/DeskCategoryBadge';
import { DeskFormModal } from '../components/DeskFormModal';
import { Legend } from '../components/Legend';
import { Spinner } from '../components/Spinner';
import { emitApiErrorToast } from '../api/events';
import { useDesksQuery, useUpdateDesk } from '../hooks/useDesks';
import type { Desk } from '../types/desk';

const PAGE_SIZE = 20;

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
  const updateMutation = useUpdateDesk();

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
    updateMutation.mutate(
      {
        id: desk.id,
        body: {
          number: desk.number,
          category: desk.category,
          coordX: desk.coordX,
          coordY: desk.coordY,
          active: !desk.active,
        },
      },
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
  const totalPages = query.data?.totalPages ?? 0;
  const isFirst = query.data?.first ?? true;
  const isLast = query.data?.last ?? true;

  return (
    <section className="desks-page" aria-labelledby="desks-title">
      <header className="page-header">
        <h1 id="desks-title" className="section-title">
          {t('desks.title')}
        </h1>
        <div className="page-actions">
          <Button variant="green" icon="plus" onClick={openCreate}>
            {t('desks.new')}
          </Button>
        </div>
      </header>

      <div className="toolbar">
        <div className="search-box">
          <i className="ti ti-search" aria-hidden="true" />
          <input
            type="search"
            aria-label={t('desks.searchLabel')}
            placeholder={t('desks.searchPlaceholder')}
            value={q}
            onChange={(event) => setQ(event.target.value)}
          />
        </div>
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
      </div>

      {query.isLoading ? <Spinner /> : null}

      {query.isError ? (
        <p className="form-error" role="alert">
          {t('desks.loadError')}
        </p>
      ) : null}

      {!query.isLoading && !query.isError ? (
        <div className="table-scroll">
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
              {visibleDesks.length === 0 ? (
                <tr>
                  <td colSpan={4} className="table-empty">
                    {t('desks.empty')}
                  </td>
                </tr>
              ) : (
                visibleDesks.map((desk) => (
                  <tr key={desk.id} className="table-row">
                    <td>{desk.number}</td>
                    <td>
                      <DeskCategoryBadge category={desk.category} />
                    </td>
                    <td>
                      <span className={`pill ${desk.active ? 'pill-green' : 'pill-gray'}`}>
                        {t(desk.active ? 'desks.status.active' : 'desks.status.inactive')}
                      </span>
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
                        disabled={updateMutation.isPending}
                        onClick={() => toggleActivation(desk)}
                      >
                        {t(desk.active ? 'desks.actions.deactivate' : 'desks.actions.activate')}
                      </Button>
                    </td>
                  </tr>
                ))
              )}
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
