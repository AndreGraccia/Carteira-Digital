import { Navigate, Outlet } from 'react-router-dom';
import PropTypes from 'prop-types';
import AuthService from './services/AuthService';

export default function ProtectedRoute({ roles = [] }) {
  const currentUser = AuthService.getCurrentUser();
  const userRoles = Array.isArray(currentUser?.roles) ? currentUser.roles : [];

  const isAuthorized =
    roles.length === 0 || roles.some((role) => userRoles.includes(role));

  return isAuthorized ? (
    <Outlet />
  ) : (
    <Navigate to="/unauthorized" replace />
  );
}

ProtectedRoute.propTypes = {
  roles: PropTypes.arrayOf(PropTypes.string),
};
