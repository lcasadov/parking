import { motion, useReducedMotion } from 'framer-motion';
import { useTranslation } from 'react-i18next';
import { useSearchParams } from 'react-router-dom';
import { Tabs, type TabItem } from '../components/Tabs';
import { DUR, EASE } from '../theme/motion';
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
  const reduceMotion = useReducedMotion();
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
      {/* Fundido de entrada al cambiar de pestaña (sin desplazamiento): el
          contenido de cada pestaña ya se pinta completo tal cual, solo se
          envuelve en un fade corto para suavizar el cambio. */}
      <motion.div
        key={tab}
        className="my-resources-tabpanel"
        initial={reduceMotion ? { opacity: 1 } : { opacity: 0 }}
        animate={{ opacity: 1 }}
        transition={{ duration: DUR.fast, ease: EASE.standard }}
      >
        {tab === 'releases' ? <MyReleasesPage /> : <MyFixedAssignmentsPage />}
      </motion.div>
    </section>
  );
}
