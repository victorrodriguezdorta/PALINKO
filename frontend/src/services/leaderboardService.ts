import apiClient from './apiClient'
import { toApiError } from './apiError'
import type { DailyLeaderboardEntry } from '@/types/leaderboard'

export async function getDailyLeaderboard(): Promise<DailyLeaderboardEntry[]> {
  try {
    const response = await apiClient.get<DailyLeaderboardEntry[]>('/leaderboard/daily')
    return response.data
  } catch (error) {
    throw toApiError(error)
  }
}
