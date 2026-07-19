import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { InfoBanner } from '../components/InfoBanner';
import { PageHeader } from '../components/PageHeader';
import { Tabs, type TabItem } from '../components/Tabs';
import { VisitorReservationsPanel } from '../components/VisitorReservationsPanel';
import { VisitorsPanel } from '../components/VisitorsPanel';

type Tab = 'visitors' | 'reservations';

// Vista ADMIN de visitantes: pestañas "Fichas de visitante" / "Reservas futuras"
// (docs/ui-screens.md §17). El guard de rol vive en ProtectedRoute (ADMIN).
export function VisitorsPage() {
  const { t } = useTranslation();
  const [tab, setTab] = useState<Tab>('visitors');

  const tabs: TabItem[] = [
    { id: 'visitors', label: t('visitors.tabs.visitors') },
    { id: 'reservations', label: t('visitors.tabs.reservations') },
  ];

  return (
    <section className="visitors-page" aria-label={t('visitors.title')}>
      <PageHeader
        eyebrow={t('visitors.eyebrow')}
        title={t('visitors.title')}
        description={t('visitors.description')}
      />

      <InfoBanner variant="blue" icon="info-circle">
        {t('visitors.emailNote')}
      </InfoBanner>

      <Tabs
        tabs={tabs}
        active={tab}
        onChange={(id) => setTab(id as Tab)}
        ariaLabel={t('visitors.title')}
      />

      <div role="tabpanel" aria-labelledby={`tab-${tab}`}>
        {tab === 'visitors' ? <VisitorsPanel /> : <VisitorReservationsPanel />}
      </div>
    </section>
  );
}
