import { useMemo, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { Button } from '../components/Button';
import { InfoBanner } from '../components/InfoBanner';
import { PageHeader } from '../components/PageHeader';
import { AdministrativeReleaseModal } from '../components/AdministrativeReleaseModal';
import { emitApiErrorToast } from '../api/events';
import { useEmployeesQuery } from '../hooks/useEmployees';
import { useParkingSpacesQuery } from '../hooks/useParkingSpaces';

const LOOKUP_SIZE = 100;

// Vista ADMIN: panel de liberacion administrativa. Abre el modal con los
// selectores de empleado, plaza, fecha y motivo obligatorio (tasks §4.3).
export function AdministrativeReleasesPage() {
  const { t } = useTranslation();
  const [isFormOpen, setIsFormOpen] = useState(false);

  const employeesQuery = useEmployeesQuery({ page: 0, size: LOOKUP_SIZE });
  const spacesQuery = useParkingSpacesQuery({ page: 0, size: LOOKUP_SIZE, active: true });

  const employees = useMemo(() => employeesQuery.data?.content ?? [], [employeesQuery.data]);
  const spaces = useMemo(() => spacesQuery.data?.content ?? [], [spacesQuery.data]);

  function handleCreated(): void {
    setIsFormOpen(false);
    emitApiErrorToast('releases.admin.created');
  }

  return (
    <section className="administrative-releases-page" aria-label={t('releases.admin.title')}>
      <PageHeader
        eyebrow={t('releases.admin.eyebrow')}
        title={t('releases.admin.title')}
        description={t('releases.admin.description')}
        actions={
          <Button variant="green" icon="plus" onClick={() => setIsFormOpen(true)}>
            {t('releases.admin.new')}
          </Button>
        }
      />

      <InfoBanner variant="blue" icon="info-circle">
        {t('releases.admin.intro')}
      </InfoBanner>

      {isFormOpen ? (
        <AdministrativeReleaseModal
          employees={employees}
          spaces={spaces}
          onClose={() => setIsFormOpen(false)}
          onCreated={handleCreated}
        />
      ) : null}
    </section>
  );
}
