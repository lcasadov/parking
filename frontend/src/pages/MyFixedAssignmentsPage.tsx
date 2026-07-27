import { useMemo, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { AbsenceReleaseModal } from '../components/AbsenceReleaseModal';
import { ConfirmDialog } from '../components/ConfirmDialog';
import { DayBadges } from '../components/DayBadges';
import { DeskMapButton } from '../components/DeskMapButton';
import { EmbeddablePageHeader } from '../components/EmbeddablePageHeader';
import { ResourceIcon } from '../components/ResourceIcon';
import { TableEmpty, TableError, TableSkeleton } from '../components/TableStates';
import { useAuth } from '../auth/useAuth';
import { useDesksByIdsQuery } from '../hooks/useDesks';
import { useEmployeeFixedAssignmentsQuery } from '../hooks/useFixedAssignments';
import { useParkingSpacesByIdsQuery } from '../hooks/useParkingSpaces';
import { useCancelRelease, useMyReleasesQuery } from '../hooks/useReleases';
import { useToast } from '../hooks/useToast';
import { mediumDate } from '../utils/calendar';
import { todayIso } from '../utils/requests';
import { groupFixedAssignments, type FixedAssignmentGroup } from '../utils/fixedAssignments';
import type { Release } from '../types/release';

// Vista EMPLOYEE "Mis sitios fijos" (read-only): muestra los recursos fijos del
// usuario (plaza y/o puesto), una fila por recurso con sus días. La liberación
// puntual vive en Mi Semana; aquí la única acción es el botón estrella de
// AUSENCIA (liberar plaza/puesto en varios días de golpe) y "Mapa" en el puesto.
export function MyFixedAssignmentsPage({ embedded = false }: { embedded?: boolean } = {}) {
  const { t, i18n } = useTranslation();
  const toast = useToast();
  const { user } = useAuth();
  const employeeId = user?.employeeId ?? null;
  const [absenceOpen, setAbsenceOpen] = useState(false);
  const [releasedOpen, setReleasedOpen] = useState(true);
  const [undoAllOpen, setUndoAllOpen] = useState(false);

  const query = useEmployeeFixedAssignmentsQuery(employeeId);
  const groups = useMemo(() => groupFixedAssignments(query.data ?? []), [query.data]);
  const ready = !query.isLoading && !query.isError;

  // Días que ya has liberado (hoy o futuro), para que sepas qué marcaste y puedas
  // deshacerlo (recuperar tu fijo ese día).
  const releasesQuery = useMyReleasesQuery({ size: 200 });
  const cancelRelease = useCancelRelease();
  const upcomingReleases = useMemo(
    () =>
      (releasesQuery.data?.content ?? [])
        .filter((release) => release.releaseDate >= todayIso())
        .sort((a, b) => a.releaseDate.localeCompare(b.releaseDate)),
    [releasesQuery.data],
  );

  function undoRelease(id: number): void {
    cancelRelease.mutate(id, {
      onSuccess: () => toast.success('myResources.released.undone'),
      onError: () => toast.error('myResources.released.undoFailed'),
    });
  }

  // Deshacer TODAS las liberaciones próximas de golpe (best-effort), tras confirmar.
  async function undoAllReleases(): Promise<void> {
    const results = await Promise.allSettled(
      upcomingReleases.map((release) => cancelRelease.mutateAsync(release.id)),
    );
    const ok = results.filter((r) => r.status === 'fulfilled').length;
    if (ok > 0) {
      toast.success('myResources.released.undone');
    }
    if (ok === 0 && results.length > 0) {
      toast.error('myResources.released.undoFailed');
    }
    setUndoAllOpen(false);
  }

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

  // Etiqueta de negocio del recurso (nunca el id interno): número real del puesto/plaza.
  function spaceLabel(group: FixedAssignmentGroup): string {
    if (group.resourceType === 'DESK') {
      const desk = desksById[group.parkingSpaceId];
      return desk ? t('requests.mine.resourceLabel.desk', { number: desk.number }) : t('common.loading');
    }
    const space = spacesById[group.parkingSpaceId];
    return space
      ? t('requests.mine.resourceLabel.parking', { number: space.number })
      : t('common.loading');
  }

  // Etiqueta del recurso de una liberación (mismo criterio: número real, nunca id).
  function releaseLabel(release: Release): string {
    if ((release.resourceType ?? 'PARKING') === 'DESK') {
      const desk = desksById[release.parkingSpaceId];
      return desk ? t('requests.mine.resourceLabel.desk', { number: desk.number }) : t('common.loading');
    }
    const space = spacesById[release.parkingSpaceId];
    return space
      ? t('requests.mine.resourceLabel.parking', { number: space.number })
      : t('common.loading');
  }

  return (
    <section className="my-fixed-assignments-page" aria-label={t('fixedAssignments.mine.title')}>
      <EmbeddablePageHeader
        embedded={embedded}
        eyebrow={t('fixedAssignments.mine.eyebrow')}
        title={t('fixedAssignments.mine.title')}
        description={t('fixedAssignments.mine.description')}
      />

      {groups.length > 0 ? (
        <button type="button" className="absence-cta" onClick={() => setAbsenceOpen(true)}>
          <span className="absence-cta-icon" aria-hidden="true">
            <i className="ti ti-beach" />
          </span>
          <span className="absence-cta-text">
            <span className="absence-cta-title">{t('myResources.absence.ctaTitle')}</span>
            <span className="absence-cta-sub">{t('myResources.absence.ctaSub')}</span>
          </span>
          <i className="ti ti-chevron-right absence-cta-arrow" aria-hidden="true" />
        </button>
      ) : null}

      {upcomingReleases.length > 0 ? (
        <section className="released-panel" aria-label={t('myResources.released.title')}>
          <div className="released-panel-head">
            <button
              type="button"
              className="released-panel-toggle"
              aria-expanded={releasedOpen}
              onClick={() => setReleasedOpen((open) => !open)}
            >
              <i
                className={`ti ti-chevron-${releasedOpen ? 'down' : 'right'}`}
                aria-hidden="true"
              />
              <i className="ti ti-calendar-off" aria-hidden="true" />
              {t('myResources.released.title')}
              <span className="released-count">{upcomingReleases.length}</span>
            </button>
            <button
              type="button"
              className="released-item-undo released-undo-all"
              onClick={() => setUndoAllOpen(true)}
              disabled={cancelRelease.isPending}
            >
              <i className="ti ti-arrow-back-up" aria-hidden="true" />
              {t('myResources.released.undoAll')}
            </button>
          </div>
          {releasedOpen ? (
          <ul className="released-list">
            {upcomingReleases.map((release) => (
              <li key={release.id} className="released-item">
                <span className="released-item-info">
                  <ResourceIcon type={release.resourceType ?? 'PARKING'} />
                  <span>
                    {releaseLabel(release)} · {mediumDate(release.releaseDate, i18n.language)}
                  </span>
                </span>
                <button
                  type="button"
                  className="released-item-undo"
                  onClick={() => undoRelease(release.id)}
                  disabled={cancelRelease.isPending}
                >
                  <i className="ti ti-arrow-back-up" aria-hidden="true" />
                  {t('myResources.released.undo')}
                </button>
              </li>
            ))}
          </ul>
          ) : null}
        </section>
      ) : null}

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
          <div className="table-scroll mfa-table">
            <table className="table">
              <thead>
                <tr className="table-header">
                  <th scope="col">{t('fixedAssignments.mine.columns.resource')}</th>
                  <th scope="col">{t('fixedAssignments.mine.columns.days')}</th>
                  <th scope="col" className="sr-only-head">
                    {t('releases.mine.columns.actions')}
                  </th>
                </tr>
              </thead>
              <tbody>
                {groups.map((group) => (
                  <tr key={group.key} className="table-row">
                    <td className="mfa-cell-resource">
                      <span className="mfa-resource">
                        <span className="mfa-resource-icon" aria-hidden="true">
                          <ResourceIcon type={group.resourceType} />
                        </span>
                        {spaceLabel(group)}
                      </span>
                    </td>
                    <td className="mfa-cell-days">
                      <DayBadges days={group.days} />
                    </td>
                    <td className="table-actions mfa-cell-actions">
                      {group.resourceType === 'DESK' ? (
                        <DeskMapButton deskLabel={spaceLabel(group)} date={todayIso()} />
                      ) : null}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )
      ) : null}

      {absenceOpen ? (
        <AbsenceReleaseModal
          groups={groups}
          labelFor={spaceLabel}
          onClose={() => setAbsenceOpen(false)}
          onDone={() => setAbsenceOpen(false)}
        />
      ) : null}

      <ConfirmDialog
        open={undoAllOpen}
        onOpenChange={setUndoAllOpen}
        icon="arrow-back-up"
        title={t('myResources.released.undoAllConfirmTitle')}
        description={t('myResources.released.undoAllConfirmBody', { count: upcomingReleases.length })}
        confirmLabel={t('myResources.released.undoAll')}
        busy={cancelRelease.isPending}
        onConfirm={() => void undoAllReleases()}
      />
    </section>
  );
}
