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

  const authenticatedUserId = getJwtSubject(token)

  // Form states
  const [createForm, setCreateForm] = useState({ currency: 'TRY' })
  const [depositAmount, setDepositAmount] = useState('')
  const [withdrawAmount, setWithdrawAmount] = useState('')
  const [transferForm, setTransferForm] = useState({ toUserId: '', amount: '', idempotencyKey: '' })

  // Independent loading and error states for each operation
  const [createState, setCreateState] = useState({ loading: false, error: null as string | null })
  const [depositState, setDepositState] = useState({ loading: false, error: null as string | null })
  const [withdrawState, setWithdrawState] = useState({ loading: false, error: null as string | null })
  const [transferState, setTransferState] = useState({ loading: false, error: null as string | null })

  const fetchWallet = useCallback(async () => {
    if (!authenticatedUserId) {
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
    setCreateState({ loading: true, error: null })

    try {
      if (!authenticatedUserId) {
        setCreateState({ loading: false, error: 'Kullanici kimligi token icinden okunamadi' })
        return
      }
      await createWallet({ userId: authenticatedUserId, currency: createForm.currency })
      await fetchWallet()
      setCreateForm({ currency: 'TRY' })
    } catch (err) {
      const normalized = normalizeError(err)
      setCreateState({ loading: false, error: normalized.message })
    } finally {
      setCreateState((prev) => ({ ...prev, loading: false }))
    }
  }

  const handleDeposit = async (e: FormEvent) => {
    e.preventDefault()
    setDepositState({ loading: true, error: null })

    const validation = amountSchema.safeParse(depositAmount)
    if (!validation.success) {
      setDepositState({ loading: false, error: validation.error.errors[0].message })
      return
    }

    try {
      if (!authenticatedUserId) {
        setDepositState({ loading: false, error: 'Kullanici kimligi token icinden okunamadi' })
        return
      }
      await deposit({ userId: authenticatedUserId, amount: depositAmount })
      await fetchWallet()
      setDepositAmount('')
    } catch (err) {
      const normalized = normalizeError(err)
      setDepositState({ loading: false, error: normalized.message })
    } finally {
      setDepositState((prev) => ({ ...prev, loading: false }))
    }
  }

  const handleWithdraw = async (e: FormEvent) => {
    e.preventDefault()
    setWithdrawState({ loading: true, error: null })

    const validation = amountSchema.safeParse(withdrawAmount)
    if (!validation.success) {
      setWithdrawState({ loading: false, error: validation.error.errors[0].message })
      return
    }

    try {
      if (!authenticatedUserId) {
        setWithdrawState({ loading: false, error: 'Kullanici kimligi token icinden okunamadi' })
        return
      }
      await withdraw({ userId: authenticatedUserId, amount: withdrawAmount })
      await fetchWallet()
      setWithdrawAmount('')
    } catch (err) {
      const normalized = normalizeError(err)
      setWithdrawState({ loading: false, error: normalized.message })
    } finally {
      setWithdrawState((prev) => ({ ...prev, loading: false }))
    }
  }

  const handleTransfer = async (e: FormEvent) => {
    e.preventDefault()
    setTransferState({ loading: true, error: null })

    const parsedToUserId = parseInt(transferForm.toUserId, 10)
    if (isNaN(parsedToUserId) || parsedToUserId <= 0) {
      setTransferState({ loading: false, error: 'Geçerli bir alıcı ID girin' })
      return
    }

    const amountValidation = amountSchema.safeParse(transferForm.amount)
    if (!amountValidation.success) {
      setTransferState({ loading: false, error: amountValidation.error.errors[0].message })
      return
    }

    try {
      await transfer(
        { toUserId: parsedToUserId, amount: transferForm.amount },
        transferForm.idempotencyKey || undefined
      )
      await fetchWallet()
      setTransferForm({ toUserId: '', amount: '', idempotencyKey: '' })
    } catch (err) {
      const normalized = normalizeError(err)
      setTransferState({ loading: false, error: normalized.message })
    } finally {
      setTransferState((prev) => ({ ...prev, loading: false }))
    }
  }

  return (
    <div className={styles.container}>
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
                  setCreateForm((prev: typeof createForm) => ({ ...prev, currency: e.target.value }))
                }
                placeholder="TRY"
              />
              {createState.error && <div className={styles.fieldError}>{createState.error}</div>}
              <Button type="submit" isLoading={createState.loading}>
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
              {depositState.error && <div className={styles.fieldError}>{depositState.error}</div>}
              <Button type="submit" isLoading={depositState.loading}>
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
              {withdrawState.error && <div className={styles.fieldError}>{withdrawState.error}</div>}
              <Button type="submit" variant="danger" isLoading={withdrawState.loading}>
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
                  setTransferForm((prev: typeof transferForm) => ({ ...prev, toUserId: e.target.value }))
                }
                placeholder="Örn: 2"
              />
              <Input
                label="Tutar"
                type="number"
                step="0.01"
                value={transferForm.amount}
                onChange={(e) =>
                  setTransferForm((prev: typeof transferForm) => ({ ...prev, amount: e.target.value }))
                }
                placeholder="0.00"
              />
              <Input
                label="Idempotency Key (opsiyonel)"
                value={transferForm.idempotencyKey}
                onChange={(e) =>
                  setTransferForm((prev: typeof transferForm) => ({ ...prev, idempotencyKey: e.target.value }))
                }
                placeholder="Tekrarlanabilir işlem anahtarı"
              />
              {transferState.error && <div className={styles.fieldError}>{transferState.error}</div>}
              <Button type="submit" isLoading={transferState.loading}>
                Transfer Yap
              </Button>
            </form>
          </Card>
        )}
      </div>
    </div>
  )
}
