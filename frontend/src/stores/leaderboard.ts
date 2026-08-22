import { defineStore } from 'pinia'
import { ref } from 'vue'
import * as leaderboardService from '@/services/leaderboardService'
import type { DailyLeaderboardEntry } from '@/types/leaderboard'

export const useLeaderboardStore = defineStore('leaderboard', () => {
  const entries = ref<DailyLeaderboardEntry[]>([])
  const loading = ref(false)

  async function fetchDaily(): Promise<void> {
    loading.value = true
    try {
      entries.value = await leaderboardService.getDailyLeaderboard()
    } finally {
      loading.value = false
    }
  }

  return {
    entries,
    loading,
    fetchDaily,
  }
})
