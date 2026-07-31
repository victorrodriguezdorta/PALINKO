import { Client, type IMessage } from '@stomp/stompjs'
import SockJS from 'sockjs-client'
import type { ApiError, GameLanguage, RoomIdentity, RoomSnapshot, TypingBroadcast } from '@/types/game'

type SnapshotHandler = (snapshot: RoomSnapshot) => void
type ErrorHandler = (error: ApiError) => void
type TypingHandler = (typing: TypingBroadcast) => void

const TYPING_THROTTLE_MS = 150

/**
 * Thin wrapper around @stomp/stompjs + sockjs-client matching the protocol
 * the backend expects: identity travels once as CONNECT headers (not
 * repeated per message); each player's own personalized snapshot (their
 * target word can secretly differ from everyone else's) is pushed to their
 * private /user/queue/room-updates on every change; the ephemeral live-typing
 * preview is a separate shared /topic/rooms/{code}/typing with no secret
 * data; and per-player domain errors (e.g. voting for yourself) arrive only
 * on /user/queue/errors.
 */
export class GameSocket {
  private client: Client | null = null
  private lastTypingSentAt = 0
  private pendingTypingTimeout: ReturnType<typeof setTimeout> | null = null

  connect(
    identity: RoomIdentity,
    onSnapshot: SnapshotHandler,
    onError: ErrorHandler,
    onTyping: TypingHandler,
  ): void {
    this.disconnect()

    const client = new Client({
      webSocketFactory: () => new SockJS(import.meta.env.VITE_WS_URL),
      connectHeaders: {
        'room-code': identity.roomCode,
        'player-id': identity.playerId,
        'reconnect-token': identity.reconnectToken,
      },
      reconnectDelay: 3000,
      debug: () => {},
    })

    client.onConnect = () => {
      client.subscribe('/user/queue/room-updates', (message: IMessage) => {
        onSnapshot(JSON.parse(message.body) as RoomSnapshot)
      })
      client.subscribe('/user/queue/errors', (message: IMessage) => {
        onError(JSON.parse(message.body) as ApiError)
      })
      client.subscribe(`/topic/rooms/${identity.roomCode}/typing`, (message: IMessage) => {
        onTyping(JSON.parse(message.body) as TypingBroadcast)
      })
    }

    client.onStompError = (frame) => {
      onError({ code: 'GENERIC', message: frame.body || frame.headers['message'] || 'STOMP connection error', args: {} })
    }

    this.client = client
    client.activate()
  }

  disconnect(): void {
    if (this.pendingTypingTimeout) {
      clearTimeout(this.pendingTypingTimeout)
      this.pendingTypingTimeout = null
    }
    this.client?.deactivate()
    this.client = null
  }

  start(roomCode: string): void {
    this.publish(`/app/rooms/${roomCode}/start`, {})
  }

  submitWord(roomCode: string, wordText: string): void {
    this.publish(`/app/rooms/${roomCode}/word`, { wordText })
  }

  submitVote(roomCode: string, suspectPlayerId: string): void {
    this.publish(`/app/rooms/${roomCode}/vote`, { suspectPlayerId })
  }

  playAgain(roomCode: string): void {
    this.publish(`/app/rooms/${roomCode}/play-again`, {})
  }

  kickPlayer(roomCode: string, targetPlayerId: string): void {
    this.publish(`/app/rooms/${roomCode}/kick`, { targetPlayerId })
  }

  updateSettings(
    roomCode: string,
    wordTimeSeconds: number,
    voteTimeSeconds: number,
    language: GameLanguage,
    infiltratorCount: number,
    phaseCount: number,
  ): void {
    this.publish(`/app/rooms/${roomCode}/update-settings`, {
      wordTimeSeconds,
      voteTimeSeconds,
      language,
      infiltratorCount,
      phaseCount,
    })
  }

  /**
   * Throttled so every keystroke doesn't open a network round-trip: sends
   * immediately if enough time has passed since the last send, otherwise
   * schedules a single trailing send (replacing any already pending) with
   * whatever text is current once the throttle window elapses.
   */
  sendTyping(roomCode: string, text: string): void {
    const now = Date.now()
    if (this.pendingTypingTimeout) {
      clearTimeout(this.pendingTypingTimeout)
      this.pendingTypingTimeout = null
    }
    const elapsed = now - this.lastTypingSentAt
    if (elapsed >= TYPING_THROTTLE_MS) {
      this.lastTypingSentAt = now
      this.publish(`/app/rooms/${roomCode}/typing`, { text })
    } else {
      this.pendingTypingTimeout = setTimeout(() => {
        this.lastTypingSentAt = Date.now()
        this.pendingTypingTimeout = null
        this.publish(`/app/rooms/${roomCode}/typing`, { text })
      }, TYPING_THROTTLE_MS - elapsed)
    }
  }

  private publish(destination: string, body: unknown): void {
    this.client?.publish({ destination, body: JSON.stringify(body) })
  }
}

export const gameSocket = new GameSocket()
