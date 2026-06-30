import { BrowserRouter } from 'react-router-dom';
import AppShell from './AppShell';
import AppRouter from './AppRouter';

export default function App() {
  return (
    <BrowserRouter>
      <AppShell>
        <AppRouter />
      </AppShell>
    </BrowserRouter>
  );
}