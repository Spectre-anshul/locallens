import { Routes, Route, Navigate } from 'react-router-dom';
import { useAuthStore } from './store/authStore';
import Navbar from './components/common/Navbar';
import HomePage from './pages/HomePage';
import LoginPage from './pages/LoginPage';
import RegisterPage from './pages/RegisterPage';
import TripCreatePage from './pages/TripCreatePage';
import DashboardPage from './pages/DashboardPage';
import MapPage from './pages/MapPage';
import ExplorePage from './pages/ExplorePage';
import CreatorDashboardPage from './pages/CreatorDashboardPage';
import NotFoundPage from './pages/NotFoundPage';

function ProtectedRoute({ children }) {
  const isAuthenticated = useAuthStore((s) => s.isAuthenticated);
  return isAuthenticated ? children : <Navigate to="/login" />;
}

export default function App() {
  return (
    <div className="app" data-theme={useAuthStore((s) => s.darkMode) ? 'dark' : 'light'}>
      <Navbar />
      <main>
        <Routes>
          <Route path="/" element={<HomePage />} />
          <Route path="/login" element={<LoginPage />} />
          <Route path="/register" element={<RegisterPage />} />
          <Route path="/trip/new" element={<ProtectedRoute><TripCreatePage /></ProtectedRoute>} />
          <Route path="/trip/:tripId" element={<ProtectedRoute><DashboardPage /></ProtectedRoute>} />
          <Route path="/map" element={<MapPage />} />
          <Route path="/explore" element={<ExplorePage />} />
          <Route path="/creator/dashboard" element={<ProtectedRoute><CreatorDashboardPage /></ProtectedRoute>} />
          <Route path="*" element={<NotFoundPage />} />
        </Routes>
      </main>
    </div>
  );
}
