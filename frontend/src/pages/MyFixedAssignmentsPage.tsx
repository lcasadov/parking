import { useMemo, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { Button } from '../components/Button';
import { DayBadges } from '../components/DayBadges';
import { EmbeddablePageHeader } from '../components/EmbeddablePageHeader';
import { ReleaseResourceModal } from '../components/ReleaseResourceModal';
import { TableEmpty, TableError, TableSkeleton } from '../components/TableStates';
import { emitApiErrorToast } from '../api/events';
import { useAuth } from '../auth/useAuth';
import { useDesksByIdsQuery } from '../hooks/useDesks';
import { useEmployeeFixedAssignmentsQuery } from '../hooks/useFixedAssignments';
import { useParkingSpacesByIdsQuery } from '../hooks/useParkingSpaces';
import { groupFixedAssignments, type FixedAssignmentGroup } from '../utils/fixedAssignments';
import type { ResourceType } from '../types/request';

interface ReleaseTarget {
  parkingSpaceId: number;
  spaceLabel: string;
  resourceType: ResourceType;
}

// Vista EMPLOYEE: "mis asignaciones fijas" del usuario autenticado
// (getEmployeeFixedAssignments con su propio id). Cada recurso fijo ofrece la
// accion "Liberar" para una fecha presente o futura (tasks §4.1).
export function MyFixedAssignmentsPage({ embedded = false }: { embedded?: boolean } = {}) {
  const { t } = useTranslation();
  const { user } = useAuth();
  const employeeId = user?.employeeId ?? null;
  const [releaseTarget, setReleaseTarget] = useState<ReleaseTarget | null>(null);

  const query = useEmployeeFixedAssignmentsQuery(employeeId);
  const groups = useMemo(() => groupFixedAssignments(query.data ?? []), [query.data]);
  const ready = !query.isLoading && !query.isError;

  // Numero real del puesto/plaza (GET /desks/{id} y GET /parking-spaces/{id} son
  // accesibles a EMPLOYEE, a diferencia de sus catalogos que son solo ADMIN). Solo
  // se resuelven los recursos presentes en la lista, separados por tipo.
  const deskIds = useMemo(
    () => groups.filter((group) => group.resourceType === 'DESK').map((group) => group.parkingSpaceId),
    [groups],
  );
  const spaceIds = useMemo(
    () => groups.filter((group) => group.resourceType === 'PARKING').map((group) => group.parkingSpaceId),
    [groups],
  );
  const desksById = useDesksByIdsQuery(deskIds);
  const spacesById = useParkingSpacesByIdsQuery(spaceIds);

  // Etiqueta de negocio del recurso (nunca el id interno, spec employee-portal
  // "Liberación de recurso fijo con tipo y etiqueta correctos"): tanto el puesto
  // como la plaza muestran su numero real (GET .../{id} EMPLOYEE-safe).
  function spaceLabel(group: FixedAssignmentGroup): string {
    if (group.resourceType === 'DESK') {
      const desk = desksById[group.parkingSpaceId];
      return desk
        ? t('requests.mine.resourceLabel.desk', { number: desk.number })
        : t('common.loading');
    }
    const space = spacesById[group.parkingSpaceId];
    return space
      ? t('requests.mine.resourceLabel.parking', { number: space.number })
      : t('common.loading');
  }

  function handleReleased(): void {
    setReleaseTarget(null);
    emitApiErrorToast('releases.release.created');
  }

  return (
    <section className="my-fixed-assignments-page" aria-label={t('fixedAssignments.mine.title')}>
      <EmbeddablePageHeader
        embedded={embedded}
        eyebrow={t('fixedAssignments.mine.eyebrow')}
        title={t('fixedAssignments.mine.title')}
        description={t('fixedAssignments.mine.description')}
      />

      {query.isLoading ? <TableSkeleton label={t('common.loading')} columns={3} /> : null}

      {query.isError ? (
        <TableError
          message={t('fixedAssignments.mine.loadError')}
          retryLabel={t('common.retry')}
          onRetry={() => void query.refetch()}
        />
      ) : null}

      {ready ? (
        groups.length === 0 ? (
          <TableEmpty icon="calendar-star" message={t('fixedAssignments.mine.empty')} />
        ) : (
          <div className="table-scroll">
            <table className="table">
              <thead>
                <tr className="table-header">
                  <th scope="col">{t('fixedAssignments.mine.columns.space')}</th>
                  <th scope="col">{t('fixedAssignments.mine.columns.days')}</th>
                  <th scope="col">{t('releases.mine.columns.actions')}</th>
                </tr>
              </thead>
              <tbody>
                {groups.map((group) => (
                  <tr key={group.key} className="table-row">
                    <td>{spaceLabel(group)}</td>
                    <td>
                      <DayBadges days={group.days} />
                    </td>
                    <td className="table-actions">
                      <Button
                        variant="blue"
                        icon="calendar-off"
                        onClick={() =>
                          setReleaseTarget({
                            parkingSpaceId: group.parkingSpaceId,
                            spaceLabel: spaceLabel(group),
                            resourceType: group.resourceType,
                          })
                        }
                      >
                        {t('releases.release.action')}
                      </Button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )
      ) : null}

      {releaseTarget ? (
        <ReleaseResourceModal
          parkingSpaceId={releaseTarget.parkingSpaceId}
          spaceLabel={releaseTarget.spaceLabel}
          resourceType={releaseTarget.resourceType}
          onClose={() => setReleaseTarget(null)}
          onReleased={handleReleased}
        />
      ) : null}
    </section>
  );
}
