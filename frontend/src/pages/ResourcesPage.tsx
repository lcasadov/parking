import { motion, useReducedMotion } from 'framer-motion';
import { useTranslation } from 'react-i18next';
import { useSearchParams } from 'react-router-dom';
import { PageHeader } from '../components/PageHeader';
import { SectionSwitch, type SectionSwitchItem } from '../components/SectionSwitch';
import { DUR, EASE } from '../theme/motion';
import { DesksPage } from './DesksPage';
import { ParkingSpacesPage } from './ParkingSpacesPage';

type ResourceTab = 'parking' | 'desks';
const DEFAULT_TAB: ResourceTab = 'parking';

function isResourceTab(value: string | null): value is ResourceTab {
  return value === 'parking' || value === 'desks';
}

// Destino "Recursos" (fusion de secciones): cabecera de sección (título arriba) +
// conmutador GRANDE Plazas | Puestos (mismo lenguaje que Ocupación) SIEMPRE debajo
// del título, y la sub-página embebida (sin su propio título). La pestaña activa se
// refleja en `?tab=` para que las rutas antiguas redirijan aqui preseleccionada.
export function ResourcesPage() {
  const { t } = useTranslation();
  const reduceMotion = useReducedMotion();
  const [searchParams, setSearchParams] = useSearchParams();
  const requested = searchParams.get('tab');
  const tab: ResourceTab = isResourceTab(requested) ? requested : DEFAULT_TAB;

  const items: SectionSwitchItem[] = [
    { id: 'parking', label: t('resources.tabs.parking'), icon: 'parking' },
    { id: 'desks', label: t('resources.tabs.desks'), icon: 'armchair' },
  ];

  function handleChange(id: string): void {
    if (isResourceTab(id)) {
      setSearchParams(id === DEFAULT_TAB ? {} : { tab: id }, { replace: true });
    }
  }

  return (
    <section className="resources-page" aria-label={t('resources.title')}>
      <PageHeader
        eyebrow={t('resources.eyebrow')}
        title={t('resources.title')}
        description={t('resources.description')}
      />
      <SectionSwitch items={items} active={tab} onChange={handleChange} ariaLabel={t('resources.title')} />
      <motion.div
        key={tab}
        className="tab-fade-panel"
        initial={reduceMotion ? { opacity: 1 } : { opacity: 0 }}
        animate={{ opacity: 1 }}
        transition={{ duration: DUR.fast, ease: EASE.standard }}
      >
        {tab === 'desks' ? <DesksPage embedded /> : <ParkingSpacesPage embedded />}
      </motion.div>
    </section>
  );
}
