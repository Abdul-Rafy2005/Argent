import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom'
import Dashboard from '@/pages/Dashboard'
import Wallets from '@/pages/Wallets'
import WalletDetail from '@/pages/WalletDetail'
import Transactions from '@/pages/Transactions'
import Ledger from '@/pages/Ledger'
import AuditLogs from '@/pages/AuditLogs'
import Reports from '@/pages/Reports'
import Settings from '@/pages/Settings'
import Login from '@/pages/Login'
import Register from '@/pages/Register'
import Layout from '@/components/layout/Layout'
import ProtectedRoute from '@/components/layout/ProtectedRoute'

function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/login" element={<Login />} />
        <Route path="/register" element={<Register />} />
        
        <Route element={<ProtectedRoute />}>
          <Route path="/" element={<Layout />}>
            <Route index element={<Dashboard />} />
            <Route path="wallets" element={<Wallets />} />
            <Route path="wallets/:id" element={<WalletDetail />} />
            <Route path="transactions" element={<Transactions />} />
            <Route path="ledger" element={<Ledger />} />
            <Route path="audit" element={<AuditLogs />} />
            <Route path="reports" element={<Reports />} />
            <Route path="settings" element={<Settings />} />
          </Route>
        </Route>
        
        <Route path="*" element={<Navigate to="/" replace />} />
      </Routes>
    </BrowserRouter>
  )
}

export default App
