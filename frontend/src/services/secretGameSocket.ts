import { Client, type IMessage } from '@stomp/stompjs'
import SockJS from 'sockjs-client'
import type { SecretApiError, SecretRoomIdentity, SecretRoomSnapshot } from '@/types/secretGame'

type SnapshotHandler = (snapshot: SecretRoomSnapshot) => void
type ErrorHandler = (error: SecretApiError) => void

/**
 * Thin wrapper around @stomp/stompjs + sockjs-client for secretmode,
 * mirroring services/gameSocket.ts's GameSocket class exactly but talking
 * to secretmode's own destinations (/secret-rooms/{code}/... and
 * /user/queue/secret-room-updates / /user/queue/secret-errors) so the two
 * modes' STOMP traffic never mixes on the client side either, even though
 * they share the same physical /ws endpoint on the backend.
 */
export class SecretGameSocket {
  private client: Client | null = null

  connect(identity: SecretRoomIdentity, onSnapshot: SnapshotHandler, onError: ErrorHandler): void {
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
      client.subscribe('/user/queue/secret-room-updates', (message: IMessage) => {
        onSnapshot(JSON.parse(message.body) as SecretRoomSnapshot)
      })
      client.subscribe('/user/queue/secret-errors', (message: IMessage) => {
        onError(JSON.parse(message.body) as SecretApiError)
      })
    }

    client.onStompError = (frame) => {
      onError({ code: 'GENERIC', message: frame.body || frame.headers['message'] || 'STOMP connection error', args: {} })
    }

    this.client = client
    client.activate()
  }

  disconnect(): void {
    this.client?.deactivate()
    this.client = null
  }

  submitWord(roomCode: string, wordText: string): void {
    this.publish(`/app/secret-rooms/${roomCode}/word`, { wordText })
  }

  useHint(roomCode: string): void {
    this.publish(`/app/secret-rooms/${roomCode}/hint`, {})
  }

  submitSolution(roomCode: string, formulationText: string): void {
    this.publish(`/app/secret-rooms/${roomCode}/solve`, { formulationText })
  }

  giveUp(roomCode: string): void {
    this.publish(`/app/secret-rooms/${roomCode}/give-up`, {})
  }

  private publish(destination: string, body: unknown): void {
    this.client?.publish({ destination, body: JSON.stringify(body) })
  }
}

export const secretGameSocket = new SecretGameSocket()
