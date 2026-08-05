import { RESOURCE_ICON } from '../utils/resourceIcon';
import { useTranslation } from 'react-i18next';
import { useSearchParams } from 'react-router-dom';
import { SectionSwitch, type SectionSwitchItem } from '../components/SectionSwitch';
import { DesksPage } from './DesksPage';
import { ParkingSpacesPage } from './ParkingSpacesPage';

type ResourceTab = 'parking' | 'desks';
const DEFAULT_TAB: ResourceTab = 'parking';

function isResourceTab(value: string | null): value is ResourceTab {
  return value === 'parking' || value === 'desks';
}

// Destino "Recursos" (fusión de secciones): router fino que resuelve la pestaña desde
// `?tab=` y monta la sub-página (Plazas | Puestos). Cada sub-página es autónoma y usa su
// propio PageFrame (mismo patrón que Ocupación): el conmutador va en el control-row a la
// izquierda, la tira de KPIs a la derecha, "Nuevo…" en la cabecera y los filtros en la
// subbar. El conmutador se construye aquí (dueño del `?tab=`) y se pasa a la sub-página.
export function ResourcesPage() {
  const { t } = useTranslation();
  const [searchParams, setSearchParams] = useSearchParams();
  const requested = searchParams.get('tab');
  const tab: ResourceTab = isResourceTab(requested) ? requested : DEFAULT_TAB;

  const items: SectionSwitchItem[] = [
    { id: 'parking', label: t('resources.tabs.parking'), icon: RESOURCE_ICON.PARKING },
    { id: 'desks', label: t('resources.tabs.desks'), icon: RESOURCE_ICON.DESK },
  ];

  function handleChange(id: string): void {
    if (isResourceTab(id)) {
      setSearchParams(id === DEFAULT_TAB ? {} : { tab: id }, { replace: true });
    }
  }

  const tabsSwitch = (
    <SectionSwitch
      items={items}
      active={tab}
      onChange={handleChange}
      ariaLabel={t('resources.title')}
      className="pf-lead"
    />
  );

  return tab === 'desks' ? (
    <DesksPage tabsSwitch={tabsSwitch} />
  ) : (
    <ParkingSpacesPage tabsSwitch={tabsSwitch} />
  );
}
