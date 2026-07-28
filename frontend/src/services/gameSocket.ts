import { Client, type IMessage } from '@stomp/stompjs'
import SockJS from 'sockjs-client'
import type { RoomIdentity, RoomSnapshot } from '@/types/game'

type SnapshotHandler = (snapshot: RoomSnapshot) => void
type ErrorHandler = (message: string) => void

/**
 * Thin wrapper around @stomp/stompjs + sockjs-client matching the protocol
 * the backend expects: identity travels once as CONNECT headers (not
 * repeated per message), the room's full snapshot is pushed to
 * /topic/rooms/{code} on every change, and per-player domain errors (e.g.
 * voting for your own answer) arrive only on /user/queue/errors.
 */
export class GameSocket {
  private client: Client | null = null

  connect(identity: RoomIdentity, onSnapshot: SnapshotHandler, onError: ErrorHandler): void {
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
      client.subscribe(`/topic/rooms/${identity.roomCode}`, (message: IMessage) => {
        onSnapshot(JSON.parse(message.body) as RoomSnapshot)
      })
      client.subscribe('/user/queue/errors', (message: IMessage) => {
        const body = JSON.parse(message.body) as { message: string }
        onError(body.message)
      })
    }

    client.onStompError = (frame) => {
      onError(frame.body || frame.headers['message'] || 'Error de conexión con la sala')
    }

    this.client = client
    client.activate()
  }

  disconnect(): void {
    this.client?.deactivate()
    this.client = null
  }

  start(roomCode: string): void {
    this.publish(`/app/rooms/${roomCode}/start`, {})
  }

  submitAnswer(roomCode: string, answerText: string): void {
    this.publish(`/app/rooms/${roomCode}/answer`, { answerText })
  }

  cancelAnswer(roomCode: string): void {
    this.publish(`/app/rooms/${roomCode}/cancel-answer`, {})
  }

  submitVote(roomCode: string, votedAnswerId: string): void {
    this.publish(`/app/rooms/${roomCode}/vote`, { votedAnswerId })
  }

  nextRound(roomCode: string): void {
    this.publish(`/app/rooms/${roomCode}/next-round`, {})
  }

  playAgain(roomCode: string): void {
    this.publish(`/app/rooms/${roomCode}/play-again`, {})
  }

  updateSettings(
    roomCode: string,
    totalRounds: number,
    answerTimeSeconds: number,
    voteTimeSeconds: number,
  ): void {
    this.publish(`/app/rooms/${roomCode}/update-settings`, {
      totalRounds,
      answerTimeSeconds,
      voteTimeSeconds,
    })
  }

  private publish(destination: string, body: unknown): void {
    this.client?.publish({ destination, body: JSON.stringify(body) })
  }
}

export const gameSocket = new GameSocket()
