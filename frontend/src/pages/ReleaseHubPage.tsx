import { type ReactNode } from 'react';
import { useTranslation } from 'react-i18next';
import { useSearchParams } from 'react-router-dom';
import { Tabs, type TabItem } from '../components/Tabs';
import { AdministrativeReleasesPage } from './AdministrativeReleasesPage';
import { ReleaseByDatePage } from './ReleaseByDatePage';
import { MyAdministrativeReleasesPage } from './MyAdministrativeReleasesPage';

type ReleaseHubTab = 'byEmployee' | 'byDate' | 'history';
const DEFAULT_TAB: ReleaseHubTab = 'byEmployee';

function isReleaseHubTab(value: string | null): value is ReleaseHubTab {
  return value === 'byEmployee' || value === 'byDate' || value === 'history';
}

// Destino "Liberar" (app-shell spec, fusion de secciones): pestañas Por empleado |
// Por fecha | Historial, que montan las paginas ya existentes tal cual (D3: agrupacion
// de UI, cero cambios de contrato). Disponible para ADMIN y AGENCIA (design §D5;
// AGENCIA se orienta con "Historial" = sus propias liberaciones administrativas). La
// pestaña activa se refleja en `?tab=` para que las rutas antiguas redirijan aqui.
export function ReleaseHubPage() {
  const { t } = useTranslation();
  const [searchParams, setSearchParams] = useSearchParams();
  const requested = searchParams.get('tab');
  const tab: ReleaseHubTab = isReleaseHubTab(requested) ? requested : DEFAULT_TAB;

  const tabs: TabItem[] = [
    { id: 'byEmployee', label: t('releases.hub.tabs.byEmployee') },
    { id: 'byDate', label: t('releases.hub.tabs.byDate') },
    { id: 'history', label: t('releases.hub.tabs.history') },
  ];

  function handleChange(id: string): void {
    if (isReleaseHubTab(id)) {
      setSearchParams(id === DEFAULT_TAB ? {} : { tab: id }, { replace: true });
    }
  }

  function renderTab(): ReactNode {
    if (tab === 'byDate') {
      return <ReleaseByDatePage />;
    }
    if (tab === 'history') {
      return <MyAdministrativeReleasesPage />;
    }
    return <AdministrativeReleasesPage />;
  }

  return (
    <section className="release-hub-page" aria-label={t('releases.hub.title')}>
      <Tabs tabs={tabs} active={tab} onChange={handleChange} ariaLabel={t('releases.hub.title')} />
      {renderTab()}
    </section>
  );
}
