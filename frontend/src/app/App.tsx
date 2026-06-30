import { BrowserRouter, Route, Routes } from 'react-router-dom';
import HomePage from '../features/home/pages/HomePage';
import PlatformUsersPage from '../features/platform/pages/PlatformUsersPage';

export default function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/" element={<HomePage />} />
        <Route path="/platform/users" element={<PlatformUsersPage />} />
      </Routes>
    </BrowserRouter>
  );
}
