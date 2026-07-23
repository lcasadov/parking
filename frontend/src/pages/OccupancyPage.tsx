import { useTranslation } from 'react-i18next';
import { useSearchParams } from 'react-router-dom';
import { Tabs, type TabItem } from '../components/Tabs';
import { AdminCalendarPage } from './AdminCalendarPage';
import { AvailabilityPage } from './AvailabilityPage';

type OccupancyTab = 'weekly' | 'availability';
const DEFAULT_TAB: OccupancyTab = 'weekly';

function isOccupancyTab(value: string | null): value is OccupancyTab {
  return value === 'weekly' || value === 'availability';
}

// Destino "Ocupación" (app-shell spec, fusion de secciones): pestañas Semanal |
// Disponibilidad que montan las paginas ya existentes tal cual (sin reescribir su
// logica). La pestaña activa se refleja en `?tab=` para que las rutas antiguas
// (/admin/calendar, /admin/availability) puedan redirigir aqui preseleccionada.
// Nota: las celdas accionables (asignar/liberar inline) son una fase posterior
// (design.md D2/F2, requiere endpoint de asignacion puntual); aqui la fusion es
// solo de navegacion, sin cambios de contrato.
export function OccupancyPage() {
  const { t } = useTranslation();
  const [searchParams, setSearchParams] = useSearchParams();
  const requested = searchParams.get('tab');
  const tab: OccupancyTab = isOccupancyTab(requested) ? requested : DEFAULT_TAB;

  const tabs: TabItem[] = [
    { id: 'weekly', label: t('occupancy.tabs.weekly') },
    { id: 'availability', label: t('occupancy.tabs.availability') },
  ];

  function handleChange(id: string): void {
    if (isOccupancyTab(id)) {
      setSearchParams(id === DEFAULT_TAB ? {} : { tab: id }, { replace: true });
    }
  }

  return (
    <section className="occupancy-page" aria-label={t('occupancy.title')}>
      <Tabs tabs={tabs} active={tab} onChange={handleChange} ariaLabel={t('occupancy.title')} />
      {tab === 'availability' ? <AvailabilityPage /> : <AdminCalendarPage />}
    </section>
  );
}
