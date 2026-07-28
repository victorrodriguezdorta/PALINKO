import apiClient from './apiClient'
import type { RoomJoinedResponse } from '@/types/game'

export interface CreateRoomPayload {
  hostName: string
  totalRounds: number
  answerTimeSeconds: number
  voteTimeSeconds: number
}

export async function createRoom(payload: CreateRoomPayload): Promise<RoomJoinedResponse> {
  const response = await apiClient.post<RoomJoinedResponse>('/rooms', payload)
  return response.data
}

export async function joinRoom(roomCode: string, playerName: string): Promise<RoomJoinedResponse> {
  const response = await apiClient.post<RoomJoinedResponse>(`/rooms/${roomCode}/join`, { playerName })
  return response.data
}
