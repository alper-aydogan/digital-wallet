import { apiClient } from './client'
import type { DemoTokenRequest, DemoTokenResponse } from './types'

export async function getDemoToken(userId: number): Promise<string> {
  const payload: DemoTokenRequest = { userId }
  const response = await apiClient.post<DemoTokenResponse>('/api/v1/auth/demo-token', payload)
  return response.data.token
}
