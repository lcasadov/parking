import { useTranslation } from 'react-i18next';
import { longDate } from '../../utils/calendar';
import { RESOURCE_DESK } from './wizardTypes';
import type { BookingOutcome } from './wizardTypes';

interface StepResultProps {
  outcomes: BookingOutcome[];
  employeeName: string;
  // Visitante: no se notifica por email, así que el resumen no menciona notificación.
  isVisitor?: boolean;
}

// Paso final — Resultado por fecha del loop de confirmación. Estado celebratorio
// cuando todas las reservas se crean; resumen mixto ("N creadas · M no disponibles")
// cuando hay fallos parciales, detallando el motivo por fecha.
export function StepResult({ outcomes, employeeName, isVisitor = false }: StepResultProps) {
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
          {t(isVisitor ? 'wizard.result.summaryVisitor' : 'wizard.result.summary', {
            created: created.length,
            failed: failed.length,
            name: employeeName,
          })}
        </p>
      </div>

      <ul className="rzw-result-list">
        {outcomes.map((outcome) => (
          <li
            key={`${outcome.resourceType ?? ''}-${outcome.date}`}
            className={`rzw-result-item${outcome.ok ? ' is-ok' : ' is-fail'}`}
          >
            <i
              className={`ti ti-${outcome.ok ? 'check' : 'x'}`}
              aria-hidden="true"
            />
            <span className="mono">{longDate(outcome.date, i18n.language)}</span>
            {outcome.resourceType ? (
              <span className="rzw-result-type">
                {t(outcome.resourceType === RESOURCE_DESK ? 'wizard.resource.desk' : 'wizard.resource.parking')}
              </span>
            ) : null}
            <span className="rzw-result-reason">
              {outcome.ok ? t('wizard.result.reasonOk') : t(outcome.reasonKey ?? 'wizard.result.reasonGeneric')}
            </span>
          </li>
        ))}
      </ul>
    </div>
  );
}
