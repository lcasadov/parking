import { useTranslation } from 'react-i18next';
import { longDate } from '../../utils/calendar';
import type { BookingOutcome } from './wizardTypes';

interface StepResultProps {
  outcomes: BookingOutcome[];
  employeeName: string;
}

// Paso final — Resultado por fecha del loop de confirmación. Estado celebratorio
// cuando todas las reservas se crean; resumen mixto ("N creadas · M no disponibles")
// cuando hay fallos parciales, detallando el motivo por fecha.
export function StepResult({ outcomes, employeeName }: StepResultProps) {
  const { t, i18n } = useTranslation();
  const created = outcomes.filter((outcome) => outcome.ok);
  const failed = outcomes.filter((outcome) => !outcome.ok);
  const allOk = failed.length === 0;

  return (
    <div className="rzw-step-body rzw-result">
      <div className={`rzw-result-hero${allOk ? ' is-success' : ' is-mixed'}`}>
        <span className="rzw-result-badge" aria-hidden="true">
          <i className={`ti ti-${allOk ? 'circle-check' : 'alert-triangle'}`} />
        </span>
        <h3 className="rzw-result-title">
          {allOk ? t('wizard.result.successTitle') : t('wizard.result.mixedTitle')}
        </h3>
        <p className="rzw-result-sub">
          {t('wizard.result.summary', {
            created: created.length,
            failed: failed.length,
            name: employeeName,
          })}
        </p>
      </div>

      <ul className="rzw-result-list">
        {outcomes.map((outcome) => (
          <li key={outcome.date} className={`rzw-result-item${outcome.ok ? ' is-ok' : ' is-fail'}`}>
            <i
              className={`ti ti-${outcome.ok ? 'check' : 'x'}`}
              aria-hidden="true"
            />
            <span className="mono">{longDate(outcome.date, i18n.language)}</span>
            <span className="rzw-result-reason">
              {outcome.ok ? t('wizard.result.reasonOk') : t(outcome.reasonKey ?? 'wizard.result.reasonGeneric')}
            </span>
          </li>
        ))}
      </ul>
    </div>
  );
}
