import { useTranslation } from 'react-i18next';
import { LANGUAGE_STORAGE_KEY, SUPPORTED_LANGUAGES } from '../i18n';

// Segmented control ES/EN. Persiste la preferencia en localStorage.
export function LanguageToggle() {
  const { i18n, t } = useTranslation();
  const current = i18n.language.startsWith('en') ? 'en' : 'es';

  function changeLanguage(lang: string) {
    void i18n.changeLanguage(lang);
    localStorage.setItem(LANGUAGE_STORAGE_KEY, lang);
  }

  return (
    <div className="segmented" role="group" aria-label={t('common.language')}>
      {SUPPORTED_LANGUAGES.map((lang) => (
        <button
          key={lang}
          type="button"
          className={current === lang ? 'active' : ''}
          aria-pressed={current === lang}
          onClick={() => changeLanguage(lang)}
        >
          {lang.toUpperCase()}
        </button>
      ))}
    </div>
  );
}
