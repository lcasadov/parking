import { useTranslation } from 'react-i18next';
import { useSearchParams } from 'react-router-dom';
import { Tabs, type TabItem } from '../components/Tabs';
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
      {tab === 'loginLogs' ? <LoginLogsPage /> : <AuditPage />}
    </section>
  );
}
