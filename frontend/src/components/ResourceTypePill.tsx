import { useTranslation } from 'react-i18next';
import type { ResourceType } from '../types/request';

interface ResourceTypePillProps {
  resourceType?: ResourceType;
}

// Pill que distingue el tipo de recurso de una solicitud/asignacion: puesto
// (pill-blue) vs plaza (pill-gray, neutro). Default PARKING (retrocompatible).
export function ResourceTypePill({ resourceType }: ResourceTypePillProps) {
  const { t } = useTranslation();
  const type: ResourceType = resourceType ?? 'PARKING';
  const tone = type === 'DESK' ? 'pill-blue' : 'pill-gray';
  return <span className={`pill ${tone}`}>{t(`requests.resourceType.${type}`)}</span>;
}
