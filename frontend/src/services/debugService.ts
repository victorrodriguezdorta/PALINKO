import apiClient from './apiClient'
import { toApiError } from './apiError'
import type { GameLanguage } from '@/types/game'

export interface WordRelationPayload {
  wordA: string
  wordB: string
  language: GameLanguage
}

export interface WordRelationResult {
  relatednessPercentage: number
  justification: string | null
}

export async function calculateWordRelation(payload: WordRelationPayload): Promise<WordRelationResult> {
  try {
    const response = await apiClient.post<WordRelationResult>('/debug/word-relation', payload)
    return response.data
  } catch (error) {
    throw toApiError(error)
  }
}
