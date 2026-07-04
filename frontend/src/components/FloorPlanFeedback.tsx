import { useTranslation } from 'react-i18next';

export type FloorPlanFeedbackKind = 'success' | 'conflict' | null;

// Mensaje de resultado de una solicitud desde el plano (éxito o conflicto).
export function FloorPlanFeedback({ feedback }: { feedback: FloorPlanFeedbackKind }) {
  const { t } = useTranslation();
  if (feedback === null) {
    return null;
  }
  const isSuccess = feedback === 'success';
  return (
    <p
      className={`floor-plan-feedback ${isSuccess ? 'is-success' : 'is-conflict'}`}
      role={isSuccess ? 'status' : 'alert'}
      aria-live="polite"
    >
      {t(isSuccess ? 'floorPlan.requestSuccess' : 'floorPlan.requestConflict')}
    </p>
  );
}
