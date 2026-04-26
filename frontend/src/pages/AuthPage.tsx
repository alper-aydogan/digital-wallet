import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { z } from 'zod'
import { useAuth } from '@/app/AuthContext'
import { getDemoToken } from '@/shared/api/auth'
import { normalizeError } from '@/shared/lib/normalizeError'
import { Button, Input, Card } from '@/shared/ui'
import styles from './AuthPage.module.css'

const userIdSchema = z
  .number({ invalid_type_error: 'Geçerli bir kullanıcı ID girin' })
  .int('Tam sayı olmalı')
  .positive('0\'dan büyük olmalı')

export function AuthPage() {
  const [userId, setUserId] = useState('')
  const [isLoading, setIsLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const { login } = useAuth()
  const navigate = useNavigate()

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    setError(null)

    const parsedId = parseInt(userId, 10)
    const validation = userIdSchema.safeParse(parsedId)

    if (!validation.success) {
      setError(validation.error.errors[0].message)
      return
    }

    setIsLoading(true)
    try {
      const token = await getDemoToken(parsedId)
      login(token)
      navigate('/wallet')
    } catch (err) {
      const normalized = normalizeError(err)
      setError(normalized.message)
    } finally {
      setIsLoading(false)
    }
  }

  return (
    <div className={styles.container}>
      <Card className={styles.card}>
        <h1 className={styles.title}>Digital Wallet</h1>
        <p className={styles.subtitle}>Giriş yapmak için kullanıcı ID girin</p>

        <form onSubmit={handleSubmit} className={styles.form}>
          <Input
            label="Kullanıcı ID"
            type="number"
            value={userId}
            onChange={(e) => setUserId(e.target.value)}
            placeholder="Örn: 1"
            error={error || undefined}
          />

          {error && !error.includes('ID') && (
            <div className={styles.error}>{error}</div>
          )}

          <Button type="submit" isLoading={isLoading} className={styles.button}>
            Token Üret
          </Button>
        </form>
      </Card>
    </div>
  )
}
