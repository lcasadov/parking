import { useTranslation } from 'react-i18next';
import { RESOURCE_DESK, RESOURCE_PARKING } from './wizardTypes';
import type { ResourceType } from '../../types/request';

interface StepResourceTypeProps {
  value: ResourceType | null;
  onChange: (value: ResourceType) => void;
}

interface Option {
  type: ResourceType;
  icon: string;
  titleKey: string;
  descKey: string;
  tagIcon: string;
  tagKey: string;
}

const OPTIONS: Option[] = [
  {
    type: RESOURCE_PARKING,
    icon: 'car',
    titleKey: 'wizard.resource.parking',
    descKey: 'wizard.resource.parkingDesc',
    tagIcon: 'steering-wheel',
    tagKey: 'wizard.resource.parkingTag',
  },
  {
    type: RESOURCE_DESK,
    icon: 'armchair',
    titleKey: 'wizard.resource.desk',
    descKey: 'wizard.resource.deskDesc',
    tagIcon: 'map-2',
    tagKey: 'wizard.resource.deskTag',
  },
];

// Paso 1 — Tipo de recurso: dos tarjetas grandes seleccionables (plaza / puesto).
export function StepResourceType({ value, onChange }: StepResourceTypeProps) {
  const { t } = useTranslation();
  return (
    <div className="rzw-step-body">
      <p className="rzw-lead">{t('wizard.resource.lead')}</p>
      <div className="rzw-choice-grid rzw-choice-2">
        {OPTIONS.map((option) => {
          const selected = value === option.type;
          return (
            <button
              key={option.type}
              type="button"
              className={`rzw-choice-card${selected ? ' is-selected' : ''}`}
              aria-pressed={selected}
              onClick={() => onChange(option.type)}
            >
              <span className="rzw-choice-icon" aria-hidden="true">
                <i className={`ti ti-${option.icon}`} />
              </span>
              <span className="rzw-choice-title">{t(option.titleKey)}</span>
              <span className="rzw-choice-desc">{t(option.descKey)}</span>
              <span className="rzw-choice-tag">
                <i className={`ti ti-${option.tagIcon}`} aria-hidden="true" />
                {t(option.tagKey)}
              </span>
              <span className="rzw-choice-check" aria-hidden="true">
                <i className="ti ti-check" />
              </span>
            </button>
          );
        })}
      </div>
    </div>
  );
}
