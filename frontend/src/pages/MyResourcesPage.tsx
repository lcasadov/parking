import { useTranslation } from 'react-i18next';
import { PageHeader } from '../components/PageHeader';
import { MyFixedAssignmentsPage } from './MyFixedAssignmentsPage';

// Destino EMPLOYEE "Mis sitios fijos": vista informativa (read-only) de los recursos
// fijos del empleado + acción de ausencia. Se eliminó la pestaña "Liberaciones"
// (decisión de producto: no aporta al empleado); la liberación puntual vive en Mi
// Semana y la liberación por ausencia en el botón estrella de esta página.
export function MyResourcesPage() {
  const { t } = useTranslation();
  return (
    <section className="my-resources-page" aria-label={t('myResources.title')}>
      <PageHeader
        eyebrow={t('myResources.eyebrow')}
        title={t('myResources.title')}
        description={t('myResources.description')}
      />
      <MyFixedAssignmentsPage embedded />
    </section>
  );
}
