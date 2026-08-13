import { isAxiosError } from 'axios'
import apiClient from './apiClient'
import type { SecretApiError, GameLanguage, SecretRoomJoinedResponse } from '@/types/secretGame'

export interface CreateSecretDailyRoomPayload {
  language: GameLanguage
}

function toApiError(error: unknown): SecretApiError {
  if (isAxiosError(error) && error.response?.data?.code) {
    const data = error.response.data as SecretApiError
    return { code: data.code, message: data.message, args: data.args ?? {} }
  }
  return { code: 'GENERIC', message: 'Request failed', args: {} }
}

export async function createSecretDailyRoom(payload: CreateSecretDailyRoomPayload): Promise<SecretRoomJoinedResponse> {
  try {
    const response = await apiClient.post<SecretRoomJoinedResponse>('/secret-rooms/daily', payload)
    return response.data
  } catch (error) {
    throw toApiError(error)
  }
}
