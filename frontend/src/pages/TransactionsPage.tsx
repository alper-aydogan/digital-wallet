import { useState, useEffect, useCallback } from 'react'
import { useParams, Link } from 'react-router-dom'
import { getTransactions } from '@/shared/api/wallet'
import { normalizeError } from '@/shared/lib/normalizeError'
import type { TransactionResponse, PageResponse } from '@/shared/api/types'
import { Button, Card } from '@/shared/ui'
import styles from './TransactionsPage.module.css'

const SORT_OPTIONS = [
  { value: 'transactionDate', label: 'Tarih' },
  { value: 'amount', label: 'Tutar' },
  { value: 'id', label: 'ID' },
]

export function TransactionsPage() {
  const { walletId } = useParams<{ walletId: string }>()
  const [data, setData] = useState<PageResponse<TransactionResponse> | null>(null)
  const [isLoading, setIsLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)
  
  const [params, setParams] = useState({
    page: 0,
    size: 10,
    sortBy: 'transactionDate' as const,
    direction: 'DESC' as 'ASC' | 'DESC',
  })

  const fetchTransactions = useCallback(async () => {
    if (!walletId) return
    setIsLoading(true)
    setError(null)
    try {
      const result = await getTransactions(parseInt(walletId, 10), params)
      setData(result)
    } catch (err) {
      const normalized = normalizeError(err)
      setError(normalized.message)
    } finally {
      setIsLoading(false)
    }
  }, [walletId, params])

  useEffect(() => {
    fetchTransactions()
  }, [fetchTransactions])

  const handleSortChange = (sortBy: typeof params.sortBy) => {
    setParams((prev) => ({ ...prev, sortBy, page: 0 }))
  }

  const handleDirectionChange = () => {
    setParams((prev) => ({
      ...prev,
      direction: prev.direction === 'ASC' ? 'DESC' : 'ASC',
      page: 0,
    }))
  }

  const handleSizeChange = (e: React.ChangeEvent<HTMLSelectElement>) => {
    setParams((prev) => ({ ...prev, size: parseInt(e.target.value), page: 0 }))
  }

  const handlePageChange = (newPage: number) => {
    setParams((prev) => ({ ...prev, page: newPage }))
  }

  const totalPages = data?.totalPages || 0

  return (
    <div className={styles.container}>
      <div className={styles.header}>
        <Link to="/wallet" className={styles.backLink}>
          ← Cüzdan'a Dön
        </Link>
        <h1 className={styles.title}>İşlem Geçmişi</h1>
      </div>

      {error && <div className={styles.errorBanner}>{error}</div>}

      <Card>
        <div className={styles.controls}>
          <div className={styles.controlGroup}>
            <label>Sıralama:</label>
            <select
              value={params.sortBy}
              onChange={(e) => handleSortChange(e.target.value as typeof params.sortBy)}
            >
              {SORT_OPTIONS.map((opt) => (
                <option key={opt.value} value={opt.value}>
                  {opt.label}
                </option>
              ))}
            </select>
            <Button variant="secondary" onClick={handleDirectionChange}>
              {params.direction === 'ASC' ? '↑ Artan' : '↓ Azalan'}
            </Button>
          </div>

          <div className={styles.controlGroup}>
            <label>Sayfa Boyutu:</label>
            <select value={params.size} onChange={handleSizeChange}>
              <option value={5}>5</option>
              <option value={10}>10</option>
              <option value={20}>20</option>
              <option value={50}>50</option>
            </select>
          </div>
        </div>

        {isLoading ? (
          <p className={styles.loading}>Yükleniyor...</p>
        ) : data?.content.length === 0 ? (
          <p className={styles.empty}>İşlem bulunmamaktadır.</p>
        ) : (
          <>
            <div className={styles.tableWrapper}>
              <table className={styles.table}>
                <thead>
                  <tr>
                    <th>ID</th>
                    <th>Tip</th>
                    <th>Gönderen</th>
                    <th>Alıcı</th>
                    <th>Tutar</th>
                    <th>Tarih</th>
                    <th>Açıklama</th>
                  </tr>
                </thead>
                <tbody>
                  {data?.content.map((tx) => (
                    <tr key={tx.id}>
                      <td>{tx.id}</td>
                      <td>
                        <span
                          className={`${styles.badge} ${
                            tx.type === 'DEPOSIT'
                              ? styles.deposit
                              : tx.type === 'WITHDRAWAL'
                              ? styles.withdraw
                              : styles.transfer
                          }`}
                        >
                          {tx.type === 'DEPOSIT'
                            ? 'Yatırma'
                            : tx.type === 'WITHDRAWAL'
                            ? 'Çekme'
                            : 'Transfer'}
                        </span>
                      </td>
                      <td>{tx.fromWalletId ?? '-'}</td>
                      <td>{tx.toWalletId ?? '-'}</td>
                      <td className={styles.amount}>{tx.amount}</td>
                      <td>
                        {new Date(tx.transactionDate).toLocaleString('tr-TR')}
                      </td>
                      <td>{tx.description ?? '-'}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>

            {totalPages > 1 && (
              <div className={styles.pagination}>
                <Button
                  variant="secondary"
                  onClick={() => handlePageChange(params.page - 1)}
                  disabled={params.page === 0 || isLoading}
                >
                  Önceki
                </Button>
                <span className={styles.pageInfo}>
                  Sayfa {params.page + 1} / {totalPages}
                </span>
                <Button
                  variant="secondary"
                  onClick={() => handlePageChange(params.page + 1)}
                  disabled={params.page >= totalPages - 1 || isLoading}
                >
                  Sonraki
                </Button>
              </div>
            )}

            <div className={styles.totalInfo}>
              Toplam {data?.totalElements} işlem
            </div>
          </>
        )}
      </Card>
    </div>
  )
}
