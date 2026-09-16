import { Navigate, Outlet } from 'react-router-dom';
import AuthService from './services/AuthService';

export default function PrivateRoute() {
  const currentUser = AuthService.getCurrentUser();

  return currentUser ? <Outlet /> : <Navigate to="/login" replace />;
}
