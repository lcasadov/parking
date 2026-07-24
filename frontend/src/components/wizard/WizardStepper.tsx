import { useTranslation } from 'react-i18next';

interface WizardStepperProps {
  // Etiquetas i18n ya resueltas de cada paso, en orden.
  steps: string[];
  current: number;
  // Índice de paso más alto al que se puede saltar directamente ahora mismo
  // (completado, o alcanzable en cadena con los datos actuales). Los pasos
  // por encima de este índice son futuros/no válidos y no son clicables.
  reachable: number;
  // Navega directamente a un paso ya alcanzado (clic en el stepper).
  onStepClick: (index: number) => void;
}

// Barra de progreso del asistente: nodos numerados unidos por una línea que se
// rellena hasta el paso actual. Los pasos ya completados muestran un check; el
// actual se realza con el acento esmeralda. Los nodos alcanzables (completados,
// o cualquiera anterior al actual con datos válidos) son clicables para volver
// directamente a ese paso; los futuros/no válidos quedan inertes.
export function WizardStepper({ steps, current, reachable, onStepClick }: WizardStepperProps) {
  const { t } = useTranslation();
  const progress = steps.length > 1 ? (current / (steps.length - 1)) * 100 : 0;
  return (
    <ol
      className="rzw-stepper"
      aria-label={t('wizard.progressLabel', { current: current + 1, total: steps.length })}
    >
      <span className="rzw-stepper-rail" aria-hidden="true">
        <span className="rzw-stepper-rail-fill" style={{ width: `${progress}%` }} />
      </span>
      {steps.map((label, index) => {
        const done = index < current;
        const active = index === current;
        const state = done ? 'is-done' : active ? 'is-active' : 'is-idle';
        const clickable = index !== current && index <= reachable;
        const dot = (
          <span className="rzw-step-dot" aria-hidden="true">
            {done ? <i className="ti ti-check" /> : index + 1}
          </span>
        );
        return (
          <li key={label} className={`rzw-step ${state}`}>
            {clickable ? (
              <button
                type="button"
                className="rzw-step-btn"
                onClick={() => onStepClick(index)}
                aria-label={t('wizard.nav.goToStep', { label })}
              >
                {dot}
                <span className="rzw-step-label">{label}</span>
              </button>
            ) : (
              <div
                className="rzw-step-btn is-static"
                aria-current={active ? 'step' : undefined}
              >
                {dot}
                <span className="rzw-step-label">{label}</span>
              </div>
            )}
          </li>
        );
      })}
    </ol>
  );
}
