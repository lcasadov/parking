import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { Button } from './Button';
import { Dialog } from './Dialog';
import { FloorPlanFocusModal } from './FloorPlanFocusModal';
import { Spinner } from './Spinner';
import { useFloorPlanQuery } from '../hooks/useFloorPlan';
import { isValidIsoDate } from '../utils/calendar';

// Número de puesto a partir de la etiqueta ("D-05" -> 5), para resolverlo en el plano.
function deskNumberFromLabel(label: string): number | null {
  const match = /\d+/.exec(label);
  return match ? Number(match[0]) : null;
}

interface DeskMapModalProps {
  deskLabel: string;
  date: string;
  onClose: () => void;
}

// Resuelve el puesto en el plano de la fecha (por número) y muestra el focus modal
// con el asiento resaltado y pulsando. Mientras carga muestra un spinner; si no se
// localiza el puesto, un aviso. Se monta solo al abrir (la query corre bajo demanda).
function DeskMapModal({ deskLabel, date, onClose }: DeskMapModalProps) {
  const { t } = useTranslation();
  const valid = isValidIsoDate(date);
  const query = useFloorPlanQuery(date, valid);
  const deskNumber = deskNumberFromLabel(deskLabel);
  const desk = query.data?.desks.find((item) => item.deskNumber === deskNumber) ?? null;

  if (desk) {
    return <FloorPlanFocusModal desk={desk} deskLabel={deskLabel} date={date} onClose={onClose} />;
  }

  return (
    <Dialog
      open
      icon="map-pin"
      title={t('occupancy.weekly.viewInPlanModalTitle', { resource: deskLabel })}
      onOpenChange={(open) => {
        if (!open) {
          onClose();
        }
      }}
      footer={
        <Button variant="white" onClick={onClose}>
          {t('floorPlan.select.close')}
        </Button>
      }
    >
      {query.isLoading ? (
        <Spinner />
      ) : (
        <p className="hint">{t('calendar.myWeek.deskNotFound')}</p>
      )}
    </Dialog>
  );
}

interface DeskMapButtonProps {
  // Etiqueta del puesto asignado ese día ("D-01").
  deskLabel: string;
  date: string;
  className?: string;
}

// Botón "Mapa" para las tarjetas de PUESTO de Mi Semana: abre el plano enfocado en
// el puesto asignado (Feature C). Reutiliza FloorPlanFocusModal (mismo realce/pulso).
export function DeskMapButton({ deskLabel, date, className }: DeskMapButtonProps) {
  const { t } = useTranslation();
  const [open, setOpen] = useState(false);
  return (
    <>
      <Button variant="white" icon="map-pin" className={className} onClick={() => setOpen(true)}>
        {t('calendar.myWeek.viewMap')}
      </Button>
      {open ? <DeskMapModal deskLabel={deskLabel} date={date} onClose={() => setOpen(false)} /> : null}
    </>
  );
}
