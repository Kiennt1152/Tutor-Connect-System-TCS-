import { StrictMode } from 'react';
import { createRoot } from 'react-dom/client';
import './shared/theme/tokens.css';
import './shared/theme/buttons.css';
import './shared/theme/layout.css';
import './shared/theme/forms.css';
import './shared/theme/components.css';
import App from './app/App';
import { AppProvider } from './app/providers/AppProvider';

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <AppProvider>
      <App />
    </AppProvider>
  </StrictMode>,
);
