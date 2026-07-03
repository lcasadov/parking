import { useTranslation } from 'react-i18next';

export function Spinner({ label }: { label?: string }) {
  const { t } = useTranslation();
  const text = label ?? t('common.loading');
  return (
    <output className="spinner" aria-live="polite">
      <span className="spinner-dot" aria-hidden="true" />
      <span className="spinner-label">{text}</span>
    </output>
  );
}
