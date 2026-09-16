import { Route, Routes } from 'react-router-dom';

import DashboardLayout from './layouts/dashboard/DashboardLayout';
import PrivateRoute from './PrivateRoute';
import ProtectedRoute from './ProtectedRoute';

import Login from './pages/auth/Login';
import Signup from './pages/auth/Signup';
import Unauthorized from './pages/auth/Unauthorized';
import NotFound from './pages/auth/NotFound';

import Dashboard from './pages/dashboard/Dashboard';
import Transaction from './pages/transaction/Transaction';
import BasicTabs from './pages/transfer/BasicTabs';
import AddFunds from './pages/wallet/AddFunds';
import NewWallet from './pages/wallet/NewWallet';
import Wallet from './pages/wallet/Wallet';

const USER_ROLES = ['ROLE_USER', 'ROLE_ADMIN'];

export default function App() {
  return (
    <Routes>
      <Route path="/login" element={<Login />} />
      <Route path="/signup" element={<Signup />} />

      <Route element={<PrivateRoute />}>
        <Route path="/unauthorized" element={<Unauthorized />} />

        <Route element={<ProtectedRoute roles={USER_ROLES} />}>
          <Route element={<DashboardLayout />}>
            <Route index element={<Dashboard />} />

            <Route path="wallets">
              <Route index element={<Wallet />} />
              <Route path="new" element={<NewWallet />} />
              <Route path="addFunds" element={<AddFunds />} />
            </Route>

            <Route path="transfers" element={<BasicTabs />} />
            <Route path="transactions" element={<Transaction />} />
          </Route>
        </Route>
      </Route>

      <Route path="*" element={<NotFound />} />
    </Routes>
  );
}
