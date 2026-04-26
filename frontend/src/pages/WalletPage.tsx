import { useState, useEffect, useCallback } from 'react'
import type { FormEvent } from 'react'
import { Link } from 'react-router-dom'
import { z } from 'zod'
import { useAuth } from '@/app/AuthContext'
import {
  createWallet,
  getWallet,
  deposit,
  withdraw,
  transfer,
} from '@/shared/api/wallet'
import { normalizeError } from '@/shared/lib/normalizeError'
import { getJwtSubject } from '@/shared/lib/jwt'
import type { WalletResponse } from '@/shared/api/types'
import { Button, Input, Card } from '@/shared/ui'
import styles from './WalletPage.module.css'

const amountSchema = z
  .string()
  .refine((val) => !isNaN(parseFloat(val)) && parseFloat(val) > 0, {
    message: '0\'dan büyük bir tutar girin',
  })

export function WalletPage() {
  const { token, logout } = useAuth()
  const [wallet, setWallet] = useState<WalletResponse | null>(null)
  const [isLoading, setIsLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)

  const authenticatedUserId = getJwtSubject(token)

  // Form states
  const [createForm, setCreateForm] = useState({ currency: 'TRY' })
  const [depositAmount, setDepositAmount] = useState('')
  const [withdrawAmount, setWithdrawAmount] = useState('')
  const [transferForm, setTransferForm] = useState({ toUserId: '', amount: '', idempotencyKey: '' })

  const fetchWallet = useCallback(async () => {
    if (!authenticatedUserId) {
      setError('Oturum bilgisi gecersiz. Lutfen tekrar giris yapin.')
      return
    }

    try {
      const data = await getWallet()
      setWallet(data)
    } catch (err) {
      const normalized = normalizeError(err)
      if (normalized.status === 401 || normalized.status === 403) {
        logout()
        return
      }
      setWallet(null)
    }
  }, [authenticatedUserId, logout])

  useEffect(() => {
    fetchWallet()
  }, [fetchWallet])

  const handleCreateWallet = async (e: FormEvent) => {
    e.preventDefault()
    setError(null)
    setIsLoading(true)

    try {
      if (!authenticatedUserId) {
        setError('Kullanici kimligi token icinden okunamadi')
        return
      }
      await createWallet({ userId: authenticatedUserId, currency: createForm.currency })
      await fetchWallet()
      setCreateForm({ currency: 'TRY' })
    } catch (err) {
      const normalized = normalizeError(err)
      setError(normalized.message)
    } finally {
      setIsLoading(false)
    }
  }

  const handleDeposit = async (e: FormEvent) => {
    e.preventDefault()
    setError(null)

    const validation = amountSchema.safeParse(depositAmount)
    if (!validation.success) {
      setError(validation.error.errors[0].message)
      return
    }

    setIsLoading(true)
    try {
      if (!authenticatedUserId) {
        setError('Kullanici kimligi token icinden okunamadi')
        return
      }
      await deposit({ userId: authenticatedUserId, amount: depositAmount })
      await fetchWallet()
      setDepositAmount('')
    } catch (err) {
      const normalized = normalizeError(err)
      setError(normalized.message)
    } finally {
      setIsLoading(false)
    }
  }

  const handleWithdraw = async (e: FormEvent) => {
    e.preventDefault()
    setError(null)

    const validation = amountSchema.safeParse(withdrawAmount)
    if (!validation.success) {
      setError(validation.error.errors[0].message)
      return
    }

    setIsLoading(true)
    try {
      if (!authenticatedUserId) {
        setError('Kullanici kimligi token icinden okunamadi')
        return
      }
      await withdraw({ userId: authenticatedUserId, amount: withdrawAmount })
      await fetchWallet()
      setWithdrawAmount('')
    } catch (err) {
      const normalized = normalizeError(err)
      setError(normalized.message)
    } finally {
      setIsLoading(false)
    }
  }

  const handleTransfer = async (e: FormEvent) => {
    e.preventDefault()
    setError(null)

    const parsedToUserId = parseInt(transferForm.toUserId, 10)
    if (isNaN(parsedToUserId) || parsedToUserId <= 0) {
      setError('Geçerli bir alıcı ID girin')
      return
    }

    const amountValidation = amountSchema.safeParse(transferForm.amount)
    if (!amountValidation.success) {
      setError(amountValidation.error.errors[0].message)
      return
    }

    setIsLoading(true)
    try {
      await transfer(
        { toUserId: parsedToUserId, amount: transferForm.amount },
        transferForm.idempotencyKey || undefined
      )
      await fetchWallet()
      setTransferForm({ toUserId: '', amount: '', idempotencyKey: '' })
    } catch (err) {
      const normalized = normalizeError(err)
      setError(normalized.message)
    } finally {
      setIsLoading(false)
    }
  }

  return (
    <div className={styles.container}>
      {error && <div className={styles.errorBanner}>{error}</div>}

      <div className={styles.grid}>
        {/* Wallet Info */}
        <Card title="Cüzdan Bilgileri" className={styles.walletCard}>
          {wallet ? (
            <div className={styles.walletInfo}>
              <div className={styles.balanceRow}>
                <span className={styles.balanceLabel}>Bakiye:</span>
                <span className={styles.balanceValue}>
                  {wallet.balance} {wallet.currency}
                </span>
              </div>
              <div className={styles.walletDetails}>
                <p>Cüzdan ID: {wallet.id}</p>
                <p>Kullanıcı ID: {wallet.userId}</p>
              </div>
              <Link
                to={`/transactions/${wallet.id}`}
                className={styles.transactionsLink}
              >
                İşlem Geçmişini Görüntüle →
              </Link>
            </div>
          ) : (
            <p className={styles.noWallet}>Henüz cüzdan oluşturulmamış.</p>
          )}
        </Card>

        {/* Create Wallet */}
        {!wallet && (
          <Card title="Cüzdan Oluştur">
            <form onSubmit={handleCreateWallet} className={styles.form}>
              <Input label="Kullanıcı ID" value={authenticatedUserId ?? ''} disabled />
              <Input
                label="Para Birimi"
                value={createForm.currency}
                onChange={(e) =>
                  setCreateForm((prev) => ({ ...prev, currency: e.target.value }))
                }
                placeholder="TRY"
              />
              <Button type="submit" isLoading={isLoading}>
                Cüzdan Oluştur
              </Button>
            </form>
          </Card>
        )}

        {/* Deposit */}
        {wallet && (
          <Card title="Para Yatır">
            <form onSubmit={handleDeposit} className={styles.form}>
              <Input
                label="Tutar"
                type="number"
                step="0.01"
                value={depositAmount}
                onChange={(e) => setDepositAmount(e.target.value)}
                placeholder="0.00"
              />
              <Button type="submit" isLoading={isLoading}>
                Para Yatır
              </Button>
            </form>
          </Card>
        )}

        {/* Withdraw */}
        {wallet && (
          <Card title="Para Çek">
            <form onSubmit={handleWithdraw} className={styles.form}>
              <Input
                label="Tutar"
                type="number"
                step="0.01"
                value={withdrawAmount}
                onChange={(e) => setWithdrawAmount(e.target.value)}
                placeholder="0.00"
              />
              <Button type="submit" variant="danger" isLoading={isLoading}>
                Para Çek
              </Button>
            </form>
          </Card>
        )}

        {/* Transfer */}
        {wallet && (
          <Card title="Para Transferi">
            <form onSubmit={handleTransfer} className={styles.form}>
              <Input
                label="Alıcı Kullanıcı ID"
                type="number"
                value={transferForm.toUserId}
                onChange={(e) =>
                  setTransferForm((prev) => ({ ...prev, toUserId: e.target.value }))
                }
                placeholder="Örn: 2"
              />
              <Input
                label="Tutar"
                type="number"
                step="0.01"
                value={transferForm.amount}
                onChange={(e) =>
                  setTransferForm((prev) => ({ ...prev, amount: e.target.value }))
                }
                placeholder="0.00"
              />
              <Input
                label="Idempotency Key (opsiyonel)"
                value={transferForm.idempotencyKey}
                onChange={(e) =>
                  setTransferForm((prev) => ({ ...prev, idempotencyKey: e.target.value }))
                }
                placeholder="Tekrarlanabilir işlem anahtarı"
              />
              <Button type="submit" isLoading={isLoading}>
                Transfer Yap
              </Button>
            </form>
          </Card>
        )}
      </div>
    </div>
  )
}
