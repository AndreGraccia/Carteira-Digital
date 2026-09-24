import { Navigate, Route, Routes } from 'react-router-dom';

import DashboardLayout from './layouts/dashboard/DashboardLayout';
import PrivateRoute from './PrivateRoute';
import ProtectedRoute from './ProtectedRoute';

import Login from './pages/auth/Login';
import Signup from './pages/auth/Signup';
import Unauthorized from './pages/auth/Unauthorized';
import NotFound from './pages/auth/NotFound';

import Dashboard from './pages/dashboard/Dashboard';
import Transaction from './pages/transaction/Transaction';
import Envelopes from './pages/envelopes/Envelopes';
import About from './pages/About';
import NewWallet from './pages/wallet/NewWallet';
import Wallet from './pages/wallet/Wallet';
import Insights from './pages/insights/Insights';
import OpenFinance from './pages/open-finance/OpenFinance';
import Future from './pages/future/Future';
import Ask from './pages/ask/Ask';
import Commitments from './pages/commitments/Commitments';

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
              <Route path="addFunds" element={<Navigate to="/open-finance" replace />} />
              <Route path=":walletId" element={<Wallet />} />
            </Route>

            <Route path="envelopes" element={<Envelopes />} />
            {/* Old links and bookmarks from the "Movimentar" screen, replaced by envelopes. */}
            <Route path="transfers" element={<Navigate to="/envelopes" replace />} />
            <Route path="transactions" element={<Transaction />} />
            <Route path="future" element={<Future />} />
            <Route path="ask" element={<Ask />} />
            <Route path="commitments" element={<Commitments />} />
            <Route path="insights" element={<Insights />} />
            <Route path="open-finance" element={<OpenFinance />} />
            <Route path="about" element={<About />} />
          </Route>
        </Route>
      </Route>

      <Route path="*" element={<NotFound />} />
    </Routes>
  );
}
