import apiClient from './apiClient'
import { toApiError } from './apiError'

export async function getCompletedDailyChallengeDates(): Promise<string[]> {
  try {
    const response = await apiClient.get<string[]>('/users/me/daily-history')
    return response.data
  } catch (error) {
    throw toApiError(error)
  }
}
