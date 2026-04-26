import { AxiosError } from 'axios'
import type { BackendError } from '@/shared/api/types'

export interface NormalizedError {
  status: number
  code: string
  message: string
}

export function normalizeError(error: unknown): NormalizedError {
  if (error instanceof AxiosError && error.response?.data) {
    const data = error.response.data as Partial<BackendError>
    return {
      status: error.response.status,
      code: data.code || 'UNKNOWN_ERROR',
      message: data.message || 'Bilinmeyen bir hata oluştu',
    }
  }

  if (error instanceof Error) {
    return {
      status: 0,
      code: 'CLIENT_ERROR',
      message: error.message || 'İstemci hatası oluştu',
    }
  }

  return {
    status: 500,
    code: 'UNKNOWN_ERROR',
    message: 'Beklenmeyen bir hata oluştu',
  }
}
