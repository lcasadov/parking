import { useTranslation } from 'react-i18next';
import { Button } from './Button';
import { Modal } from './Modal';
import { longDate } from '../utils/calendar';
import type { FloorPlanDesk } from '../types/floorPlan';

interface RequestDeskConfirmModalProps {
  desk: FloorPlanDesk;
  date: string;
  onConfirm: () => void;
  onClose: () => void;
}

// Confirmacion explicita antes de crear una solicitud de puesto desde el plano
// (marcador o boton "Solicitar" de la lista movil): evita altas accidentales por
// toques/zoom/desplazamiento en pantallas tactiles (requests spec, "Confirmacion
// al solicitar un puesto desde el plano").
export function RequestDeskConfirmModal({
  desk,
  date,
  onConfirm,
  onClose,
}: RequestDeskConfirmModalProps) {
  const { t, i18n } = useTranslation();

  const footer = (
    <>
      <Button variant="white" onClick={onClose}>
        {t('floorPlan.confirmRequest.cancel')}
      </Button>
      <Button variant="green" icon="calendar-plus" onClick={onConfirm}>
        {t('floorPlan.confirmRequest.confirm')}
      </Button>
    </>
  );

  return (
    <Modal title={t('floorPlan.confirmRequest.title')} onClose={onClose} footer={footer}>
      <p>
        {t('floorPlan.confirmRequest.body', {
          number: desk.deskNumber,
          date: longDate(date, i18n.language),
        })}
      </p>
    </Modal>
  );
}
