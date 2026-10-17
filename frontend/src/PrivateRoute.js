import { Navigate, Outlet, useLocation } from 'react-router-dom';
import AuthService from './services/AuthService';

export default function PrivateRoute() {
  const location = useLocation();
  const currentUser = AuthService.getCurrentUser();

  return currentUser ? (
    <Outlet />
  ) : (
    <Navigate to="/login" replace state={{ from: location.pathname + location.search }} />
  );
}
