import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { VisitorReservationsPanel } from '../components/VisitorReservationsPanel';
import { VisitorsPanel } from '../components/VisitorsPanel';

type Tab = 'visitors' | 'reservations';

// Vista ADMIN de visitantes: pestañas "Fichas de visitante" / "Reservas futuras"
// (docs/ui-screens.md §17). El guard de rol vive en ProtectedRoute (ADMIN).
export function VisitorsPage() {
  const { t } = useTranslation();
  const [tab, setTab] = useState<Tab>('visitors');

  return (
    <section className="visitors-page" aria-labelledby="visitors-title">
      <header className="page-header">
        <h1 id="visitors-title" className="section-title">
          {t('visitors.title')}
        </h1>
      </header>

      <p className="hint">{t('visitors.emailNote')}</p>

      <div className="tabs" role="tablist" aria-label={t('visitors.title')}>
        <button
          type="button"
          role="tab"
          id="visitors-tab-visitors"
          aria-selected={tab === 'visitors'}
          className={`tab${tab === 'visitors' ? ' active' : ''}`}
          onClick={() => setTab('visitors')}
        >
          {t('visitors.tabs.visitors')}
        </button>
        <button
          type="button"
          role="tab"
          id="visitors-tab-reservations"
          aria-selected={tab === 'reservations'}
          className={`tab${tab === 'reservations' ? ' active' : ''}`}
          onClick={() => setTab('reservations')}
        >
          {t('visitors.tabs.reservations')}
        </button>
      </div>

      <div role="tabpanel" aria-labelledby={`visitors-tab-${tab}`}>
        {tab === 'visitors' ? <VisitorsPanel /> : <VisitorReservationsPanel />}
      </div>
    </section>
  );
}
