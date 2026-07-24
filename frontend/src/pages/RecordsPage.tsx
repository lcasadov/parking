import { motion, useReducedMotion } from 'framer-motion';
import { useTranslation } from 'react-i18next';
import { useSearchParams } from 'react-router-dom';
import { Tabs, type TabItem } from '../components/Tabs';
import { DUR, EASE } from '../theme/motion';
import { AuditPage } from './AuditPage';
import { LoginLogsPage } from './LoginLogsPage';

type RecordsTab = 'audit' | 'loginLogs';
const DEFAULT_TAB: RecordsTab = 'audit';

function isRecordsTab(value: string | null): value is RecordsTab {
  return value === 'audit' || value === 'loginLogs';
}

// Destino "Registros" (app-shell spec, fusion de secciones): pestañas Auditoría |
// Accesos que montan las paginas ya existentes tal cual (sin reescribir su
// logica). La pestaña activa se refleja en `?tab=` para que las rutas antiguas
// (/admin/audit, /admin/login-logs) puedan redirigir aqui preseleccionada.
export function RecordsPage() {
  const { t } = useTranslation();
  const reduceMotion = useReducedMotion();
  const [searchParams, setSearchParams] = useSearchParams();
  const requested = searchParams.get('tab');
  const tab: RecordsTab = isRecordsTab(requested) ? requested : DEFAULT_TAB;

  const tabs: TabItem[] = [
    { id: 'audit', label: t('records.tabs.audit') },
    { id: 'loginLogs', label: t('records.tabs.loginLogs') },
  ];

  function handleChange(id: string): void {
    if (isRecordsTab(id)) {
      setSearchParams(id === DEFAULT_TAB ? {} : { tab: id }, { replace: true });
    }
  }

  return (
    <section className="records-page" aria-label={t('records.title')}>
      <Tabs tabs={tabs} active={tab} onChange={handleChange} ariaLabel={t('records.title')} />
      <motion.div
        key={tab}
        className="tab-fade-panel"
        initial={reduceMotion ? { opacity: 1 } : { opacity: 0 }}
        animate={{ opacity: 1 }}
        transition={{ duration: DUR.fast, ease: EASE.standard }}
      >
        {tab === 'loginLogs' ? <LoginLogsPage /> : <AuditPage />}
      </motion.div>
    </section>
  );
}
