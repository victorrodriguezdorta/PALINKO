import { isAxiosError } from 'axios'
import apiClient from './apiClient'
import type { ApiError, GameLanguage } from '@/types/game'

export interface WordRelationPayload {
  wordA: string
  wordB: string
  language: GameLanguage
}

function toApiError(error: unknown): ApiError {
  if (isAxiosError(error) && error.response?.data?.code) {
    const data = error.response.data as ApiError
    return { code: data.code, message: data.message, args: data.args ?? {} }
  }
  return { code: 'GENERIC', message: 'Request failed', args: {} }
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
