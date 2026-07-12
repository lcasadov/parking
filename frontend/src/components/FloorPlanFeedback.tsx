import { useTranslation } from 'react-i18next';

export type FloorPlanFeedbackKind = 'success' | 'conflict' | 'saved' | null;

// Clave i18n del mensaje según el tipo de feedback (sin literales repetidos).
const FEEDBACK_KEY: Record<'success' | 'conflict' | 'saved', string> = {
  success: 'floorPlan.requestSuccess',
  conflict: 'floorPlan.requestConflict',
  saved: 'floorPlan.positionsSaved',
};

// Mensaje de resultado de una acción del plano: solicitud (éxito/conflicto) o
// guardado explícito de posiciones (editor). `saved` reutiliza el tono de éxito.
export function FloorPlanFeedback({ feedback }: { feedback: FloorPlanFeedbackKind }) {
  const { t } = useTranslation();
  if (feedback === null) {
    return null;
  }
  const isSuccess = feedback !== 'conflict';
  return (
    <p
      className={`floor-plan-feedback ${isSuccess ? 'is-success' : 'is-conflict'}`}
      role={isSuccess ? 'status' : 'alert'}
      aria-live="polite"
    >
      {t(FEEDBACK_KEY[feedback])}
    </p>
  );
}
