import i18n from 'i18next';
import { initReactI18next } from 'react-i18next';
import { es } from './locales/es';
import { en } from './locales/en';

export const LANGUAGE_STORAGE_KEY = 'parking.language';
export const SUPPORTED_LANGUAGES = ['es', 'en'] as const;
export type SupportedLanguage = (typeof SUPPORTED_LANGUAGES)[number];

function resolveInitialLanguage(): SupportedLanguage {
  const stored = localStorage.getItem(LANGUAGE_STORAGE_KEY);
  if (stored === 'es' || stored === 'en') {
    return stored;
  }
  return 'es';
}

void i18n.use(initReactI18next).init({
  resources: {
    es: { translation: es },
    en: { translation: en },
  },
  lng: resolveInitialLanguage(),
  fallbackLng: 'es',
  interpolation: { escapeValue: false },
});

export default i18n;
