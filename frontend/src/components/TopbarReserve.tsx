import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { Button } from './Button';
import { CreateRequestModal } from './CreateRequestModal';
import { ReservationWizard } from './wizard/ReservationWizard';

interface TopbarReserveProps {
  // El asistente completo (crear en nombre de un empleado) es sólo para ADMIN;
  // el resto de roles con reserva propia abren su flujo de solicitud.
  isAdmin: boolean;
}

// CTA "Nueva reserva" del topbar y su superficie modal. ADMIN abre el asistente
// multipaso; EMPLOYEE abre su solicitud propia. Aislar esto de AppShell mantiene
// el shell simple (complejidad cognitiva baja).
export function TopbarReserve({ isAdmin }: TopbarReserveProps) {
  const { t } = useTranslation();
  const [open, setOpen] = useState(false);
  const close = () => setOpen(false);

  return (
    <>
      <Button variant="green" icon="plus" onClick={() => setOpen(true)}>
        {t('layout.nav.newReservation')}
      </Button>
      {open && isAdmin ? <ReservationWizard onClose={close} /> : null}
      {open && !isAdmin ? <CreateRequestModal onClose={close} onCreated={close} /> : null}
    </>
  );
}
