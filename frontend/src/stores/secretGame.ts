import { defineStore } from 'pinia'
import { ref } from 'vue'
import { createSecretDailyRoom as createSecretDailyRoomRequest } from '@/services/secretRoomService'
import { secretGameSocket } from '@/services/secretGameSocket'
import type { SecretApiError, GameLanguage, SecretRoomIdentity, SecretRoomSnapshot } from '@/types/secretGame'

// Distinct key from the original mode's "human-or-ai:identity" so a
// player can have both modes open in different tabs without one
// overwriting the other's stored identity.
const STORAGE_KEY = 'palinko-code:identity'

function loadIdentity(): SecretRoomIdentity | null {
  const raw = sessionStorage.getItem(STORAGE_KEY)
  if (!raw) return null
  try {
    return JSON.parse(raw) as SecretRoomIdentity
  } catch {
    return null
  }
}

export const useSecretGameStore = defineStore('secretGame', () => {
  const stored = loadIdentity()
  const roomCode = ref<string | null>(stored?.roomCode ?? null)
  const playerId = ref<string | null>(stored?.playerId ?? null)
  const reconnectToken = ref<string | null>(stored?.reconnectToken ?? null)
  const snapshot = ref<SecretRoomSnapshot | null>(null)
  const errorMessage = ref<SecretApiError | null>(null)

  function persistIdentity() {
    if (roomCode.value && playerId.value && reconnectToken.value) {
      sessionStorage.setItem(
        STORAGE_KEY,
        JSON.stringify({
          roomCode: roomCode.value,
          playerId: playerId.value,
          reconnectToken: reconnectToken.value,
        }),
      )
    }
  }

  function clearIdentity() {
    sessionStorage.removeItem(STORAGE_KEY)
    roomCode.value = null
    playerId.value = null
    reconnectToken.value = null
    snapshot.value = null
  }

  function connectSocket() {
    if (!roomCode.value || !playerId.value || !reconnectToken.value) return
    errorMessage.value = null
    secretGameSocket.connect(
      { roomCode: roomCode.value, playerId: playerId.value, reconnectToken: reconnectToken.value },
      (newSnapshot) => {
        snapshot.value = newSnapshot
      },
      (error) => {
        errorMessage.value = error
      },
    )
  }

  async function createDailyRoom(language: GameLanguage) {
    const result = await createSecretDailyRoomRequest({ language })
    roomCode.value = result.roomCode
    playerId.value = result.playerId
    reconnectToken.value = result.reconnectToken
    snapshot.value = result.snapshot
    persistIdentity()
  }

  function submitWord(text: string) {
    if (roomCode.value) secretGameSocket.submitWord(roomCode.value, text)
  }

  function useHint() {
    if (roomCode.value) secretGameSocket.useHint(roomCode.value)
  }

  function submitSolution(text: string) {
    if (roomCode.value) secretGameSocket.submitSolution(roomCode.value, text)
  }

  function giveUp() {
    if (roomCode.value) secretGameSocket.giveUp(roomCode.value)
  }

  function leaveRoom() {
    secretGameSocket.disconnect()
    clearIdentity()
  }

  function dismissError() {
    errorMessage.value = null
  }

  return {
    roomCode,
    playerId,
    snapshot,
    errorMessage,
    connectSocket,
    createDailyRoom,
    submitWord,
    useHint,
    submitSolution,
    giveUp,
    leaveRoom,
    dismissError,
  }
})
