import { useTranslation } from 'react-i18next';
import type { DeskCategory } from '../types/desk';

interface DeskCategoryBadgeProps {
  category: DeskCategory;
}

// Distinción visual de categoría en los listados: EXECUTIVE resalta en azul
// (dirección), STANDARD en gris neutro (init-desks §4.3). El plano llega en floor-plan.
export function DeskCategoryBadge({ category }: DeskCategoryBadgeProps) {
  const { t } = useTranslation();
  const isExecutive = category === 'EXECUTIVE';
  return (
    <span className={`pill ${isExecutive ? 'pill-blue' : 'pill-gray'}`}>
      {t(`desks.category.${category}`)}
    </span>
  );
}
