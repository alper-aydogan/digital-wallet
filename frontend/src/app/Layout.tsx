import { Outlet, useNavigate } from 'react-router-dom'
import { useAuth } from './AuthContext'
import { Button } from '@/shared/ui/Button'
import styles from './Layout.module.css'

export function Layout() {
  const { logout } = useAuth()
  const navigate = useNavigate()

  const handleLogout = () => {
    logout()
    navigate('/auth')
  }

  return (
    <div className={styles.layout}>
      <header className={styles.header}>
        <div className={styles.headerContent}>
          <h1 className={styles.title}>Digital Wallet</h1>
          <Button variant="secondary" onClick={handleLogout}>
            Çıkış Yap
          </Button>
        </div>
      </header>
      <main className={styles.main}>
        <Outlet />
      </main>
    </div>
  )
}
