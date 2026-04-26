export interface BackendError {
  status: number
  code: string
  message: string
  timestamp: string
}

export interface DemoTokenRequest {
  userId: number
}

export interface DemoTokenResponse {
  token: string
}

export interface CreateWalletRequest {
  userId: number
  currency: string
}

export interface WalletResponse {
  id: number
  userId: number
  balance: string
  currency: string
}

export interface DepositRequest {
  userId: number
  amount: string
}

export interface WithdrawRequest {
  userId: number
  amount: string
}

export interface TransferRequest {
  toUserId: number
  amount: string
}

export interface TransactionResponse {
  id: number
  fromWalletId: number | null
  toWalletId: number | null
  amount: string
  transactionDate: string
  description: string | null
}

export interface PageResponse<T> {
  content: T[]
  totalElements: number
  totalPages: number
  size: number
  number: number
}

export interface TransactionsQueryParams {
  page?: number
  size?: number
  sortBy?: 'transactionDate' | 'amount' | 'id'
  direction?: 'ASC' | 'DESC'
}
