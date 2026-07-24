import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { InfoBanner } from '../components/InfoBanner';
import { PageHeader } from '../components/PageHeader';
import { SectionSwitch, type SectionSwitchItem } from '../components/SectionSwitch';
import { VisitorReservationsPanel } from '../components/VisitorReservationsPanel';
import { VisitorsPanel } from '../components/VisitorsPanel';

type Tab = 'visitors' | 'reservations';

// Vista ADMIN de visitantes: sub-vistas "Fichas de visitante" / "Reservas futuras"
// (docs/ui-screens.md §17) con el mismo conmutador GRANDE que el resto de secciones.
// El guard de rol vive en ProtectedRoute (ADMIN).
export function VisitorsPage() {
  const { t } = useTranslation();
  const [tab, setTab] = useState<Tab>('visitors');

  const items: SectionSwitchItem[] = [
    { id: 'visitors', label: t('visitors.tabs.visitors'), icon: 'user' },
    { id: 'reservations', label: t('visitors.tabs.reservations'), icon: 'calendar-event' },
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

      <SectionSwitch items={items} active={tab} onChange={(id) => setTab(id as Tab)} ariaLabel={t('visitors.title')} />

      <div role="tabpanel" aria-labelledby={`tab-${tab}`}>
        {tab === 'visitors' ? <VisitorsPanel /> : <VisitorReservationsPanel />}
      </div>
    </section>
  );
}
