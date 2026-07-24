import { useTranslation } from 'react-i18next';

interface WizardStepperProps {
  // Etiquetas i18n ya resueltas de cada paso, en orden.
  steps: string[];
  current: number;
}

// Barra de progreso del asistente: nodos numerados unidos por una línea que se
// rellena hasta el paso actual. Los pasos ya completados muestran un check; el
// actual se realza con el acento esmeralda. Puramente presentacional.
export function WizardStepper({ steps, current }: WizardStepperProps) {
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
        return (
          <li key={label} className={`rzw-step ${state}`}>
            <span className="rzw-step-dot" aria-hidden="true">
              {done ? <i className="ti ti-check" /> : index + 1}
            </span>
            <span className="rzw-step-label">{label}</span>
          </li>
        );
      })}
    </ol>
  );
}
