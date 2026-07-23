import { useTranslation } from 'react-i18next';
import { useSearchParams } from 'react-router-dom';
import { Tabs, type TabItem } from '../components/Tabs';
import { MyFixedAssignmentsPage } from './MyFixedAssignmentsPage';
import { MyReleasesPage } from './MyReleasesPage';

type MyResourcesTab = 'fixed' | 'releases';
const DEFAULT_TAB: MyResourcesTab = 'fixed';

function isMyResourcesTab(value: string | null): value is MyResourcesTab {
  return value === 'fixed' || value === 'releases';
}

// Destino EMPLOYEE "Mis plazas" (employee-portal spec, fusion de asignaciones
// fijas y liberaciones): pestañas Asignaciones fijas | Liberaciones que montan
// las paginas ya existentes tal cual (sin reescribir su logica). La pestaña
// activa se refleja en `?tab=` para que las rutas antiguas
// (/employee/fixed-assignments, /employee/releases) puedan redirigir aqui
// preseleccionada.
export function MyResourcesPage() {
  const { t } = useTranslation();
  const [searchParams, setSearchParams] = useSearchParams();
  const requested = searchParams.get('tab');
  const tab: MyResourcesTab = isMyResourcesTab(requested) ? requested : DEFAULT_TAB;

  const tabs: TabItem[] = [
    { id: 'fixed', label: t('myResources.tabs.fixed') },
    { id: 'releases', label: t('myResources.tabs.releases') },
  ];

  function handleChange(id: string): void {
    if (isMyResourcesTab(id)) {
      setSearchParams(id === DEFAULT_TAB ? {} : { tab: id }, { replace: true });
    }
  }

  return (
    <section className="my-resources-page" aria-label={t('myResources.title')}>
      <Tabs tabs={tabs} active={tab} onChange={handleChange} ariaLabel={t('myResources.title')} />
      {tab === 'releases' ? <MyReleasesPage /> : <MyFixedAssignmentsPage />}
    </section>
  );
}
