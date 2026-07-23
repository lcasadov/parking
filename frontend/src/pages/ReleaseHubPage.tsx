import { useTranslation } from 'react-i18next';
import { useSearchParams } from 'react-router-dom';
import { Tabs, type TabItem } from '../components/Tabs';
import { AdministrativeReleasesPage } from './AdministrativeReleasesPage';
import { ReleaseByDatePage } from './ReleaseByDatePage';

type ReleaseHubTab = 'byEmployee' | 'byDate';
const DEFAULT_TAB: ReleaseHubTab = 'byEmployee';

function isReleaseHubTab(value: string | null): value is ReleaseHubTab {
  return value === 'byEmployee' || value === 'byDate';
}

// Destino "Liberar" (app-shell spec, fusion de secciones): pestañas Por empleado |
// Por fecha que montan las paginas ya existentes tal cual (sin reescribir su
// logica; D3 del design: agrupacion de UI, cero cambios de contrato). La pestaña
// activa se refleja en `?tab=` para que las rutas antiguas (/admin/releases,
// /admin/release-by-date) puedan redirigir aqui preseleccionada.
export function ReleaseHubPage() {
  const { t } = useTranslation();
  const [searchParams, setSearchParams] = useSearchParams();
  const requested = searchParams.get('tab');
  const tab: ReleaseHubTab = isReleaseHubTab(requested) ? requested : DEFAULT_TAB;

  const tabs: TabItem[] = [
    { id: 'byEmployee', label: t('releases.hub.tabs.byEmployee') },
    { id: 'byDate', label: t('releases.hub.tabs.byDate') },
  ];

  function handleChange(id: string): void {
    if (isReleaseHubTab(id)) {
      setSearchParams(id === DEFAULT_TAB ? {} : { tab: id }, { replace: true });
    }
  }

  return (
    <section className="release-hub-page" aria-label={t('releases.hub.title')}>
      <Tabs tabs={tabs} active={tab} onChange={handleChange} ariaLabel={t('releases.hub.title')} />
      {tab === 'byDate' ? <ReleaseByDatePage /> : <AdministrativeReleasesPage />}
    </section>
  );
}
