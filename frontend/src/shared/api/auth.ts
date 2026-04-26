import { apiClient } from './client'

export async function getDemoToken(userId: number): Promise<string> {
  const response = await apiClient.post<string>(`/api/v1/auth/demo-token?userId=${userId}`)
  return response.data
}
