export type SecretRoomStatus = 'IN_PROGRESS' | 'SOLVED' | 'GAVE_UP' | 'CLOSED'
export type WordAttemptOutcome = 'ACCEPTED' | 'REJECTED'
export type JudgementSource = 'WORD_BANK' | 'AI' | 'HINT'
export type SolutionMatchLevel = 'CORRECT' | 'PARTIAL' | 'INCORRECT'
export type GameLanguage = 'ENGLISH' | 'SPANISH'

export interface SecretWordAttemptView {
  id: string
  text: string
  outcome: WordAttemptOutcome
  source: JudgementSource
  submittedAt: string
}

export interface SecretSolutionAttemptView {
  id: string
  formulationText: string
  matchLevel: SolutionMatchLevel
  submittedAt: string
}

/**
 * Deliberately has no numeric accuracy field, mirroring the backend's
 * SecretRoomSnapshot.RevealView: showing a percentage anywhere in the UI
 * would let the player play "hot/cold" against the number instead of
 * genuinely guessing the secret quality.
 */
export interface SecretRevealView {
  hiddenQuality: string
  finalScore: number | null
  totalAttempts: number
  acceptedAttempts: number
  rejectedAttempts: number
  hintsUsed: number
}

export interface SecretRoomSnapshot {
  roomCode: string
  status: SecretRoomStatus
  language: GameLanguage
  conceptId: string
  attempts: SecretWordAttemptView[]
  totalFailures: number
  failuresPerHint: number
  hintsAvailable: number
  hintsUsed: number
  solutionAttempts: SecretSolutionAttemptView[]
  reveal: SecretRevealView | null
  createdAt: string
}

export interface SecretRoomIdentity {
  roomCode: string
  playerId: string
  reconnectToken: string
}

export interface SecretRoomJoinedResponse {
  roomCode: string
  playerId: string
  reconnectToken: string
  snapshot: SecretRoomSnapshot
}

/** Shared shape for both SecretStompErrorMessage and SecretGameErrorMessage on the backend. */
export interface SecretApiError {
  code: string
  message: string
  args: Record<string, string>
}
