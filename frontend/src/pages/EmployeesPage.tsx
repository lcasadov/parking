import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { Button } from '../components/Button';
import { EmployeeFormModal } from '../components/EmployeeFormModal';
import { ResetPasswordModal } from '../components/ResetPasswordModal';
import { Spinner } from '../components/Spinner';
import {
  useDeactivateEmployee,
  useEmployeesQuery,
  useExportEmployees,
  useReactivateEmployee,
} from '../hooks/useEmployees';
import type { Employee } from '../types/employee';

const PAGE_SIZE = 20;

// Vista de gestion de empleados (ADMIN): tabla paginada + busqueda + acciones.
export function EmployeesPage() {
  const { t } = useTranslation();
  const [q, setQ] = useState('');
  const [page, setPage] = useState(0);
  const [formEmployee, setFormEmployee] = useState<Employee | null>(null);
  const [isFormOpen, setIsFormOpen] = useState(false);
  const [resetEmployee, setResetEmployee] = useState<Employee | null>(null);

  const query = useEmployeesQuery({ page, size: PAGE_SIZE, q });
  const deactivateMutation = useDeactivateEmployee();
  const reactivateMutation = useReactivateEmployee();
  const exportMutation = useExportEmployees();

  function handleSearch(value: string): void {
    setQ(value);
    setPage(0);
  }

  function openCreate(): void {
    setFormEmployee(null);
    setIsFormOpen(true);
  }

  function openEdit(employee: Employee): void {
    setFormEmployee(employee);
    setIsFormOpen(true);
  }

  function closeForm(): void {
    setIsFormOpen(false);
    setFormEmployee(null);
  }

  function toggleActive(employee: Employee): void {
    if (employee.active) {
      deactivateMutation.mutate(employee.id);
    } else {
      reactivateMutation.mutate(employee.id);
    }
  }

  const employees = query.data?.content ?? [];
  const totalPages = query.data?.totalPages ?? 0;
  const isFirst = query.data?.first ?? true;
  const isLast = query.data?.last ?? true;

  return (
    <section className="employees-page" aria-labelledby="employees-title">
      <header className="page-header">
        <h1 id="employees-title" className="section-title">
          {t('employees.title')}
        </h1>
        <div className="page-actions">
          <Button variant="white" icon="download" onClick={() => exportMutation.mutate('csv')}>
            {t('employees.exportCsv')}
          </Button>
          <Button variant="white" icon="download" onClick={() => exportMutation.mutate('xlsx')}>
            {t('employees.exportXlsx')}
          </Button>
          <Button variant="green" icon="plus" onClick={openCreate}>
            {t('employees.new')}
          </Button>
        </div>
      </header>

      <div className="toolbar">
        <input
          type="search"
          className="field-input"
          aria-label={t('employees.searchLabel')}
          placeholder={t('employees.searchPlaceholder')}
          value={q}
          onChange={(event) => handleSearch(event.target.value)}
        />
      </div>

      {query.isLoading ? <Spinner /> : null}

      {query.isError ? (
        <p className="form-error" role="alert">
          {t('employees.loadError')}
        </p>
      ) : null}

      {!query.isLoading && !query.isError ? (
        <div className="table-scroll">
          <table className="table">
            <thead>
              <tr className="table-header">
                <th scope="col">{t('employees.columns.name')}</th>
                <th scope="col">{t('employees.columns.email')}</th>
                <th scope="col">{t('employees.columns.department')}</th>
                <th scope="col">{t('employees.columns.role')}</th>
                <th scope="col">{t('employees.columns.status')}</th>
                <th scope="col">{t('employees.columns.actions')}</th>
              </tr>
            </thead>
            <tbody>
              {employees.length === 0 ? (
                <tr>
                  <td colSpan={6} className="table-empty">
                    {t('employees.empty')}
                  </td>
                </tr>
              ) : (
                employees.map((employee) => (
                  <tr key={employee.id} className="table-row">
                    <td>{`${employee.firstName} ${employee.lastName}`}</td>
                    <td>{employee.email}</td>
                    <td>{employee.department ?? '—'}</td>
                    <td>{t(`employees.role.${employee.role}`)}</td>
                    <td>
                      <span className={`pill ${employee.active ? 'pill-green' : 'pill-gray'}`}>
                        {t(employee.active ? 'employees.status.active' : 'employees.status.inactive')}
                      </span>
                    </td>
                    <td className="table-actions">
                      <Button
                        variant="white"
                        icon="pencil"
                        aria-label={t('employees.actions.edit')}
                        onClick={() => openEdit(employee)}
                      >
                        {t('employees.actions.edit')}
                      </Button>
                      <Button
                        variant={employee.active ? 'red' : 'green'}
                        onClick={() => toggleActive(employee)}
                      >
                        {t(
                          employee.active
                            ? 'employees.actions.deactivate'
                            : 'employees.actions.reactivate',
                        )}
                      </Button>
                      <Button
                        variant="blue"
                        icon="key"
                        onClick={() => setResetEmployee(employee)}
                      >
                        {t('employees.actions.resetPassword')}
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
        <nav className="pagination" aria-label={t('employees.title')}>
          <Button variant="white" disabled={isFirst} onClick={() => setPage((p) => p - 1)}>
            {t('employees.pagination.previous')}
          </Button>
          <span className="pagination-info">
            {t('employees.pagination.pageInfo', { page: page + 1, total: totalPages })}
          </span>
          <Button variant="white" disabled={isLast} onClick={() => setPage((p) => p + 1)}>
            {t('employees.pagination.next')}
          </Button>
        </nav>
      ) : null}

      {isFormOpen ? (
        <EmployeeFormModal employee={formEmployee} onClose={closeForm} onSaved={closeForm} />
      ) : null}

      {resetEmployee ? (
        <ResetPasswordModal employee={resetEmployee} onClose={() => setResetEmployee(null)} />
      ) : null}
    </section>
  );
}
