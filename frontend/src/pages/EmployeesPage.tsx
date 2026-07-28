import { useMemo, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { Avatar } from '../components/Avatar';
import { Button } from '../components/Button';
import { ConfirmDialog } from '../components/ConfirmDialog';
import { Tooltip, TooltipProvider } from '../components/Tooltip';
import { EmployeeFormModal } from '../components/EmployeeFormModal';
import { ExportMenu } from '../components/ExportMenu';
import { Menu } from '../components/Menu';
import { PageHeader } from '../components/PageHeader';
import { ResetPasswordModal } from '../components/ResetPasswordModal';
import { SearchBox } from '../components/SearchBox';
import { StatusPill } from '../components/StatusPill';
import { TableEmpty, TableError, TableSkeleton } from '../components/TableStates';
import { Toolbar } from '../components/Toolbar';
import { EXPORT_PATHS } from '../api/exportApi';
import {
  useDeactivateEmployee,
  useEmployeesQuery,
  useReactivateEmployee,
} from '../hooks/useEmployees';
import { useDesksQuery } from '../hooks/useDesks';
import { useFixedAssignmentsQuery } from '../hooks/useFixedAssignments';
import { useParkingSpacesQuery } from '../hooks/useParkingSpaces';
import { buildDeskLabels } from '../utils/desks';
import { initialsOf } from '../utils/initials';
import {
  assignedWeekDays,
  distinctWeekResources,
  indexDayResourceMapsByEmployee,
  type DayResourceMap,
} from '../utils/fixedAssignments';
import type { Employee } from '../types/employee';
import type { ParkingSpace } from '../types/parkingSpace';

const PAGE_SIZE = 20;
const LOOKUP_SIZE = 200;
const NONE = '—';

function buildSpaceLabels(spaces: ParkingSpace[]): Map<number, string> {
  const map = new Map<number, string>();
  for (const space of spaces) {
    map.set(space.id, space.label);
  }
  return map;
}

const EMPTY_MAP: DayResourceMap = {};

// Celda de recurso fijo. Tres formas segun el dato (mockup 03):
//  · sin recurso -> "—".
//  · mismo recurso todos sus dias -> etiqueta (verde) + chips de dia (L-V).
//  · recursos distintos por dia -> chips por dia con la etiqueta del recurso de
//    ESE dia (p.ej. D-03 lunes, D-07 miercoles), respetando el dato real.
// Celda de recurso fijo (plaza/puesto): RESUMEN compacto de una línea + tooltip con
// el detalle por día (para que la fila del empleado no crezca en alto).
function ResourceCell({ map, labels }: { map: DayResourceMap; labels: Map<number, string> }) {
  const { t } = useTranslation();
  const resourceIds = distinctWeekResources(map);
  if (resourceIds.length === 0) {
    return <span className="text-muted">{NONE}</span>;
  }
  const labelFor = (id: number): string => labels.get(id) ?? `#${id}`;
  const days = assignedWeekDays(map);
  const summary =
    resourceIds.length === 1
      ? `${labelFor(resourceIds[0])} · ${days.map((d) => t(`common.weekdayChip.${d}`)).join(' ')}`
      : resourceIds.map(labelFor).join(', ');
  const full = (
    <ul className="fixed-tip-list">
      {days.map((d) => (
        <li key={d}>
          <span className="fixed-tip-day">{t(`common.weekdayName.${d}`)}</span>
          <span>{labelFor(map[d] as number)}</span>
        </li>
      ))}
    </ul>
  );
  return (
    <TooltipProvider>
      <Tooltip content={full}>
        <button type="button" className="fixed-cell-summary">
          {summary}
        </button>
      </Tooltip>
    </TooltipProvider>
  );
}

// Diálogo de confirmación de BAJA (acción sensible). Extraído a módulo para no
// cargar la complejidad cognitiva de la página (S3776); guarda el null aquí.
function DeactivateConfirm({
  target,
  busy,
  onConfirm,
  onCancel,
}: {
  target: Employee | null;
  busy: boolean;
  onConfirm: (employee: Employee) => void;
  onCancel: () => void;
}) {
  const { t } = useTranslation();
  return (
    <ConfirmDialog
      open={target !== null}
      onOpenChange={(open) => {
        if (!open) onCancel();
      }}
      tone="green"
      icon="user-off"
      title={t('employees.deactivateConfirm.title')}
      description={
        target
          ? t('employees.deactivateConfirm.body', {
              name: `${target.firstName} ${target.lastName}`.trim(),
            })
          : ''
      }
      confirmLabel={t('employees.actions.deactivate')}
      busy={busy}
      onConfirm={() => {
        if (target) onConfirm(target);
      }}
    />
  );
}

// Vista de gestion de empleados (ADMIN): tabla paginada + busqueda + acciones,
// con avatar por empleado y columnas de plaza y puesto fijos (mockup 03).
export function EmployeesPage() {
  const { t } = useTranslation();
  const [q, setQ] = useState('');
  const [page, setPage] = useState(0);
  const [formEmployee, setFormEmployee] = useState<Employee | null>(null);
  const [isFormOpen, setIsFormOpen] = useState(false);
  const [resetEmployee, setResetEmployee] = useState<Employee | null>(null);
  // Empleado pendiente de confirmar la BAJA (acción sensible). La reactivación no
  // pide confirmación.
  const [deactivateTarget, setDeactivateTarget] = useState<Employee | null>(null);

  const query = useEmployeesQuery({ page, size: PAGE_SIZE, q });
  const assignmentsQuery = useFixedAssignmentsQuery({ page: 0, size: LOOKUP_SIZE });
  const spacesQuery = useParkingSpacesQuery({ page: 0, size: LOOKUP_SIZE });
  const desksQuery = useDesksQuery({ page: 0, size: LOOKUP_SIZE });
  const deactivateMutation = useDeactivateEmployee();
  const reactivateMutation = useReactivateEmployee();

  const spaceLabels = useMemo(
    () => buildSpaceLabels(spacesQuery.data?.content ?? []),
    [spacesQuery.data],
  );
  const deskLabels = useMemo(
    () => buildDeskLabels(desksQuery.data?.content ?? []),
    [desksQuery.data],
  );
  const resourcesByEmployee = useMemo(
    () => indexDayResourceMapsByEmployee(assignmentsQuery.data?.content ?? []),
    [assignmentsQuery.data],
  );

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
      setDeactivateTarget(employee); // baja → confirmar primero
    } else {
      reactivateMutation.mutate(employee.id);
    }
  }

  function resourcesFor(employeeId: number): { parking: DayResourceMap; desk: DayResourceMap } {
    return resourcesByEmployee.get(employeeId) ?? { parking: EMPTY_MAP, desk: EMPTY_MAP };
  }

  const employees = query.data?.content ?? [];
  const ready = !query.isLoading && !query.isError;
  const totalPages = query.data?.totalPages ?? 0;
  const isFirst = query.data?.first ?? true;
  const isLast = query.data?.last ?? true;

  return (
    <section className="employees-page" aria-label={t('employees.title')}>
      <PageHeader
        eyebrow={t('employees.eyebrow')}
        title={t('employees.title')}
        description={t('employees.description')}
        actions={
          <>
            <ExportMenu
              path={EXPORT_PATHS.employees}
              fallbackBase="employees"
              requiredRole="ADMIN"
            />
            <Button variant="green" icon="plus" onClick={openCreate}>
              {t('employees.new')}
            </Button>
          </>
        }
      />

      <Toolbar ariaLabel={t('employees.searchLabel')}>
        <SearchBox
          label={t('employees.searchLabel')}
          placeholder={t('employees.searchPlaceholder')}
          value={q}
          onValueChange={handleSearch}
        />
      </Toolbar>

      {query.isLoading ? <TableSkeleton label={t('employees.loading')} columns={8} /> : null}

      {query.isError ? (
        <TableError
          message={t('employees.loadError')}
          retryLabel={t('common.retry')}
          onRetry={() => void query.refetch()}
        />
      ) : null}

      {ready && employees.length === 0 ? (
        <TableEmpty
          icon="users"
          message={t('employees.empty')}
          action={
            <Button variant="green" icon="plus" onClick={openCreate}>
              {t('employees.new')}
            </Button>
          }
        />
      ) : null}

      {ready && employees.length > 0 ? (
        <div className="table-scroll table-cards-mobile emp-table">
          <table className="table">
            <thead>
              <tr className="table-header">
                <th scope="col">{t('employees.columns.name')}</th>
                <th scope="col">{t('employees.columns.department')}</th>
                <th scope="col">{t('employees.columns.parkingFixed')}</th>
                <th scope="col">{t('employees.columns.deskFixed')}</th>
                <th scope="col">{t('employees.columns.role')}</th>
                <th scope="col">{t('employees.columns.category')}</th>
                <th scope="col">{t('employees.columns.status')}</th>
                <th scope="col">{t('employees.columns.actions')}</th>
              </tr>
            </thead>
            <tbody>
              {employees.map((employee) => {
                const fullName = `${employee.firstName} ${employee.lastName}`;
                const resources = resourcesFor(employee.id);
                return (
                  <tr key={employee.id} className="table-row">
                    <td data-label={t('employees.columns.name')}>
                      <div className="employee-cell">
                        <Avatar
                          initials={initialsOf(employee)}
                          label={fullName}
                          size="sm"
                          seed={fullName}
                        />
                        <div className="employee-cell-text">
                          <span className="employee-cell-name">{fullName}</span>
                          <span className="employee-cell-email">{employee.email}</span>
                        </div>
                      </div>
                    </td>
                    <td data-label={t('employees.columns.department')}>
                      {employee.department ?? NONE}
                    </td>
                    <td data-label={t('employees.columns.parkingFixed')}>
                      <ResourceCell map={resources.parking} labels={spaceLabels} />
                    </td>
                    <td data-label={t('employees.columns.deskFixed')}>
                      <ResourceCell map={resources.desk} labels={deskLabels} />
                    </td>
                    <td data-label={t('employees.columns.role')}>
                      {t(`employees.role.${employee.role}`)}
                    </td>
                    <td data-label={t('employees.columns.category')}>
                      {t(`employees.category.${employee.category}`)}
                    </td>
                    <td data-label={t('employees.columns.status')}>
                      <StatusPill tone={employee.active ? 'occupied' : 'free'}>
                        {t(
                          employee.active ? 'employees.status.active' : 'employees.status.inactive',
                        )}
                      </StatusPill>
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
                      <Menu
                        ariaLabel={t('employees.actions.more')}
                        trigger={
                          <button
                            type="button"
                            className="icon-btn"
                            aria-label={t('employees.actions.more')}
                          >
                            <i className="ti ti-dots-vertical" aria-hidden="true" />
                          </button>
                        }
                        items={[
                          {
                            key: 'toggle',
                            label: t(
                              employee.active
                                ? 'employees.actions.deactivate'
                                : 'employees.actions.reactivate',
                            ),
                            icon: employee.active ? 'user-off' : 'user-check',
                            danger: employee.active,
                            onSelect: () => toggleActive(employee),
                          },
                          {
                            key: 'reset',
                            label: t('employees.actions.resetPassword'),
                            icon: 'key',
                            onSelect: () => setResetEmployee(employee),
                          },
                        ]}
                      />
                    </td>
                  </tr>
                );
              })}
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

      <DeactivateConfirm
        target={deactivateTarget}
        busy={deactivateMutation.isPending}
        onCancel={() => setDeactivateTarget(null)}
        onConfirm={(employee) => {
          deactivateMutation.mutate(employee.id);
          setDeactivateTarget(null);
        }}
      />
    </section>
  );
}
