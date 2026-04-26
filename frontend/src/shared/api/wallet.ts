import { apiClient } from './client'
import type {
  CreateWalletRequest,
  WalletResponse,
  DepositRequest,
  WithdrawRequest,
  TransferRequest,
  TransactionResponse,
  PageResponse,
  TransactionsQueryParams,
} from './types'

export async function createWallet(request: CreateWalletRequest): Promise<WalletResponse> {
  const response = await apiClient.post<WalletResponse>('/api/v1/wallets', request)
  return response.data
}

export async function getWallet(userId: number): Promise<WalletResponse> {
  const response = await apiClient.get<WalletResponse>(`/api/v1/wallets?userId=${userId}`)
  return response.data
}

export async function deposit(
  request: DepositRequest,
  idempotencyKey?: string
): Promise<WalletResponse> {
  const headers = idempotencyKey ? { 'Idempotency-Key': idempotencyKey } : undefined
  const response = await apiClient.post<WalletResponse>('/api/v1/wallets/deposit', request, {
    headers,
  })
  return response.data
}

export async function withdraw(
  request: WithdrawRequest,
  idempotencyKey?: string
): Promise<WalletResponse> {
  const headers = idempotencyKey ? { 'Idempotency-Key': idempotencyKey } : undefined
  const response = await apiClient.post<WalletResponse>('/api/v1/wallets/withdraw', request, {
    headers,
  })
  return response.data
}

export async function transfer(
  request: TransferRequest,
  idempotencyKey?: string
): Promise<TransactionResponse> {
  const headers = idempotencyKey ? { 'Idempotency-Key': idempotencyKey } : undefined
  const response = await apiClient.post<TransactionResponse>('/api/v1/wallets/transfer', request, {
    headers,
  })
  return response.data
}

export async function getTransactions(
  walletId: number,
  params: TransactionsQueryParams = {}
): Promise<PageResponse<TransactionResponse>> {
  const { page = 0, size = 10, sortBy = 'transactionDate', direction = 'DESC' } = params
  const response = await apiClient.get<PageResponse<TransactionResponse>>(
    `/api/v1/wallets/${walletId}/transactions`,
    {
      params: { page, size, sortBy, direction },
    }
  )
  return response.data
}
