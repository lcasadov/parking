import { StrictMode } from 'react';
import { createRoot } from 'react-dom/client';
import '@fontsource-variable/geist/index.css';
import '@fontsource-variable/geist-mono/index.css';
import '@tabler/icons-webfont/dist/tabler-icons.min.css';
import './styles/tokens.css';
import './styles/base.css';
import './styles/components.css';
import './styles/wayfinding.css';
import './i18n';
import { App } from './App';

const container = document.getElementById('root');
if (!container) {
  throw new Error('Root container #root not found');
}

createRoot(container).render(
  <StrictMode>
    <App />
  </StrictMode>,
);
