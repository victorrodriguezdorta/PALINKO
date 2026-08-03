<template>
  <div class="mx-auto p-6" :class="snapshot && snapshot.status !== 'CLOSED' ? 'max-w-6xl' : 'max-w-2xl'">
    <AppHeader
      :title="isDaily ? t('room.titleDaily') : t('room.title', { code: roomCode })"
      show-back
      center-logo
      :back-label="t('room.backToHome')"
      @back="onLeave"
    />

    <p v-if="gameStore.errorMessage" class="mb-4 flex items-center gap-2 rounded bg-error-100 p-2 text-sm text-error-700">
      <span>{{ translateError(gameStore.errorMessage) }}</span>
      <CartoonButton size="sm" :color="THEME_COLORS.error500" @click="gameStore.dismissError">{{ t('common.close') }}</CartoonButton>
    </p>

    <CartoonCard v-if="needsNameToJoin" :accent="THEME_COLORS.success500">
      <template #title>{{ t('room.joinPrompt.heading', { code: roomCode }) }}</template>
      <div class="mb-3 flex flex-col items-center gap-2">
        <PlayerAvatar
          :seed="joinByLinkAvatarSeed"
          size="lg"
          editable
          :shuffle-label="t('home.avatar.shuffle')"
          @shuffle="onShuffleJoinByLinkAvatar"
        />
        <p class="text-xs text-gray-500">{{ t('home.avatar.hint') }}</p>
      </div>
      <form class="flex flex-col gap-2" @submit.prevent="onJoinByLink">
        <label class="text-sm">
          {{ t('common.yourName') }}
          <input v-model="joinByLinkName" class="w-full rounded border border-gray-300 p-2" required maxlength="24" autofocus />
        </label>
        <CartoonButton type="submit" block :color="THEME_COLORS.success500" class="mt-2" :disabled="joinByLinkLoading">
          {{ t('room.joinPrompt.submit') }}
        </CartoonButton>
      </form>
      <p v-if="joinByLinkError" class="mt-3 rounded bg-error-100 p-2 text-sm text-error-700">{{ joinByLinkError }}</p>
    </CartoonCard>

    <div v-else-if="!snapshot" class="text-gray-500">{{ t('room.connecting') }}</div>

    <template v-else>
      <section v-if="snapshot.status === 'LOBBY'">
        <div class="mb-4 flex flex-wrap items-center justify-between gap-4 rounded-2xl border-2 border-white/20 bg-white p-4 shadow-lg">
          <p>{{ t('room.lobby.shareCodeLabel') }} <strong>{{ roomCode }}</strong></p>

          <CartoonButton
            v-if="gameStore.isHost"
            :color="settingsValid ? THEME_COLORS.secondary500 : THEME_COLORS.gray400"
            :disabled="!settingsValid"
            @click="gameStore.start"
          >
            {{ t('room.lobby.startButton') }}
          </CartoonButton>
          <p v-else class="text-gray-500">{{ t('room.lobby.waitingForHost') }}</p>
        </div>

        <div class="grid gap-4 md:grid-cols-2">
          <RulesCard
            :settings="snapshot.settings"
            :is-host="gameStore.isHost"
            :max-infiltrator-count="maxInfiltratorCount"
            :max-phase-count="MAX_PHASE_COUNT"
            :player-count="snapshot.players.length"
            @update="onUpdateSettings"
            @validity-change="onSettingsValidityChange"
          />
          <PlayerRosterCard
            v-if="!isDaily"
            :players="snapshot.players"
            :host-player-id="snapshot.hostPlayerId"
            :viewer-player-id="snapshot.viewerPlayerId"
            :is-host="gameStore.isHost"
            :max-players="MAX_ROOM_PLAYERS"
            @kick="onKickPlayer"
          />
        </div>
      </section>

      <div v-else class="grid gap-4" :class="!isDaily && snapshot.status !== 'FINISHED' ? 'md:grid-cols-[16rem_1fr]' : ''">
      <aside v-if="!isDaily && snapshot.status !== 'FINISHED'" class="md:sticky md:top-4 md:self-start">
        <PlayerScoreboardCard
          :players="snapshot.players"
          :viewer-player-id="snapshot.viewerPlayerId"
          :host-player-id="snapshot.hostPlayerId"
          :is-host="gameStore.isHost"
          :max-players="MAX_ROOM_PLAYERS"
          @kick="onKickPlayer"
        />
      </aside>

      <div class="relative rounded-2xl border-2 border-white/20 bg-white p-4 shadow-lg">
      <div v-if="isDaily && (snapshot.status === 'IN_PROGRESS' || snapshot.status === 'FINISHED') && chain" class="absolute left-4 top-0 translate-y-[-90%]">
        <DailyChallengeBanner />
      </div>
      <section v-if="(snapshot.status === 'IN_PROGRESS' || snapshot.status === 'FINISHED') && chain">
        <p v-if="chain.totalPhases > 1 && chain.phase === 'VOTING'" class="mb-3 text-center text-sm font-semibold text-gray-600">
          {{ t('room.chain.phaseProgress', { current: chain.currentPhaseNumber, total: chain.totalPhases }) }}
        </p>

        <template v-if="snapshot.status === 'IN_PROGRESS' && chain.phase === 'WORD_CHAIN'">
          <ChainBoardCard
            ref="chainBoardRef"
            v-model:word-draft="wordDraft"
            :players="snapshot.players"
            :viewer-player-id="snapshot.viewerPlayerId"
            :attempts="currentPhaseAttempts"
            :current-phase-number="chain.currentPhaseNumber"
            :total-phases="chain.totalPhases"
            :infiltrator-count="chain.infiltratorCount"
            :start-word="chain.startWord"
            :your-target-word="chain.yourTargetWord"
            :current-word="chain.currentWord"
            :current-turn-player-id="chain.currentTurnPlayerId"
            :remaining-seconds="remainingSeconds"
            :is-my-turn="isMyTurn"
            :is-daily="isDaily"
            :other-typing-preview="otherTypingPreview"
            :previous-phase-target-word="previousPhaseTargetWord"
            :previous-phase-final-attempt="previousPhaseFinalAttempt"
            :can-rewind="chain.canRewind"
            :rewind-used="chain.rewindUsed"
            @typing="onTypingInput"
            @submit="onSubmitWord"
            @rewind="onRewindWord"
          />
        </template>

        <template v-else-if="snapshot.status === 'IN_PROGRESS' && chain.phase === 'VOTING'">
          <ol v-if="chain.totalPhases > 1" class="mb-3 flex flex-col gap-1 text-sm">
            <li
              v-for="phaseWord in phaseChainWords"
              :key="phaseWord.phaseNumber"
              class="rounded border px-3 py-1"
              :class="phaseWord.phaseNumber === chain.currentPhaseNumber ? 'border-secondary-400 bg-secondary-50' : 'border-gray-200 text-gray-500'"
            >
              {{ t('room.chain.phaseLabel', { number: phaseWord.phaseNumber }) }}
              <strong>{{ phaseWord.startWord }}</strong> → <strong>{{ phaseWord.targetWord }}</strong>
            </li>
          </ol>

          <p class="mb-1 text-sm text-gray-500">{{ t('room.chain.startWordLabel') }} <strong>{{ chain.startWord }}</strong></p>
          <p class="mb-4 text-sm text-gray-500">{{ t('room.chain.yourTargetLabel') }} <strong>{{ chain.yourTargetWord }}</strong></p>

          <ol class="mb-4 flex flex-col gap-1">
            <li
              v-for="attempt in currentPhaseAttempts"
              :key="attempt.id"
              class="rounded border px-3 py-1 text-sm"
              :class="attemptClass(attempt)"
            >
              <span v-if="!isDaily" class="font-semibold">{{ nameFor(attempt.authorPlayerId) }}:</span>
              <template v-if="attempt.outcome === 'SKIPPED'">
                <span class="italic text-gray-500">{{ t('room.chain.timedOut') }}</span>
              </template>
              <template v-else>
                {{ attempt.text }}
                <span class="text-xs text-gray-500">
                  {{ t('room.chain.relatedPercent', { value: attempt.relatednessToPrevious }) }}
                  <template v-if="attempt.justification"> — {{ attempt.justification }}</template>
                </span>
                <span v-if="attempt.reachedTarget" class="text-xs text-success-700">{{ t('room.chain.targetReached') }}</span>
              </template>
            </li>
            <li v-if="currentPhaseAttempts.length === 0" class="text-sm text-gray-400">{{ t('room.chain.noAttempts') }}</li>
          </ol>

          <div>
            <p class="mb-2 text-sm text-gray-500">
              {{ t('room.voting.prompt') }}
              <span v-if="remainingSeconds !== null"> {{ t('room.chain.secondsRemaining', { value: remainingSeconds }) }}</span>
            </p>
            <ul class="mb-3 flex flex-col gap-2">
              <li v-for="player in votablePlayers" :key="player.id">
                <button
                  class="w-full rounded border p-2 text-left"
                  :class="myVoteSuspectId === player.id ? 'border-secondary-500 bg-secondary-50' : 'border-gray-300'"
                  @click="onSelectVote(player.id)"
                >
                  {{ player.name }}
                </button>
                <p v-if="votersFor(player.id).length > 0" class="mt-1 pl-2 text-xs text-gray-500">
                  {{ t('room.voting.suspects', { names: votersFor(player.id).join(', ') }) }}
                </p>
              </li>
            </ul>
          </div>
        </template>

        <template v-else-if="snapshot.status === 'FINISHED' && chain.reveal">
          <div v-if="chain.reveal.infiltratorPlayerIds.length > 0 && chain.reveal.endedByInfiltratorWord" class="mb-4 rounded border border-error-300 bg-error-50 p-3">
            <p class="mb-1 font-semibold text-error-700">{{ t('room.reveal.infiltratorWordGuessed') }}</p>
            <p class="mb-1">
              {{ t('room.reveal.infiltratorsWereLabel', chain.reveal.infiltratorPlayerIds.length) }}
              <strong>{{ infiltratorNames }}</strong>
              {{ t('room.reveal.secretTarget', { word: chain.reveal.infiltratorTargetWord }) }}
            </p>
          </div>
          <template v-else-if="chain.reveal.infiltratorPlayerIds.length > 0">
            <div class="mb-4 rounded border border-gray-300 p-3">
              <p class="mb-1">
                {{ t('room.reveal.infiltratorsWereLabel', chain.reveal.infiltratorPlayerIds.length) }}
                <strong>{{ infiltratorNames }}</strong>
                {{ t('room.reveal.secretTarget', { word: chain.reveal.infiltratorTargetWord }) }}
              </p>
              <p v-if="chain.reveal.accusedPlayerId" class="mb-1">
                {{ t('room.reveal.mostVotedLabel') }} <strong>{{ nameFor(chain.reveal.accusedPlayerId) }}</strong>
              </p>
              <p v-else class="mb-1">{{ t('room.reveal.noSingleMostVoted') }}</p>
              <p class="font-semibold" :class="chain.reveal.crewWon ? 'text-success-700' : 'text-error-700'">
                {{ chain.reveal.crewWon ? t('room.reveal.crewWon') : t('room.reveal.infiltratorEscaped') }}
              </p>
            </div>

            <div class="mb-4 rounded border border-gray-300 p-3">
              <h3 class="mb-2 font-semibold">{{ t('room.reveal.votingResultsHeading') }}</h3>
              <!-- Infiltrators' own accusations never counted toward the
                   result, so they're left out here too, even though they
                   were visible live while voting was open. -->
              <ul v-if="votingResults.length > 0" class="flex flex-col gap-1 text-sm text-gray-600">
                <li v-for="entry in votingResults" :key="entry.suspectPlayerId">
                  <strong>{{ entry.suspectName }}</strong>: {{ entry.voterNames.join(', ') }}
                </li>
              </ul>
              <p v-else class="text-sm text-gray-400">{{ t('room.reveal.nobodyVoted') }}</p>
            </div>
          </template>

          <GameResultsCard
            class="mb-4"
            :chain="chain as ChainView & { reveal: NonNullable<ChainView['reveal']> }"
            :sorted-by-score="sortedByScore"
            :average-accuracy-percent="averageAccuracyPercent"
          />

          <div class="flex items-center gap-3">
            <CartoonButton v-if="gameStore.isHost && !isDaily" :color="THEME_COLORS.success500" @click="gameStore.playAgain">
              {{ t('room.reveal.playAgain') }}
            </CartoonButton>
            <CartoonButton :color="THEME_COLORS.secondary500" @click="onLeave">
              {{ t('room.backToHome') }}
            </CartoonButton>
          </div>
          <p v-if="!gameStore.isHost" class="mt-2 text-sm text-gray-500">
            {{ t('room.reveal.waitingPlayAgain') }}
          </p>
          <div v-if="isDaily" class="mt-3">
            <DailyCountdown />
          </div>
        </template>
      </section>

      <section v-else-if="snapshot.status === 'CLOSED'">
        <h2 class="mb-3 text-lg font-semibold">{{ t('room.closed.heading') }}</h2>
        <p class="mb-4 text-gray-500">{{ t('room.closed.text') }}</p>
        <CartoonButton :color="THEME_COLORS.secondary500" @click="onLeave">
          {{ t('room.closed.backNow') }}
        </CartoonButton>
      </section>
      </div>
      </div>
    </template>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { useGameStore } from '@/stores/game'
import { translateError } from '@/i18n'
import DailyCountdown from '@/components/DailyCountdown.vue'
import DailyChallengeBanner from '@/components/DailyChallengeBanner.vue'
import CartoonButton from '@/components/CartoonButton.vue'
import CartoonCard from '@/components/CartoonCard.vue'
import AppHeader from '@/components/AppHeader.vue'
import PlayerAvatar from '@/components/PlayerAvatar.vue'
import RulesCard from '@/components/RulesCard.vue'
import PlayerRosterCard from '@/components/PlayerRosterCard.vue'
import PlayerScoreboardCard from '@/components/PlayerScoreboardCard.vue'
import ChainBoardCard from '@/components/ChainBoardCard.vue'
import GameResultsCard from '@/components/GameResultsCard.vue'
import { loadOrCreateAvatarSeed, persistAvatarSeed, randomAvatarSeed } from '@/utils/avatar'
import { THEME_COLORS } from '@/assets/theme'
import { useSound } from '@/composables/useSound'
import type { ApiError, AttemptView, ChainView, GameLanguage } from '@/types/game'

// Mirrors the backend's own Room.MAXIMUM_PLAYERS — manual mirror, same
// convention already used for the GameLanguage enum.
const MAX_ROOM_PLAYERS = 10
// Mirrors the backend's own RoomSettings.MAX_PHASE_COUNT.
const MAX_PHASE_COUNT = 10

const { t } = useI18n()
const route = useRoute()
const router = useRouter()
const gameStore = useGameStore()
const { play } = useSound()

const roomCode = (route.params.code as string).trim().toUpperCase()

const joinByLinkName = ref('')
const joinByLinkAvatarSeed = ref(loadOrCreateAvatarSeed())
const joinByLinkLoading = ref(false)
const joinByLinkError = ref<string | null>(null)

function onShuffleJoinByLinkAvatar() {
  joinByLinkAvatarSeed.value = randomAvatarSeed()
  persistAvatarSeed(joinByLinkAvatarSeed.value)
}
// True until this browser tab either already holds an identity for this
// room code (rejoining/reconnecting) or successfully joins via the prompt
// below — covers the "someone opened a shared room link cold" case, which
// previously just bounced back to Home instead of letting them in.
const needsNameToJoin = ref(!gameStore.hasIdentityFor(roomCode))

async function onJoinByLink() {
  joinByLinkLoading.value = true
  joinByLinkError.value = null
  try {
    await gameStore.joinRoom(roomCode, joinByLinkName.value, joinByLinkAvatarSeed.value)
    play('roomEnter')
    needsNameToJoin.value = false
    gameStore.connectSocket()
    tickInterval = setInterval(() => {
      now.value = Date.now()
    }, 1000)
  } catch (err) {
    joinByLinkError.value = translateError(err as ApiError)
  } finally {
    joinByLinkLoading.value = false
  }
}

const wordDraft = ref('')
const chainBoardRef = ref<InstanceType<typeof ChainBoardCard> | null>(null)

const snapshot = computed(() => gameStore.snapshot)
const chain = computed(() => snapshot.value?.chain ?? null)
const isDaily = computed(() => snapshot.value?.settings.daily ?? false)

// Mirrors the backend's own Room.maxInfiltratorCount (floor(playerCount/3)),
// so the host sees the real current cap live as players join/leave.
const maxInfiltratorCount = computed(() => Math.floor((snapshot.value?.players.length ?? 0) / 3))

const infiltratorNames = computed(() =>
  (chain.value?.reveal?.infiltratorPlayerIds ?? []).map(nameFor).join(', '),
)

// The whole chain is dealt up front by the backend, so every phase's
// start/target word pair is already known and can be shown before it's
// actually reached — phaseStartWords[i] always pairs with
// yourPhaseTargetWords[i] for the same phase.
const phaseChainWords = computed(() => {
  const c = chain.value
  if (!c) return []
  return c.phaseStartWords.map((startWord, index) => ({
    phaseNumber: index + 1,
    startWord,
    targetWord: c.yourPhaseTargetWords[index] ?? '',
  }))
})

// The target word of the phase just completed, shown as a "phase cleared"
// marker at the top of the attempts column once the next phase begins —
// null during phase 1, since there's no previous phase to recap yet.
const previousPhaseTargetWord = computed(() => {
  const c = chain.value
  if (!c || c.currentPhaseNumber <= 1) return null
  return c.yourPhaseTargetWords[c.currentPhaseNumber - 2] ?? null
})

// The attempt that actually reached the previous phase's target — it lives
// under the old phase's own phaseIndex, so currentPhaseAttempts (filtered
// to the current phase) never includes it. ChainBoardCard needs the real
// attempt (not just the target word) so that word can still appear in the
// played-words column, right where it drops to once the phase marker
// pushes it down, instead of vanishing the moment the phase changes.
const previousPhaseFinalAttempt = computed(() => {
  const c = chain.value
  if (!c || c.currentPhaseNumber <= 1) return null
  const previousPhaseIndex = c.currentPhaseNumber - 2
  return c.attempts.find((attempt) => attempt.phaseIndex === previousPhaseIndex && attempt.reachedTarget) ?? null
})

// Only the current phase's own attempts count toward that phase — an
// attempt logged under an earlier phase must not be shown as if it were
// still in play now.
const currentPhaseAttempts = computed(() => {
  const c = chain.value
  if (!c) return []
  return c.attempts.filter((attempt) => attempt.phaseIndex === c.currentPhaseNumber - 1)
})

const sortedByScore = computed(() => [...(snapshot.value?.players ?? [])].sort((a, b) => b.score - a.score))

// Mean relatedness-to-previous across every ACCEPTED word of the whole
// game (every phase, not just the current one) — the single number shown
// on the reveal screen for "how accurate were the words that made it into
// the chain". null when nothing was ever accepted, so the reveal screen
// can hide the line entirely rather than show a meaningless 0%.
const averageAccuracyPercent = computed(() => {
  const accepted = (chain.value?.attempts ?? []).filter((attempt) => attempt.outcome === 'ACCEPTED')
  if (accepted.length === 0) return null
  const total = accepted.reduce((sum, attempt) => sum + attempt.relatednessToPrevious, 0)
  return Math.round(total / accepted.length)
})

function nameFor(playerId: string | null): string {
  if (!playerId) return '?'
  return snapshot.value?.players.find((p) => p.id === playerId)?.name ?? playerId
}

function attemptClass(attempt: AttemptView): string {
  if (attempt.outcome === 'REJECTED') return 'border-error-300 bg-error-50'
  if (attempt.outcome === 'SKIPPED') return 'border-gray-200 bg-gray-50'
  return 'border-gray-300'
}

const isMyTurn = computed(() => !!chain.value && chain.value.currentTurnPlayerId === gameStore.playerId)

const otherTypingPreview = computed(() => {
  const turnPlayerId = chain.value?.currentTurnPlayerId
  if (!turnPlayerId) return ''
  return gameStore.typingPreview[turnPlayerId] ?? ''
})

const votablePlayers = computed(() =>
  (snapshot.value?.players ?? []).filter((player) => player.id !== gameStore.playerId && !player.kicked),
)

const myVoteSuspectId = computed(() => {
  const myId = gameStore.playerId
  if (!myId) return null
  return chain.value?.votes.find((vote) => vote.voterPlayerId === myId)?.suspectPlayerId ?? null
})

function votersFor(suspectPlayerId: string): string[] {
  return (chain.value?.votes ?? [])
    .filter((vote) => vote.suspectPlayerId === suspectPlayerId)
    .map((vote) => nameFor(vote.voterPlayerId))
}

// Same live votes shown during VOTING, but recapped for the FINISHED
// screen with the infiltrator's own accusation dropped — it never counted
// toward the result, so showing it in the final recap would just be
// confusing.
const votingResults = computed(() => {
  const reveal = chain.value?.reveal
  if (!reveal) return []
  const voterNamesBySuspectId = new Map<string, string[]>()
  for (const vote of chain.value?.votes ?? []) {
    if (reveal.infiltratorPlayerIds.includes(vote.voterPlayerId)) continue
    const voterNames = voterNamesBySuspectId.get(vote.suspectPlayerId) ?? []
    voterNames.push(nameFor(vote.voterPlayerId))
    voterNamesBySuspectId.set(vote.suspectPlayerId, voterNames)
  }
  return Array.from(voterNamesBySuspectId.entries()).map(([suspectPlayerId, voterNames]) => ({
    suspectPlayerId,
    suspectName: nameFor(suspectPlayerId),
    voterNames,
  }))
})

// A one-second ticker driving the turn/vote countdown shown to players —
// purely presentational, the server's own PhaseScheduler is what actually
// enforces the deadline.
const now = ref(Date.now())
let tickInterval: ReturnType<typeof setInterval> | undefined
const remainingSeconds = computed(() => {
  const deadline = chain.value?.phaseDeadline
  if (!deadline) return null
  return Math.max(0, Math.floor((new Date(deadline).getTime() - now.value) / 1000))
})

// Ticks once per second for the last 10 seconds of a turn/vote deadline —
// the change-detection on remainingSeconds itself (driven by the 1s `now`
// ticker above) is what keeps this from firing more than once per second.
watch(remainingSeconds, (seconds, previousSeconds) => {
  if (seconds === null || seconds === previousSeconds) return
  if (seconds > 0 && seconds <= 10) play('timeRunningOutTick')
})

watch(
  () => chain.value?.currentTurnPlayerId,
  (newTurnPlayerId, oldTurnPlayerId) => {
    wordDraft.value = ''
    if (newTurnPlayerId) gameStore.clearTypingPreview(newTurnPlayerId)
    // Skip the very first turn of the round (oldTurnPlayerId undefined) so
    // this doesn't double up with the WORD_CHAIN-phase-start cue below.
    if (!oldTurnPlayerId || !newTurnPlayerId) return
    play(newTurnPlayerId === gameStore.playerId ? 'yourTurn' : 'turnPassed')
  },
)

watch(
  () => snapshot.value?.status,
  (status) => {
    if (status === 'CLOSED') {
      setTimeout(onLeave, 3000)
    }
  },
  { immediate: true },
)

// Reacts to phase transitions and round outcomes purely off the snapshot,
// so every client hears the same cue at the same moment regardless of
// which action (if any) of theirs caused it.
watch(
  () => chain.value?.phase,
  (phase, previousPhase) => {
    if (!phase || phase === previousPhase) return
    if (phase === 'VOTING') play('votingStart')
  },
)

watch(
  () => snapshot.value?.status,
  (status, previousStatus) => {
    if (status !== 'FINISHED' || previousStatus === status) return
    const reveal = chain.value?.reveal
    if (!reveal) return
    if (reveal.endedByInfiltratorWord) play('infiltratorWordGuessed')
    else if (reveal.infiltratorPlayerIds.length === 0 || reveal.crewWon) play('crewWon')
    else play('infiltratorEscaped')
  },
)

watch(
  () => snapshot.value?.status,
  (status, previousStatus) => {
    if (status === 'IN_PROGRESS' && previousStatus === 'LOBBY') play('hostStartGame')
  },
)

// Every new attempt (accepted/rejected/skipped/reached-target) is heard by
// all players at once, since attempts arrive as part of the shared
// snapshot rather than only to whoever submitted them.
const seenAttemptIds = new Set<string>()
watch(
  () => chain.value?.attempts,
  (attempts) => {
    if (!attempts) return
    for (const attempt of attempts) {
      if (seenAttemptIds.has(attempt.id)) continue
      seenAttemptIds.add(attempt.id)
      if (attempt.outcome === 'REJECTED') play('wordRejected')
      else if (attempt.outcome === 'SKIPPED') play('wordTimedOut')
      else if (attempt.reachedTarget) play('targetReached')
      else if (attempt.outcome === 'ACCEPTED') play('wordAccepted')
    }
  },
  { deep: true },
)

watch(
  () => snapshot.value?.players.length,
  (count, previousCount) => {
    if (previousCount === undefined || count === undefined) return
    if (count > previousCount) play('playerJoin')
    else if (count < previousCount) play('playerLeave')
  },
)

watch(
  () => gameStore.errorMessage,
  (error) => {
    if (error) play(error.code === 'KICKED' ? 'playerKicked' : 'gameError')
  },
)

// The host just removed this player from the room: the store already
// cleared this client's identity and disconnected the socket (see
// game.ts's onError handling of the KICKED code) — this only needs to
// bounce the view itself back to Home once that happens.
watch(
  () => gameStore.errorMessage?.code,
  (code) => {
    if (code === 'KICKED') {
      setTimeout(onLeave, 2000)
    }
  },
)

function onUpdateSettings(
  wordTimeSeconds: number,
  voteTimeSeconds: number,
  language: GameLanguage,
  infiltratorCount: number,
  phaseCount: number,
) {
  play('settingsChange')
  gameStore.updateSettings(wordTimeSeconds, voteTimeSeconds, language, infiltratorCount, phaseCount)
}

// RulesCard flags an invalid in-progress edit (e.g. infiltrator count over
// the cap) before it's corrected, so the start button reflects that
// immediately rather than only after the next settings update lands.
const settingsValid = ref(true)
function onSettingsValidityChange(valid: boolean) {
  settingsValid.value = valid
}

function onKickPlayer(targetPlayerId: string) {
  gameStore.kickPlayer(targetPlayerId)
}

function onTypingInput() {
  if (isMyTurn.value) gameStore.sendTyping(wordDraft.value)
}

function onSubmitWord() {
  const text = wordDraft.value.trim()
  if (!text) return
  play('wordSubmit')
  chainBoardRef.value?.submitPending(text)
  gameStore.submitWord(text)
  wordDraft.value = ''
}

function onRewindWord() {
  play('rewindUsed')
  gameStore.rewindWord()
}

function onSelectVote(suspectPlayerId: string) {
  if (myVoteSuspectId.value === suspectPlayerId) return
  play('voteCast')
  gameStore.submitVote(suspectPlayerId)
}

function onLeave() {
  gameStore.leaveRoom()
  router.push('/')
}

onMounted(() => {
  // Arriving with no identity for this room (e.g. a shared room link
  // opened cold) shows the join-by-name prompt above instead of bouncing
  // back to Home — onJoinByLink connects the socket itself once that
  // succeeds, so there's nothing to do here yet.
  if (needsNameToJoin.value) return
  gameStore.connectSocket()
  tickInterval = setInterval(() => {
    now.value = Date.now()
  }, 1000)
})

onUnmounted(() => {
  if (tickInterval) clearInterval(tickInterval)
})
</script>
