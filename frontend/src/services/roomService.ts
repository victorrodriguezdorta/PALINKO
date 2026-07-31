import { isAxiosError } from 'axios'
import apiClient from './apiClient'
import type { ApiError, GameLanguage, RoomJoinedResponse } from '@/types/game'

export interface CreateRoomPayload {
  hostName: string
  avatarSeed: string
  language: GameLanguage
}

export interface CreateDailyRoomPayload {
  language: GameLanguage
}

/**
 * Normalizes any create/join failure into an ApiError so callers can
 * translate it the same way regardless of source: the backend's own
 * {code, message, args} body when present (see GameErrorMessage), or a
 * generic client-side fallback for anything else (network failure,
 * unexpected 5xx, a body that isn't shaped like an ApiError).
 */
function toApiError(error: unknown): ApiError {
  if (isAxiosError(error) && error.response?.data?.code) {
    const data = error.response.data as ApiError
    return { code: data.code, message: data.message, args: data.args ?? {} }
  }
  return { code: 'GENERIC', message: 'Request failed', args: {} }
}

export async function createRoom(payload: CreateRoomPayload): Promise<RoomJoinedResponse> {
  try {
    const response = await apiClient.post<RoomJoinedResponse>('/rooms', payload)
    return response.data
  } catch (error) {
    throw toApiError(error)
  }
}

export async function createDailyRoom(payload: CreateDailyRoomPayload): Promise<RoomJoinedResponse> {
  try {
    const response = await apiClient.post<RoomJoinedResponse>('/rooms/daily', payload)
    return response.data
  } catch (error) {
    throw toApiError(error)
  }
}

export async function joinRoom(
  roomCode: string,
  playerName: string,
  avatarSeed: string,
): Promise<RoomJoinedResponse> {
  try {
    const response = await apiClient.post<RoomJoinedResponse>(`/rooms/${roomCode}/join`, { playerName, avatarSeed })
    return response.data
  } catch (error) {
    throw toApiError(error)
  }
}
