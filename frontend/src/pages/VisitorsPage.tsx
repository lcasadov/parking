import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { Button } from '../components/Button';
import { PageFrame } from '../components/PageFrame';
import { SearchBox } from '../components/SearchBox';
import { SectionSwitch, type SectionSwitchItem } from '../components/SectionSwitch';
import { VisitorFormModal } from '../components/VisitorFormModal';
import { VisitorReservationsPanel } from '../components/VisitorReservationsPanel';
import { VisitorsPanel } from '../components/VisitorsPanel';
import { ReservationWizard } from '../components/wizard/ReservationWizard';
import type { Visitor } from '../types/visitor';

type Tab = 'visitors' | 'reservations';

// Vista ADMIN de visitantes: sub-vistas "Fichas de visitante" / "Reservas futuras"
// (docs/ui-screens.md §17) con el conmutador GRANDE anclado a la izquierda y, en la
// pestaña de fichas, el buscador + "Nuevo visitante" a la derecha en la MISMA fila.
// El guard de rol vive en ProtectedRoute (ADMIN).
export function VisitorsPage() {
  const { t } = useTranslation();
  const [tab, setTab] = useState<Tab>('visitors');
  // El buscador y el alta/edición se izan al control-row para compartir la fila con las
  // pestañas; el modal de ficha lo renderiza el hub (lo abren el botón "Nuevo" y "Editar").
  const [q, setQ] = useState('');
  const [formVisitor, setFormVisitor] = useState<Visitor | null>(null);
  const [isFormOpen, setIsFormOpen] = useState(false);
  const [isReservationOpen, setIsReservationOpen] = useState(false);

  const items: SectionSwitchItem[] = [
    { id: 'visitors', label: t('visitors.tabs.visitors'), icon: 'user' },
    { id: 'reservations', label: t('visitors.tabs.reservations'), icon: 'calendar-event' },
  ];

  function openCreate(): void {
    setFormVisitor(null);
    setIsFormOpen(true);
  }

  function openEdit(visitor: Visitor): void {
    setFormVisitor(visitor);
    setIsFormOpen(true);
  }

  function closeForm(): void {
    setIsFormOpen(false);
    setFormVisitor(null);
  }

  return (
    <PageFrame
      eyebrow={t('visitors.eyebrow')}
      title={t('visitors.title')}
      bodyLabel={t('visitors.title')}
      resourceSelector={
        <SectionSwitch
          items={items}
          active={tab}
          onChange={(id) => setTab(id as Tab)}
          ariaLabel={t('visitors.title')}
          className="pf-lead"
        />
      }
      toolbar={
        tab === 'visitors' ? (
          <>
            <SearchBox
              label={t('visitors.searchLabel')}
              placeholder={t('visitors.searchPlaceholder')}
              value={q}
              onValueChange={setQ}
            />
            <Button variant="green" icon="plus" onClick={openCreate}>
              {t('visitors.newVisitor')}
            </Button>
          </>
        ) : (
          <Button variant="green" icon="calendar-plus" onClick={() => setIsReservationOpen(true)}>
            {t('visitors.newReservation')}
          </Button>
        )
      }
    >
      <div role="tabpanel" aria-labelledby={`tab-${tab}`}>
        {tab === 'visitors' ? (
          <VisitorsPanel q={q} onCreate={openCreate} onEdit={openEdit} />
        ) : (
          <VisitorReservationsPanel onCreate={() => setIsReservationOpen(true)} />
        )}
      </div>

      {isFormOpen ? (
        <VisitorFormModal visitor={formVisitor} onClose={closeForm} onSaved={closeForm} />
      ) : null}

      {isReservationOpen ? (
        <ReservationWizard
          initialBeneficiaryType="VISITOR"
          onClose={() => setIsReservationOpen(false)}
        />
      ) : null}
    </PageFrame>
  );
}
