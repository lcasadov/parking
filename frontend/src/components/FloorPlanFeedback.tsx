import { useTranslation } from 'react-i18next';

export type FloorPlanFeedbackKind = 'success' | 'conflict' | 'saved' | 'saveError' | null;

// Clave i18n del mensaje según el tipo de feedback (sin literales repetidos).
const FEEDBACK_KEY: Record<'success' | 'conflict' | 'saved' | 'saveError', string> = {
  success: 'floorPlan.requestSuccess',
  conflict: 'floorPlan.requestConflict',
  saved: 'floorPlan.positionsSaved',
  saveError: 'floorPlan.saveError',
};

// Tipos de feedback que representan un fallo (tono de alerta) frente a los de éxito.
const ERROR_KINDS: FloorPlanFeedbackKind[] = ['conflict', 'saveError'];

// Mensaje de resultado de una acción del plano: solicitud (éxito/conflicto) o
// guardado de posiciones (editor). `saved` reutiliza el tono de éxito; `saveError`
// el de alerta.
export function FloorPlanFeedback({ feedback }: { feedback: FloorPlanFeedbackKind }) {
  const { t } = useTranslation();
  if (feedback === null) {
    return null;
  }
  const isSuccess = !ERROR_KINDS.includes(feedback);
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
