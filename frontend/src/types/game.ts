export type RoomStatus = 'LOBBY' | 'IN_PROGRESS' | 'FINISHED' | 'CLOSED'
export type RoundPhase = 'WORD_CHAIN' | 'VOTING' | 'REVEAL'
export type AttemptOutcome = 'ACCEPTED' | 'REJECTED' | 'SKIPPED'
export type GameLanguage = 'ENGLISH' | 'SPANISH'

export interface PlayerView {
  id: string
  name: string
  score: number
  connected: boolean
  host: boolean
}

export interface AttemptView {
  id: string
  authorPlayerId: string
  turnNumber: number
  text: string
  outcome: AttemptOutcome
  relatednessToPrevious: number
  justification: string | null
  relatednessToTarget: number | null
  reachedTarget: boolean
  phaseIndex: number
}

export interface VoteView {
  voterPlayerId: string
  suspectPlayerId: string
}

export interface RevealView {
  infiltratorPlayerIds: string[]
  infiltratorTargetWord: string | null
  accusedPlayerId: string | null
  crewWon: boolean
  scoreDeltaByPlayerId: Record<string, number>
  endedByInfiltratorWord: boolean
  gameStartWord: string
  acceptedWordChain: string[]
  acceptedWordCountByPhase: number[]
}

export interface ChainView {
  phase: RoundPhase
  startWord: string
  yourTargetWord: string
  currentWord: string
  currentTurnPlayerId: string | null
  phaseDeadline: string | null
  infiltratorCount: number
  currentPhaseNumber: number
  totalPhases: number
  phaseStartWords: string[]
  yourPhaseTargetWords: string[]
  attempts: AttemptView[]
  votes: VoteView[]
  reveal: RevealView | null
}

export interface RoomSettingsView {
  wordTimeSeconds: number
  voteTimeSeconds: number
  language: GameLanguage
  infiltratorCount: number
  phaseCount: number
  daily: boolean
}

export interface RoomSnapshot {
  roomCode: string
  status: RoomStatus
  hostPlayerId: string
  players: PlayerView[]
  settings: RoomSettingsView
  viewerPlayerId: string
  chain: ChainView | null
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

export interface TypingBroadcast {
  playerId: string
  text: string
}

/**
 * Shape shared by both StompErrorMessage (STOMP /user/queue/errors) and
 * GameErrorMessage (REST create/join failures) on the backend: a stable
 * code the frontend translates via i18n's errors.* namespace, any dynamic
 * values as named args for interpolation, and the raw (English) message
 * kept only for console/debug logging.
 */
export interface ApiError {
  code: string
  message: string
  args: Record<string, string>
}
