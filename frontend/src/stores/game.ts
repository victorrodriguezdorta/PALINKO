import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { createRoom as createRoomRequest, joinRoom as joinRoomRequest } from '@/services/roomService'
import { gameSocket } from '@/services/gameSocket'
import type { RoomIdentity, RoomSnapshot } from '@/types/game'

const STORAGE_KEY = 'human-or-ai:identity'

// sessionStorage, not localStorage: it is scoped to this one browser tab.
// localStorage is shared by every tab of the same origin, so opening
// several tabs to play as different players (the natural way to test or
// run a local no-login party game) would have every tab's join/create
// overwrite the same stored identity — a later tab's reload would then
// silently pick up an earlier tab's playerId/reconnectToken instead of
// its own. sessionStorage still survives a reload of this same tab, which
// is the only case reconnection needs to cover.
function loadIdentity(): RoomIdentity | null {
  const raw = sessionStorage.getItem(STORAGE_KEY)
  if (!raw) return null
  try {
    return JSON.parse(raw) as RoomIdentity
  } catch {
    return null
  }
}

export const useGameStore = defineStore('game', () => {
  const stored = loadIdentity()
  const roomCode = ref<string | null>(stored?.roomCode ?? null)
  const playerId = ref<string | null>(stored?.playerId ?? null)
  const reconnectToken = ref<string | null>(stored?.reconnectToken ?? null)
  const snapshot = ref<RoomSnapshot | null>(null)
  const errorMessage = ref<string | null>(null)

  const isHost = computed(() => !!snapshot.value && playerId.value === snapshot.value.hostPlayerId)

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

  function hasIdentityFor(code: string): boolean {
    return roomCode.value === code && !!playerId.value && !!reconnectToken.value
  }

  function connectSocket() {
    if (!roomCode.value || !playerId.value || !reconnectToken.value) return
    errorMessage.value = null
    gameSocket.connect(
      { roomCode: roomCode.value, playerId: playerId.value, reconnectToken: reconnectToken.value },
      (newSnapshot) => {
        snapshot.value = newSnapshot
      },
      (message) => {
        errorMessage.value = message
      },
    )
  }

  async function createRoom(
    hostName: string,
    totalRounds: number,
    answerTimeSeconds: number,
    voteTimeSeconds: number,
  ) {
    const result = await createRoomRequest({ hostName, totalRounds, answerTimeSeconds, voteTimeSeconds })
    roomCode.value = result.roomCode
    playerId.value = result.playerId
    reconnectToken.value = result.reconnectToken
    snapshot.value = result.snapshot
    persistIdentity()
  }

  async function joinRoom(code: string, playerName: string) {
    const result = await joinRoomRequest(code, playerName)
    roomCode.value = result.roomCode
    playerId.value = result.playerId
    reconnectToken.value = result.reconnectToken
    snapshot.value = result.snapshot
    persistIdentity()
  }

  function start() {
    if (roomCode.value) gameSocket.start(roomCode.value)
  }

  function submitAnswer(text: string) {
    if (roomCode.value) gameSocket.submitAnswer(roomCode.value, text)
  }

  function cancelAnswer() {
    if (roomCode.value) gameSocket.cancelAnswer(roomCode.value)
  }

  function submitVote(answerId: string) {
    if (roomCode.value) gameSocket.submitVote(roomCode.value, answerId)
  }

  function nextRound() {
    if (roomCode.value) gameSocket.nextRound(roomCode.value)
  }

  function playAgain() {
    if (roomCode.value) gameSocket.playAgain(roomCode.value)
  }

  function updateSettings(totalRounds: number, answerTimeSeconds: number, voteTimeSeconds: number) {
    if (roomCode.value) gameSocket.updateSettings(roomCode.value, totalRounds, answerTimeSeconds, voteTimeSeconds)
  }

  function leaveRoom() {
    gameSocket.disconnect()
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
    isHost,
    hasIdentityFor,
    connectSocket,
    createRoom,
    joinRoom,
    start,
    submitAnswer,
    cancelAnswer,
    submitVote,
    nextRound,
    playAgain,
    updateSettings,
    leaveRoom,
    dismissError,
  }
})
