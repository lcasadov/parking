import { RESOURCE_ICON } from '../utils/resourceIcon';
import type { ResourceType } from '../types/request';

// El nombre del icono vive en utils/resourceIcon (fuente única). Este componente
// solo lo pinta.
export function ResourceIcon({ type }: { type: ResourceType }) {
  return <i className={`ti ti-${RESOURCE_ICON[type]}`} aria-hidden="true" />;
}
