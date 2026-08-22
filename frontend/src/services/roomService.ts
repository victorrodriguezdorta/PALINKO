import apiClient from './apiClient'
import { toApiError } from './apiError'
import type { GameLanguage, RoomJoinedResponse } from '@/types/game'

export interface CreateRoomPayload {
  hostName: string
  avatarSeed: string
  language: GameLanguage
}

export interface CreateDailyRoomPayload {
  language: GameLanguage
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
