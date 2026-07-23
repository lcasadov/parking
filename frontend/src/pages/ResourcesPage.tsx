import { useTranslation } from 'react-i18next';
import { useSearchParams } from 'react-router-dom';
import { Tabs, type TabItem } from '../components/Tabs';
import { DesksPage } from './DesksPage';
import { ParkingSpacesPage } from './ParkingSpacesPage';

type ResourceTab = 'parking' | 'desks';
const DEFAULT_TAB: ResourceTab = 'parking';

function isResourceTab(value: string | null): value is ResourceTab {
  return value === 'parking' || value === 'desks';
}

// Destino "Recursos" (app-shell spec, fusion de secciones): pestañas Plazas |
// Puestos que montan las paginas ya existentes tal cual (sin reescribir su
// logica). La pestaña activa se refleja en `?tab=` para que las rutas antiguas
// (/admin/parking-spaces, /admin/desks) puedan redirigir aqui preseleccionada.
export function ResourcesPage() {
  const { t } = useTranslation();
  const [searchParams, setSearchParams] = useSearchParams();
  const requested = searchParams.get('tab');
  const tab: ResourceTab = isResourceTab(requested) ? requested : DEFAULT_TAB;

  const tabs: TabItem[] = [
    { id: 'parking', label: t('resources.tabs.parking') },
    { id: 'desks', label: t('resources.tabs.desks') },
  ];

  function handleChange(id: string): void {
    if (isResourceTab(id)) {
      setSearchParams(id === DEFAULT_TAB ? {} : { tab: id }, { replace: true });
    }
  }

  return (
    <section className="resources-page" aria-label={t('resources.title')}>
      <Tabs tabs={tabs} active={tab} onChange={handleChange} ariaLabel={t('resources.title')} />
      {tab === 'desks' ? <DesksPage /> : <ParkingSpacesPage />}
    </section>
  );
}
