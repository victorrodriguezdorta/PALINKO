import { defineStore } from 'pinia'
import { ref, computed, watch } from 'vue'
import {
  createRoom as createRoomRequest,
  createDailyRoom as createDailyRoomRequest,
  joinRoom as joinRoomRequest,
} from '@/services/roomService'
import { gameSocket } from '@/services/gameSocket'
import { i18n } from '@/i18n'
import { localeForGameLanguage } from '@/i18n/languages'
import { useLocaleStore } from '@/stores/locale'
import type { ApiError, GameLanguage, RoomIdentity, RoomSnapshot } from '@/types/game'

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
  const errorMessage = ref<ApiError | null>(null)
  const localeStore = useLocaleStore()
  // Live preview of whatever the current turn player is typing, keyed by
  // playerId — purely ephemeral UI state, never persisted or reconciled
  // against the snapshot.
  const typingPreview = ref<Record<string, string>>({})

  const isHost = computed(() => !!snapshot.value && playerId.value === snapshot.value.hostPlayerId)

  // Keeps the active UI locale in lockstep with the room's own language
  // (fixed by whichever language the host chose) — covers create, join,
  // reconnect, and the host changing it from the lobby settings panel
  // alike, since all of them just arrive as a new snapshot.
  watch(
    () => snapshot.value?.settings.language,
    (language) => {
      if (language) i18n.global.locale.value = localeForGameLanguage(language)
    },
  )

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
    typingPreview.value = {}
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
      (error) => {
        // The host removed this player from the room: there is no room
        // left for this client to keep showing, so drop the stored
        // identity/socket the same way leaveRoom does — RoomView reacts to
        // the KICKED code on errorMessage to redirect back to Home.
        if (error.code === 'KICKED') {
          gameSocket.disconnect()
          clearIdentity()
        }
        errorMessage.value = error
      },
      (typing) => {
        typingPreview.value = { ...typingPreview.value, [typing.playerId]: typing.text }
      },
    )
  }

  // Room rules (turn/vote timers, max turns) are only ever editable from
  // inside the LOBBY (see updateSettings below) — creating a room never
  // takes them, so the server always starts a fresh room with its own
  // fixed defaults. Language is the one exception: it's the host's own
  // chosen language, so it has to be supplied here.
  async function createRoom(hostName: string, avatarSeed: string, language: GameLanguage) {
    const result = await createRoomRequest({ hostName, avatarSeed, language })
    roomCode.value = result.roomCode
    playerId.value = result.playerId
    reconnectToken.value = result.reconnectToken
    snapshot.value = result.snapshot
    persistIdentity()
  }

  // Today's daily challenge: a solo, anonymous room that's already
  // IN_PROGRESS and fixed for everyone (see backend RoomSettings.daily) —
  // there is no rules form and no name to type, the backend always names
  // the sole player "#".
  async function createDailyRoom(language: GameLanguage) {
    const result = await createDailyRoomRequest({ language })
    roomCode.value = result.roomCode
    playerId.value = result.playerId
    reconnectToken.value = result.reconnectToken
    snapshot.value = result.snapshot
    persistIdentity()
  }

  async function joinRoom(code: string, playerName: string, avatarSeed: string) {
    const result = await joinRoomRequest(code, playerName, avatarSeed)
    roomCode.value = result.roomCode
    playerId.value = result.playerId
    reconnectToken.value = result.reconnectToken
    snapshot.value = result.snapshot
    persistIdentity()
  }

  function start() {
    if (roomCode.value) gameSocket.start(roomCode.value)
  }

  function submitWord(text: string) {
    if (roomCode.value) gameSocket.submitWord(roomCode.value, text)
  }

  function sendTyping(text: string) {
    if (roomCode.value) gameSocket.sendTyping(roomCode.value, text)
  }

  // Drops a stale preview once its author's turn has ended (or a new one
  // begins), so a player's leftover text from a previous turn never
  // flashes back up before they've typed anything new this time.
  function clearTypingPreview(forPlayerId: string) {
    if (!(forPlayerId in typingPreview.value)) return
    const next = { ...typingPreview.value }
    delete next[forPlayerId]
    typingPreview.value = next
  }

  function submitVote(suspectPlayerId: string) {
    if (roomCode.value) gameSocket.submitVote(roomCode.value, suspectPlayerId)
  }

  function playAgain() {
    if (roomCode.value) gameSocket.playAgain(roomCode.value)
  }

  function kickPlayer(targetPlayerId: string) {
    if (roomCode.value) gameSocket.kickPlayer(roomCode.value, targetPlayerId)
  }

  function updateSettings(
    wordTimeSeconds: number,
    voteTimeSeconds: number,
    language: GameLanguage,
    infiltratorCount: number,
    phaseCount: number,
  ) {
    if (roomCode.value)
      gameSocket.updateSettings(
        roomCode.value, wordTimeSeconds, voteTimeSeconds, language, infiltratorCount, phaseCount)
  }

  function leaveRoom() {
    gameSocket.disconnect()
    clearIdentity()
    // The room's language (if any) no longer applies once we're back on
    // the Home screen — restore whatever the visitor had picked for
    // themselves before they created/joined this room.
    localeStore.applyPreferred()
  }

  function dismissError() {
    errorMessage.value = null
  }

  return {
    roomCode,
    playerId,
    snapshot,
    errorMessage,
    typingPreview,
    isHost,
    hasIdentityFor,
    connectSocket,
    createRoom,
    createDailyRoom,
    joinRoom,
    start,
    submitWord,
    sendTyping,
    clearTypingPreview,
    submitVote,
    playAgain,
    kickPlayer,
    updateSettings,
    leaveRoom,
    dismissError,
  }
})
