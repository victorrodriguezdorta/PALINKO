export type RoomStatus = 'LOBBY' | 'IN_PROGRESS' | 'FINISHED' | 'CLOSED'
export type RoundPhase = 'SHOWING_QUESTION' | 'ANSWERING' | 'VOTING' | 'REVEAL'

export interface PlayerView {
  id: string
  name: string
  score: number
  connected: boolean
  host: boolean
}

export interface AnswerView {
  id: string
  text: string
  authorPlayerId: string | null
  isAi: boolean
  voterPlayerIds: string[]
}

export interface RoundResultView {
  aiAnswerId: string
  scoreDeltaByPlayerId: Record<string, number>
}

export interface RoundView {
  roundNumber: number
  phase: RoundPhase
  questionText: string
  phaseDeadline: string | null
  answers: AnswerView[]
  result: RoundResultView | null
}

export interface RoomSettingsView {
  totalRounds: number
  answerTimeSeconds: number
  voteTimeSeconds: number
}

export interface RoomSnapshot {
  roomCode: string
  status: RoomStatus
  hostPlayerId: string
  players: PlayerView[]
  settings: RoomSettingsView
  currentRound: RoundView | null
}

export interface RoomIdentity {
  roomCode: string
  playerId: string
  reconnectToken: string
}

export interface RoomJoinedResponse {
  roomCode: string
  playerId: string
  reconnectToken: string
  snapshot: RoomSnapshot
}
