import { useTranslation } from 'react-i18next';
import { useSearchParams } from 'react-router-dom';
import { SectionSwitch, type SectionSwitchItem } from '../components/SectionSwitch';
import { AuditPage } from './AuditPage';
import { LoginLogsPage } from './LoginLogsPage';

type RecordsTab = 'audit' | 'loginLogs';
const DEFAULT_TAB: RecordsTab = 'audit';

function isRecordsTab(value: string | null): value is RecordsTab {
  return value === 'audit' || value === 'loginLogs';
}

// Destino "Registros" (fusión de secciones): router fino que resuelve la pestaña desde
// `?tab=` y monta la sub-página (Auditoría | Accesos). Cada sub-página es autónoma y usa
// su propio PageFrame (mismo patrón que Recursos/Ocupación): el conmutador va en el
// control-row a la izquierda, Exportar/KPIs a la derecha y los filtros en la subbar. El
// conmutador se construye aquí (dueño del `?tab=`) y se pasa a la sub-página.
export function RecordsPage() {
  const { t } = useTranslation();
  const [searchParams, setSearchParams] = useSearchParams();
  const requested = searchParams.get('tab');
  const tab: RecordsTab = isRecordsTab(requested) ? requested : DEFAULT_TAB;

  const items: SectionSwitchItem[] = [
    { id: 'audit', label: t('records.tabs.audit'), icon: 'clipboard-list' },
    { id: 'loginLogs', label: t('records.tabs.loginLogs'), icon: 'login' },
  ];

  function handleChange(id: string): void {
    if (isRecordsTab(id)) {
      setSearchParams(id === DEFAULT_TAB ? {} : { tab: id }, { replace: true });
    }
  }

  const tabsSwitch = (
    <SectionSwitch
      items={items}
      active={tab}
      onChange={handleChange}
      ariaLabel={t('records.title')}
      className="pf-lead"
    />
  );

  return tab === 'loginLogs' ? (
    <LoginLogsPage tabsSwitch={tabsSwitch} />
  ) : (
    <AuditPage tabsSwitch={tabsSwitch} />
  );
}
