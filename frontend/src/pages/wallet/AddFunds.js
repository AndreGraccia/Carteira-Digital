import { Navigate } from 'react-router-dom';

export default function LegacyOperation() { return <Navigate to="/transfers?mode=deposit" replace />; }
