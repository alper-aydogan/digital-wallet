import { Routes, Route, Navigate } from 'react-router-dom'
import { useAuth } from './AuthContext'
import { Layout } from './Layout'
import { AuthPage } from '@/pages/AuthPage'
import { WalletPage } from '@/pages/WalletPage'
import { TransactionsPage } from '@/pages/TransactionsPage'

function ProtectedRoute({ children }: { children: React.ReactNode }) {
  const { isAuthenticated } = useAuth()
  return isAuthenticated ? <>{children}</> : <Navigate to="/auth" replace />
}

function App() {
  return (
    <Routes>
      <Route path="/auth" element={<AuthPage />} />
      <Route
        path="/"
        element={
          <ProtectedRoute>
            <Layout />
          </ProtectedRoute>
        }
      >
        <Route index element={<Navigate to="/wallet" replace />} />
        <Route path="wallet" element={<WalletPage />} />
        <Route path="transactions/:walletId" element={<TransactionsPage />} />
      </Route>
    </Routes>
  )
}

export default App
