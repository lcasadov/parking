import { RESOURCE_ICON } from '../../utils/resourceIcon';
import { useTranslation } from 'react-i18next';
import { RESOURCE_DESK, RESOURCE_PARKING } from './wizardTypes';
import type { ResourceType } from '../../types/request';

interface StepResourceTypeProps {
  // Tipos seleccionados (uno o ambos). El paso de ubicación se repite por tipo.
  values: ResourceType[];
  onToggle: (value: ResourceType) => void;
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
    icon: RESOURCE_ICON.DESK,
    titleKey: 'wizard.resource.desk',
    descKey: 'wizard.resource.deskDesc',
    tagIcon: 'map-2',
    tagKey: 'wizard.resource.deskTag',
  },
];

// Paso 1 — Tipo de recurso: dos tarjetas grandes MULTISELECCIONABLES (plaza y/o
// puesto). Elegir ambas añade un paso de ubicación por cada una (tarea 3).
export function StepResourceType({ values, onToggle }: StepResourceTypeProps) {
  const { t } = useTranslation();
  return (
    <div className="rzw-step-body">
      <p className="rzw-lead">{t('wizard.resource.lead')}</p>
      <div className="rzw-choice-grid rzw-choice-2">
        {OPTIONS.map((option) => {
          const selected = values.includes(option.type);
          return (
            <button
              key={option.type}
              type="button"
              className={`rzw-choice-card${selected ? ' is-selected' : ''}`}
              aria-pressed={selected}
              onClick={() => onToggle(option.type)}
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
