import { useMemo, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { Button } from '../components/Button';
import { DayBadges } from '../components/DayBadges';
import { FixedAssignmentModal } from '../components/FixedAssignmentModal';
import { ResourceTypePill } from '../components/ResourceTypePill';
import { RevokeFixedAssignmentModal } from '../components/RevokeFixedAssignmentModal';
import { Spinner } from '../components/Spinner';
import { useDesksQuery } from '../hooks/useDesks';
import { useEmployeesQuery } from '../hooks/useEmployees';
import { useParkingSpacesQuery } from '../hooks/useParkingSpaces';
import { useFixedAssignmentsQuery } from '../hooks/useFixedAssignments';
import { buildDeskLabels } from '../utils/desks';
import { groupFixedAssignments, type FixedAssignmentGroup } from '../utils/fixedAssignments';
import type { Employee } from '../types/employee';
import type { ParkingSpace } from '../types/parkingSpace';

const PAGE_SIZE = 20;
const LOOKUP_SIZE = 100;

// Construye el mapa id -> etiqueta legible para empleados y plazas.
function buildEmployeeNames(employees: Employee[]): Map<number, string> {
  const map = new Map<number, string>();
  for (const employee of employees) {
    map.set(employee.id, `${employee.firstName} ${employee.lastName}`);
  }
  return map;
}

function buildSpaceLabels(spaces: ParkingSpace[]): Map<number, string> {
  const map = new Map<number, string>();
  for (const space of spaces) {
    map.set(space.id, space.label);
  }
  return map;
}

interface RevokeTarget {
  employeeId: number;
  name: string;
}

// Vista ADMIN: lista paginada de asignaciones fijas activas + alta/edicion y
// revocacion por empleado (tasks §4.1-4.3, §4.5).
export function FixedAssignmentsPage() {
  const { t } = useTranslation();
  const [page, setPage] = useState(0);
  const [isFormOpen, setIsFormOpen] = useState(false);
  const [editing, setEditing] = useState<FixedAssignmentGroup | null>(null);
  const [revokeTarget, setRevokeTarget] = useState<RevokeTarget | null>(null);

  const query = useFixedAssignmentsQuery({ page, size: PAGE_SIZE });
  const employeesQuery = useEmployeesQuery({ page: 0, size: LOOKUP_SIZE });
  const spacesQuery = useParkingSpacesQuery({ page: 0, size: LOOKUP_SIZE, active: true });
  const desksQuery = useDesksQuery({ page: 0, size: LOOKUP_SIZE, active: true });

  const employees = useMemo(() => employeesQuery.data?.content ?? [], [employeesQuery.data]);
  const spaces = useMemo(() => spacesQuery.data?.content ?? [], [spacesQuery.data]);
  const desks = useMemo(() => desksQuery.data?.content ?? [], [desksQuery.data]);
  const employeeNames = useMemo(() => buildEmployeeNames(employees), [employees]);
  const spaceLabels = useMemo(() => buildSpaceLabels(spaces), [spaces]);
  const deskLabels = useMemo(() => buildDeskLabels(desks), [desks]);

  function resourceLabel(group: FixedAssignmentGroup): string {
    if (group.resourceType === 'DESK') {
      return deskLabels.get(group.parkingSpaceId) ?? `#${group.parkingSpaceId}`;
    }
    return spaceLabels.get(group.parkingSpaceId) ?? `#${group.parkingSpaceId}`;
  }

  const groups = useMemo(
    () => groupFixedAssignments(query.data?.content ?? []),
    [query.data],
  );
  const totalPages = query.data?.totalPages ?? 0;
  const isFirst = query.data?.first ?? true;
  const isLast = query.data?.last ?? true;

  function openCreate(): void {
    setEditing(null);
    setIsFormOpen(true);
  }

  function openEdit(group: FixedAssignmentGroup): void {
    setEditing(group);
    setIsFormOpen(true);
  }

  function closeForm(): void {
    setIsFormOpen(false);
    setEditing(null);
  }

  function employeeName(id: number): string {
    return employeeNames.get(id) ?? `#${id}`;
  }

  return (
    <section className="fixed-assignments-page" aria-labelledby="fixed-assignments-title">
      <header className="page-header">
        <h1 id="fixed-assignments-title" className="section-title">
          {t('fixedAssignments.title')}
        </h1>
        <div className="page-actions">
          <Button variant="green" icon="plus" onClick={openCreate}>
            {t('fixedAssignments.new')}
          </Button>
        </div>
      </header>

      {query.isLoading ? <Spinner /> : null}

      {query.isError ? (
        <p className="form-error" role="alert">
          {t('fixedAssignments.loadError')}
        </p>
      ) : null}

      {!query.isLoading && !query.isError ? (
        <div className="table-scroll">
          <table className="table">
            <thead>
              <tr className="table-header">
                <th scope="col">{t('fixedAssignments.columns.employee')}</th>
                <th scope="col">{t('fixedAssignments.columns.type')}</th>
                <th scope="col">{t('fixedAssignments.columns.space')}</th>
                <th scope="col">{t('fixedAssignments.columns.days')}</th>
                <th scope="col">{t('fixedAssignments.columns.actions')}</th>
              </tr>
            </thead>
            <tbody>
              {groups.length === 0 ? (
                <tr>
                  <td colSpan={5} className="table-empty">
                    {t('fixedAssignments.empty')}
                  </td>
                </tr>
              ) : (
                groups.map((group) => (
                  <tr key={group.key} className="table-row">
                    <td>{employeeName(group.employeeId)}</td>
                    <td>
                      <ResourceTypePill resourceType={group.resourceType} />
                    </td>
                    <td>{resourceLabel(group)}</td>
                    <td>
                      <DayBadges days={group.days} />
                    </td>
                    <td className="table-actions">
                      <Button
                        variant="white"
                        icon="pencil"
                        aria-label={t('fixedAssignments.actions.edit')}
                        onClick={() => openEdit(group)}
                      >
                        {t('fixedAssignments.actions.edit')}
                      </Button>
                      <Button
                        variant="red"
                        icon="trash"
                        onClick={() =>
                          setRevokeTarget({
                            employeeId: group.employeeId,
                            name: employeeName(group.employeeId),
                          })
                        }
                      >
                        {t('fixedAssignments.actions.revoke')}
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
        <nav className="pagination" aria-label={t('fixedAssignments.title')}>
          <Button variant="white" disabled={isFirst} onClick={() => setPage((p) => p - 1)}>
            {t('fixedAssignments.pagination.previous')}
          </Button>
          <span className="pagination-info">
            {t('fixedAssignments.pagination.pageInfo', { page: page + 1, total: totalPages })}
          </span>
          <Button variant="white" disabled={isLast} onClick={() => setPage((p) => p + 1)}>
            {t('fixedAssignments.pagination.next')}
          </Button>
        </nav>
      ) : null}

      {isFormOpen ? (
        <FixedAssignmentModal
          employees={employees}
          spaces={spaces}
          desks={desks}
          initial={editing}
          onClose={closeForm}
          onSaved={closeForm}
        />
      ) : null}

      {revokeTarget ? (
        <RevokeFixedAssignmentModal
          employeeId={revokeTarget.employeeId}
          employeeName={revokeTarget.name}
          onClose={() => setRevokeTarget(null)}
          onRevoked={() => setRevokeTarget(null)}
        />
      ) : null}
    </section>
  );
}
