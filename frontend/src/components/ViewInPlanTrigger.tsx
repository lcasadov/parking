import { useState } from 'react';
import * as RadixTooltip from '@radix-ui/react-tooltip';
import { useTranslation } from 'react-i18next';
import { FloorPlanThumbnail } from './FloorPlanThumbnail';
import { FloorPlanFocusModal } from './FloorPlanFocusModal';
import type { FloorPlanDesk } from '../types/floorPlan';

interface ViewInPlanTriggerProps {
  // Puesto resuelto desde el plano por la rejilla (null mientras el plano carga o
  // si el puesto aún no tiene posición conocida). Sin él, el disparador se degrada
  // a un botón inerte (deshabilitado): "Ver en plano" solo aparece en modo DESK.
  desk: FloorPlanDesk | null;
  // Etiqueta legible del puesto ("D-03") para aria-label y título del modal.
  deskLabel: string;
  // Fecha del snapshot del plano (la rejilla pasa HOY).
  date: string;
}

// Disparador "Ver en plano" de una fila de PUESTO (rejilla de Ocupación). HOVER →
// tooltip con un mini-plano (miniatura de la oficina con el puesto marcado); CLICK
// → modal con el plano completo y el puesto resaltado. YA NO navega: reutiliza el
// realce (focusDeskId/pulso) que antes usaba la navegación, ahora dentro del modal.
export function ViewInPlanTrigger({ desk, deskLabel, date }: ViewInPlanTriggerProps) {
  const { t } = useTranslation();
  const [open, setOpen] = useState(false);

  const button = (
    <button
      type="button"
      className="calendar-space-plan"
      onClick={() => setOpen(true)}
      disabled={!desk}
      aria-label={t('occupancy.weekly.viewInPlan', { resource: deskLabel })}
    >
      <i className="ti ti-map-pin" aria-hidden="true" />
      <span>{t('occupancy.weekly.viewInPlanShort')}</span>
    </button>
  );

  // Sin puesto resuelto no hay tooltip ni modal: solo el botón (deshabilitado).
  if (!desk) {
    return button;
  }

  return (
    <>
      <RadixTooltip.Root>
        <RadixTooltip.Trigger asChild>{button}</RadixTooltip.Trigger>
        <RadixTooltip.Portal>
          <RadixTooltip.Content
            className="plan-mini-tip"
            side="right"
            align="start"
            sideOffset={8}
            collisionPadding={12}
          >
            <span className="plan-mini-tip-label">
              {t('occupancy.weekly.viewInPlanTip', { resource: deskLabel })}
            </span>
            <FloorPlanThumbnail desk={desk} />
            <RadixTooltip.Arrow className="plan-mini-tip-arrow" width={12} height={6} />
          </RadixTooltip.Content>
        </RadixTooltip.Portal>
      </RadixTooltip.Root>

      {open ? (
        <FloorPlanFocusModal
          desk={desk}
          deskLabel={deskLabel}
          date={date}
          onClose={() => setOpen(false)}
        />
      ) : null}
    </>
  );
}
