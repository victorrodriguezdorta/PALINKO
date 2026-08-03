<template>
  <div class="chain-board">
    <p v-if="!isDaily && infiltratorCount > 0" class="chain-board__infiltrator-note">
      {{ t('room.chain.infiltratorCountLabel') }} <strong>{{ infiltratorCount }}</strong>
    </p>

    <div class="chain-board__main">
      <div class="chain-board__column">
        <transition-group name="chain-flash" tag="div" class="chain-board__list">
          <div
            v-for="flash in rejectedFlashes"
            :key="flash.id"
            class="chain-board__word-pill chain-board__word-pill--rejected"
          >
            <PlayerAvatar :seed="avatarSeedFor(flash.authorPlayerId)" size="sm" />
            <div class="chain-board__word-pill-body">
              <p class="chain-board__word-text">{{ flash.text }}</p>
              <p class="chain-board__word-meta">{{ t('room.chain.rejected') }}</p>
            </div>
          </div>

          <div
            v-for="flash in skippedFlashes"
            :key="flash.id"
            class="chain-board__word-pill chain-board__word-pill--rejected"
          >
            <PlayerAvatar :seed="avatarSeedFor(flash.authorPlayerId)" size="sm" />
            <div class="chain-board__word-pill-body">
              <p class="chain-board__word-text chain-board__word-text--muted">{{ t('room.chain.timedOut') }}</p>
              <p v-if="!isDaily" class="chain-board__word-meta">{{ nameFor(flash.authorPlayerId) }}</p>
            </div>
          </div>

          <div
            v-if="pendingSubmission"
            key="pending-submission"
            class="chain-board__word-pill chain-board__word-pill--pending"
          >
            <PlayerAvatar :seed="avatarSeedFor(viewerPlayerId)" size="sm" />
            <div class="chain-board__word-pill-body">
              <p class="chain-board__word-text">{{ pendingSubmission }}</p>
              <p class="chain-board__word-meta">
                <span class="chain-board__spinner" aria-hidden="true"></span>
                {{ t('room.chain.processing') }}
              </p>
            </div>
          </div>

          <div
            v-for="(entry, index) in displayEntries"
            :key="entry.key"
            class="chain-board__word-pill"
            :class="{
              'chain-board__word-pill--phase-marker': entry.kind === 'phase-target',
              'chain-board__word-pill--start': entry.kind === 'start-word',
              'chain-board__word-pill--last-played': !pendingSubmission && isLastPlayedEntry(entry, index),
            }"
          >
            <template v-if="entry.kind === 'start-word'">
              <div class="chain-board__word-pill-body">
                <p class="chain-board__word-text">{{ entry.text }}</p>
                <p class="chain-board__word-meta">{{ t('room.chain.startWordPlayed') }}</p>
              </div>
            </template>
            <template v-else-if="entry.kind === 'phase-target'">
              <div class="chain-board__phase-marker-icon" aria-hidden="true">
                <Flag :size="16" />
              </div>
              <div class="chain-board__word-pill-body">
                <p class="chain-board__word-text">{{ entry.text }}</p>
                <p class="chain-board__word-meta">{{ t('room.chain.phaseTargetReached', { number: entry.phaseNumber }) }}</p>
              </div>
            </template>
            <template v-else>
              <PlayerAvatar :seed="avatarSeedFor(entry.attempt.authorPlayerId)" size="sm" />
              <div class="chain-board__word-pill-body">
                <p class="chain-board__word-text">
                  {{ entry.attempt.text }}
                  <span v-if="entry.attempt.reachedTarget" class="chain-board__target-star" :title="t('room.chain.targetReached')">★</span>
                </p>
                <p class="chain-board__word-meta">
                  <span v-if="!isDaily" class="chain-board__author">{{ nameFor(entry.attempt.authorPlayerId) }}</span>
                  <span class="chain-board__percent">{{ t('room.chain.relatedPercent', { value: entry.attempt.relatednessToPrevious }) }}</span>
                </p>
                <p v-if="entry.attempt.justification" class="chain-board__justification">{{ entry.attempt.justification }}</p>
              </div>
            </template>
            <div
              v-if="!pendingSubmission && isLastPlayedEntry(entry, index)"
              class="chain-board__last-played-arrow"
              :title="t('room.chain.lastPlayedWord')"
              aria-hidden="true"
            ></div>
          </div>
        </transition-group>

        <p
          v-if="displayEntries.length === 0 && rejectedFlashes.length === 0 && skippedFlashes.length === 0 && !pendingSubmission"
          class="chain-board__empty"
        >
          {{ t('room.chain.noAttempts') }}
        </p>
      </div>

      <div class="chain-board__turn-slot">
        <div
          v-if="!isDaily && remainingSeconds !== null"
          class="chain-board__timer"
          :class="{ 'chain-board__timer--urgent': remainingSeconds <= 10 }"
        >
          {{ t('room.chain.secondsRemaining', { value: remainingSeconds }) }}
        </div>

        <template v-if="isMyTurn">
          <p class="chain-board__turn-hint">
            <i18n-t keypath="room.chain.relateHint" scope="global">
              <template #word><strong class="chain-board__hint-highlight">{{ currentWord }}</strong></template>
              <template #target><strong class="chain-board__hint-highlight">{{ yourTargetWord }}</strong></template>
            </i18n-t>
          </p>
          <input
            :value="wordDraft"
            class="chain-board__input"
            :placeholder="t('room.chain.inputPlaceholder')"
            @input="onInput"
            @keyup.enter="$emit('submit')"
          />
          <div class="chain-board__turn-actions">
            <CartoonButton :color="THEME_COLORS.secondary500" :disabled="!wordDraft.trim()" @click="$emit('submit')">
              {{ t('room.chain.submit') }}
            </CartoonButton>

            <div class="chain-board__rewind-wrap">
              <template v-if="confirmingRewind">
                <button
                  type="button"
                  class="chain-board__rewind-confirm-btn chain-board__rewind-confirm-btn--yes"
                  :aria-label="t('room.chain.rewindConfirmYes')"
                  @click="onConfirmRewind"
                >
                  <Check :size="16" aria-hidden="true" />
                </button>
                <button
                  type="button"
                  class="chain-board__rewind-confirm-btn chain-board__rewind-confirm-btn--cancel"
                  :aria-label="t('room.chain.rewindConfirmCancel')"
                  @click="confirmingRewind = false"
                >
                  <X :size="16" aria-hidden="true" />
                </button>
              </template>
              <button
                v-else
                type="button"
                class="chain-board__rewind-btn"
                :class="{ 'chain-board__rewind-btn--used': rewindUsed }"
                :disabled="rewindUsed || !canRewind"
                :aria-label="t('room.chain.rewindAriaLabel')"
                :title="rewindButtonTitle"
                @click="confirmingRewind = true"
              >
                <RotateCcw :size="18" aria-hidden="true" />
                <span v-if="!rewindUsed" class="chain-board__rewind-badge">1</span>
              </button>
            </div>
          </div>
        </template>
        <template v-else>
          <div class="chain-board__turn-indicator">
            <PlayerAvatar :seed="avatarSeedFor(currentTurnPlayerId)" size="sm" />
            <p class="chain-board__turn-name">
              {{ nameFor(currentTurnPlayerId) }}
            </p>
          </div>
          <p class="chain-board__typing-preview">{{ otherTypingPreview || '…' }}</p>

          <button
            type="button"
            class="chain-board__rewind-btn"
            :class="{ 'chain-board__rewind-btn--used': rewindUsed }"
            disabled
            :aria-label="t('room.chain.rewindAriaLabel')"
            :title="rewindButtonTitle"
          >
            <RotateCcw :size="18" aria-hidden="true" />
            <span v-if="!rewindUsed" class="chain-board__rewind-badge">1</span>
          </button>
        </template>
      </div>

      <div class="chain-board__target">
        <p class="chain-board__target-label">{{ t('room.chain.yourTargetLabel') }}</p>
        <div class="chain-board__target-word-wrap">
          <Transition name="chain-target-change" mode="out-in">
            <div :key="currentPhaseNumber" class="chain-board__target-word">{{ yourTargetWord }}</div>
          </Transition>
          <Transition name="chain-badge-change">
            <div
              :key="currentPhaseNumber"
              class="chain-board__phase-badge"
              :class="{ 'chain-board__phase-badge--single': totalPhases <= 1 }"
              :title="t('room.chain.phaseProgress', { current: currentPhaseNumber, total: totalPhases })"
            >
              <span class="chain-board__phase-number">{{ currentPhaseNumber }}</span>
              <span v-if="totalPhases > 1" class="chain-board__phase-total">/ {{ totalPhases }}</span>
            </div>
          </Transition>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { Check, Flag, RotateCcw, X } from 'lucide-vue-next'
import PlayerAvatar from '@/components/PlayerAvatar.vue'
import CartoonButton from '@/components/CartoonButton.vue'
import { THEME_COLORS } from '@/assets/theme'
import type { AttemptView, PlayerView } from '@/types/game'

const props = defineProps<{
  players: PlayerView[]
  viewerPlayerId: string
  attempts: AttemptView[]
  currentPhaseNumber: number
  totalPhases: number
  infiltratorCount: number
  startWord: string
  yourTargetWord: string
  currentWord: string
  currentTurnPlayerId: string | null
  remainingSeconds: number | null
  isMyTurn: boolean
  isDaily: boolean
  otherTypingPreview: string
  wordDraft: string
  // Whether the viewer's own one-per-game rewind power is still unspent
  // and there's an accepted word left to undo — independent of whose turn
  // it currently is; the button itself only actually fires it on your turn
  // (see isMyTurn in the template), staying visible-but-inert the rest of
  // the time as a reminder the power still exists.
  canRewind: boolean
  // Whether the viewer has already spent their rewind this game — once
  // true this never flips back, for the lifetime of the round.
  rewindUsed: boolean
  // The previous phase's target word, shown as a "phase cleared" marker at
  // the top of the attempts column once a new phase starts — null before
  // the second phase begins.
  previousPhaseTargetWord: string | null
  // The actual attempt that reached the previous phase's target — `attempts`
  // above is filtered to the current phase only, so this is the one way the
  // word that just ended the previous phase can still show up as a played
  // word instead of disappearing the instant the phase changes.
  previousPhaseFinalAttempt: AttemptView | null
}>()

const emit = defineEmits<{
  'update:wordDraft': [value: string]
  typing: []
  submit: []
  rewind: []
}>()

const { t } = useI18n()

function onInput(event: Event) {
  const value = (event.target as HTMLInputElement).value
  emit('update:wordDraft', value)
  emit('typing')
}

const confirmingRewind = ref(false)

function onConfirmRewind() {
  confirmingRewind.value = false
  emit('rewind')
}

const rewindButtonTitle = computed(() => {
  if (props.rewindUsed) return t('room.chain.rewindTitleUsed')
  if (!props.isMyTurn) return t('room.chain.rewindTitleNotYourTurn')
  return t('room.chain.rewindTitleAvailable')
})

function nameFor(playerId: string | null): string {
  if (!playerId) return '?'
  return props.players.find((p) => p.id === playerId)?.name ?? playerId
}

function avatarSeedFor(playerId: string | null): string {
  if (!playerId) return 'unknown'
  return props.players.find((p) => p.id === playerId)?.avatarSeed ?? playerId
}

type StartWordEntry = { kind: 'start-word'; key: string; text: string }
type PhaseTargetEntry = { kind: 'phase-target'; key: string; text: string; phaseNumber: number }
type AttemptEntry = { kind: 'attempt'; key: string; attempt: AttemptView }
type DisplayEntry = StartWordEntry | PhaseTargetEntry | AttemptEntry

// The permanent column never shows a REJECTED word — those only ever
// appear as a transient flash (see rejectedFlashes below). Every completed
// phase's target word gets a synthetic "phase cleared" marker slotted in
// chronologically right after that phase's own attempts, so it moves down
// the list like any other played word once the next phase's attempts start
// arriving — it only reads as the newest entry while it truly is the
// newest thing that happened. The very first phase's start word gets the
// same treatment pinned at the very bottom, shown as already "played" from
// the moment the board first renders. Built oldest-first then reversed, so
// interleaving markers with attempts by phase stays simple.
const displayEntries = computed<DisplayEntry[]>(() => {
  const chronological: DisplayEntry[] = []
  if (props.currentPhaseNumber === 1 && props.startWord) {
    chronological.push({ kind: 'start-word', key: 'start-word', text: props.startWord })
  }
  if (props.previousPhaseFinalAttempt) {
    chronological.push({ kind: 'attempt', key: props.previousPhaseFinalAttempt.id, attempt: props.previousPhaseFinalAttempt })
  }
  if (props.previousPhaseTargetWord) {
    chronological.push({
      kind: 'phase-target',
      key: `phase-target-${props.currentPhaseNumber - 1}`,
      text: props.previousPhaseTargetWord,
      phaseNumber: props.currentPhaseNumber - 1,
    })
  }
  for (const attempt of props.attempts) {
    if (attempt.outcome === 'REJECTED' || attempt.outcome === 'SKIPPED') continue
    chronological.push({ kind: 'attempt', key: attempt.id, attempt })
  }
  return chronological.reverse()
})

// The newest non-pending entry in the column (index 0, since it's newest
// first) is what the left-side arrow marker points at — once a submission
// is pending, that pill itself takes the "latest" spot instead, so the
// arrow on the list below it is hidden (see the template's `!pendingSubmission` guard).
function isLastPlayedEntry(entry: DisplayEntry, index: number): boolean {
  return index === 0 && entry.kind !== 'start-word'
}

// Shows the word this player just submitted immediately, before the
// backend's evaluation (accept/reject) comes back as a new snapshot — pure
// optimistic UI, cleared as soon as the attempts list actually changes.
const pendingSubmission = ref<string | null>(null)

function submitPending(text: string) {
  pendingSubmission.value = text
}

watch(
  () => props.attempts,
  () => {
    pendingSubmission.value = null
  },
)

defineExpose({ submitPending })

// Rejected attempts flash for ~1s then disappear — never persisted in the
// permanent column above. Tracked by attempt id so the same rejection
// never gets re-queued if the snapshot re-renders before it clears.
const rejectedFlashes = ref<{ id: string; text: string; authorPlayerId: string }[]>([])
const seenRejectedIds = new Set<string>()

watch(
  () => props.attempts,
  (attempts) => {
    for (const attempt of attempts) {
      if (attempt.outcome !== 'REJECTED' || seenRejectedIds.has(attempt.id)) continue
      seenRejectedIds.add(attempt.id)
      rejectedFlashes.value = [...rejectedFlashes.value, { id: attempt.id, text: attempt.text, authorPlayerId: attempt.authorPlayerId }]
      setTimeout(() => {
        rejectedFlashes.value = rejectedFlashes.value.filter((f) => f.id !== attempt.id)
      }, 1000)
    }
  },
  { immediate: true, deep: true },
)

// Skipped (timed-out) turns flash the same way rejected words do, instead
// of sitting permanently in the played-words column — same tracked-by-id
// pattern as rejectedFlashes above, kept as a separate list/set so the two
// kinds never interfere with each other's timers.
const skippedFlashes = ref<{ id: string; authorPlayerId: string }[]>([])
const seenSkippedIds = new Set<string>()

watch(
  () => props.attempts,
  (attempts) => {
    for (const attempt of attempts) {
      if (attempt.outcome !== 'SKIPPED' || seenSkippedIds.has(attempt.id)) continue
      seenSkippedIds.add(attempt.id)
      skippedFlashes.value = [...skippedFlashes.value, { id: attempt.id, authorPlayerId: attempt.authorPlayerId }]
      setTimeout(() => {
        skippedFlashes.value = skippedFlashes.value.filter((f) => f.id !== attempt.id)
      }, 1000)
    }
  },
  { immediate: true, deep: true },
)
</script>

<style scoped>
.chain-board {
  display: flex;
  flex-direction: column;
  gap: 1rem;
  font-family: 'Outfit', sans-serif;
}

.chain-board__timer {
  position: absolute;
  top: -0.6rem;
  right: -0.6rem;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 0.3rem 0.7rem;
  border-radius: 9999px;
  border: 2px solid var(--color-secondary-500);
  background: var(--color-secondary-50);
  color: var(--color-secondary-700);
  font-family: 'Fredoka', 'Outfit', sans-serif;
  font-weight: 700;
  font-size: 0.85rem;
  line-height: 1;
  white-space: nowrap;
  z-index: 2;
}

.chain-board__timer--urgent {
  border-color: var(--color-error-500);
  background: var(--color-error-50);
  color: var(--color-error-700);
  animation: chain-board-timer-pulse 1s ease-in-out infinite;
}

@keyframes chain-board-timer-pulse {
  0%,
  100% {
    transform: scale(1);
  }
  50% {
    transform: scale(1.06);
  }
}

@media (prefers-reduced-motion: reduce) {
  .chain-board__timer--urgent {
    animation: none;
  }
}

.chain-board__phase-badge {
  position: absolute;
  top: -0.6rem;
  right: -0.6rem;
  z-index: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 0.1rem;
  width: 2.1rem;
  height: 2.1rem;
  border-radius: 9999px;
  background: var(--color-accent-500);
  border: 2px solid var(--color-accent-700);
  box-shadow: 0 3px 0 color-mix(in srgb, var(--color-accent-700) 70%, var(--color-black) 15%);
  color: var(--color-white);
  font-family: 'Fredoka', 'Outfit', sans-serif;
}

.chain-board__phase-badge--single {
  background: var(--color-secondary-500);
  border-color: var(--color-secondary-700);
  box-shadow: 0 3px 0 color-mix(in srgb, var(--color-secondary-700) 70%, var(--color-black) 15%);
}

.chain-board__phase-number {
  font-size: 0.8rem;
  font-weight: 700;
  line-height: 1;
}

.chain-board__phase-total {
  font-size: 0.55rem;
  font-weight: 600;
  opacity: 0.85;
}

.chain-board__infiltrator-note {
  font-size: 0.8rem;
  color: var(--color-gray-500);
}

.chain-board__main {
  display: grid;
  grid-template-columns: 1fr;
  gap: 1rem;
  align-items: start;
}

/* Mobile reading order: target word first, then the input, then the
   list of played words — reversed from the DOM/desktop order below. */
.chain-board__column {
  order: 3;
}

.chain-board__turn-slot {
  order: 2;
}

.chain-board__target {
  order: 1;
}

@media (min-width: 768px) {
  .chain-board__main {
    grid-template-columns: minmax(0, 1fr) 14rem minmax(0, 10rem);
  }

  .chain-board__column,
  .chain-board__turn-slot,
  .chain-board__target {
    order: initial;
  }
}

.chain-board__column {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
  min-width: 0;
}

.chain-board__list {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
  max-height: 22rem;
  overflow-y: auto;
  padding-right: 0.15rem;
  padding-left: 0.9rem;
}

.chain-board__empty {
  font-size: 0.85rem;
  color: var(--color-gray-400);
  font-style: italic;
}

.chain-board__word-pill {
  position: relative;
  display: flex;
  align-items: center;
  gap: 0.6rem;
  padding: 0.55rem 0.75rem;
  border-radius: 1.1rem;
  background: var(--color-white);
  border: 3px solid var(--color-gray-200);
  box-shadow: 0 3px 0 color-mix(in srgb, var(--color-gray-300) 60%, var(--color-black) 10%);
}

.chain-board__word-pill--rejected {
  border-color: var(--color-error-300);
  background: var(--color-error-50);
  box-shadow: 0 3px 0 color-mix(in srgb, var(--color-error-300) 60%, var(--color-black) 10%);
}

.chain-board__word-pill--phase-marker,
.chain-board__word-pill--start {
  border-color: var(--color-success-500);
  background: var(--color-success-50);
  box-shadow: 0 3px 0 color-mix(in srgb, var(--color-success-500) 60%, var(--color-black) 10%);
}

.chain-board__word-pill--start .chain-board__word-text {
  color: var(--color-success-700);
}

.chain-board__word-pill--pending {
  border-color: var(--color-secondary-300);
  background: var(--color-secondary-50);
  box-shadow: 0 3px 0 color-mix(in srgb, var(--color-secondary-300) 60%, var(--color-black) 10%);
  opacity: 0.85;
}

.chain-board__word-pill--last-played {
  padding: 0.65rem 0.85rem;
  border-width: 3px;
  border-color: var(--color-secondary-500);
  background: var(--color-secondary-50);
  box-shadow: 0 4px 0 color-mix(in srgb, var(--color-secondary-500) 60%, var(--color-black) 15%);
}

.chain-board__word-pill--last-played .chain-board__word-text {
  font-size: 1.05rem;
  font-weight: 700;
  color: var(--color-secondary-700);
}

.chain-board__word-pill--last-played .chain-board__word-meta {
  font-size: 0.75rem;
}

.chain-board__spinner {
  display: inline-block;
  width: 0.7rem;
  height: 0.7rem;
  border: 2px solid var(--color-secondary-300);
  border-top-color: var(--color-secondary-600);
  border-radius: 9999px;
  animation: chain-board-spin 0.7s linear infinite;
}

@keyframes chain-board-spin {
  to {
    transform: rotate(360deg);
  }
}

@media (prefers-reduced-motion: reduce) {
  .chain-board__spinner {
    animation: none;
  }
}

.chain-board__last-played-arrow {
  position: absolute;
  left: -0.9rem;
  top: 50%;
  transform: translateY(-50%);
  width: 0;
  height: 0;
  border-top: 0.4rem solid transparent;
  border-bottom: 0.4rem solid transparent;
  border-left: 0.55rem solid var(--color-secondary-500);
}

.chain-board__phase-marker-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 2.5rem;
  height: 2.5rem;
  flex-shrink: 0;
  border-radius: 9999px;
  background: var(--color-success-500);
  color: var(--color-white);
}

.chain-board__word-pill-body {
  min-width: 0;
  flex: 1;
}

.chain-board__word-text {
  font-family: 'Fredoka', 'Outfit', sans-serif;
  font-weight: 600;
  font-size: 0.95rem;
  color: var(--color-gray-800);
  overflow-wrap: anywhere;
}

.chain-board__word-text--muted {
  font-style: italic;
  font-weight: 500;
  color: var(--color-gray-500);
}

.chain-board__target-star {
  color: var(--color-warning-500);
}

.chain-board__word-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 0.4rem;
  font-size: 0.7rem;
  color: var(--color-gray-500);
}

.chain-board__author {
  font-weight: 600;
  color: var(--color-secondary-700);
}

.chain-board__justification {
  margin-top: 0.15rem;
  font-size: 0.72rem;
  font-style: italic;
  color: var(--color-gray-500);
}

.chain-board__turn-slot {
  position: relative;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 0.5rem;
  padding: 0.85rem;
  border-radius: 1.25rem;
  border: 3px dashed var(--color-gray-300);
  background: var(--color-gray-50);
  text-align: center;
  min-height: 8rem;
  justify-content: center;
}

.chain-board__turn-hint {
  font-size: 0.75rem;
  color: var(--color-gray-600);
}

.chain-board__hint-highlight {
  color: var(--color-secondary-700);
}

.chain-board__input {
  width: 100%;
  border-radius: 0.9rem;
  border: 3px solid var(--color-secondary-400);
  padding: 0.5rem 0.75rem;
  font-family: 'Outfit', sans-serif;
  text-align: center;
}

.chain-board__turn-actions {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 0.6rem;
}

.chain-board__rewind-wrap {
  display: flex;
  align-items: center;
  gap: 0.35rem;
}

.chain-board__rewind-btn {
  position: relative;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 2.6rem;
  height: 2.6rem;
  flex-shrink: 0;
  border-radius: 9999px;
  border: 3px solid var(--color-accent-700);
  background: var(--color-accent-500);
  color: var(--color-white);
  box-shadow: 0 4px 0 color-mix(in srgb, var(--color-accent-700) 70%, var(--color-black) 15%);
  cursor: pointer;
  transform: translateY(0);
  transition:
    transform 0.15s cubic-bezier(0.34, 1.56, 0.64, 1),
    box-shadow 0.15s ease,
    opacity 0.15s ease;
}

.chain-board__rewind-btn:hover:not(:disabled) {
  transform: translateY(-2px) scale(1.05);
  box-shadow: 0 6px 0 color-mix(in srgb, var(--color-accent-700) 70%, var(--color-black) 15%);
}

.chain-board__rewind-btn:active:not(:disabled) {
  transform: translateY(2px) scale(0.97);
  box-shadow: 0 2px 0 color-mix(in srgb, var(--color-accent-700) 70%, var(--color-black) 15%);
}

/* Still your unspent power, just not playable this instant (not your
   turn yet) — kept visible but faded, rather than hidden, as a reminder
   it's still there for later. */
.chain-board__rewind-btn:disabled:not(.chain-board__rewind-btn--used) {
  opacity: 0.45;
  cursor: not-allowed;
  transform: none;
}

/* Permanently spent for the rest of the game — flattened to gray with no
   badge (see the template's v-if="!rewindUsed" on the badge itself). */
.chain-board__rewind-btn--used {
  border-color: var(--color-gray-400);
  background: var(--color-gray-300);
  box-shadow: 0 3px 0 var(--color-gray-400);
  opacity: 1;
  cursor: not-allowed;
  transform: none;
}

.chain-board__rewind-badge {
  position: absolute;
  top: -0.4rem;
  right: -0.4rem;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 1.15rem;
  height: 1.15rem;
  border-radius: 9999px;
  background: var(--color-error-500);
  border: 2px solid var(--color-white);
  color: var(--color-white);
  font-family: 'Fredoka', 'Outfit', sans-serif;
  font-size: 0.65rem;
  font-weight: 700;
  line-height: 1;
}

.chain-board__rewind-confirm-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 2.2rem;
  height: 2.2rem;
  flex-shrink: 0;
  border-radius: 9999px;
  cursor: pointer;
  transition:
    transform 0.15s ease,
    background 0.15s ease;
}

.chain-board__rewind-confirm-btn:hover {
  transform: scale(1.08);
}

.chain-board__rewind-confirm-btn--yes {
  border: 2px solid var(--color-success-500);
  background: var(--color-success-50);
  color: var(--color-success-700);
}

.chain-board__rewind-confirm-btn--yes:hover {
  background: var(--color-success-100);
}

.chain-board__rewind-confirm-btn--cancel {
  border: 2px solid var(--color-error-500);
  background: var(--color-error-50);
  color: var(--color-error-700);
}

.chain-board__rewind-confirm-btn--cancel:hover {
  background: var(--color-error-100);
}

.chain-board__turn-indicator {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 0.35rem;
}

.chain-board__turn-name {
  font-size: 0.8rem;
  font-weight: 600;
  color: var(--color-gray-700);
}

.chain-board__typing-preview {
  min-height: 1.2rem;
  font-size: 0.9rem;
  color: var(--color-gray-600);
  overflow-wrap: anywhere;
}

.chain-board__target {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 0.4rem;
}

.chain-board__target-label {
  font-size: 0.75rem;
  font-weight: 600;
  color: var(--color-gray-500);
  text-align: center;
}

.chain-board__target-word-wrap {
  position: relative;
}

.chain-target-change-enter-active {
  transition:
    opacity 0.35s ease,
    transform 0.35s cubic-bezier(0.34, 1.56, 0.64, 1);
}

.chain-target-change-leave-active {
  transition:
    opacity 0.2s ease,
    transform 0.2s ease;
  position: absolute;
  inset: 0;
}

.chain-target-change-enter-from {
  opacity: 0;
  transform: scale(0.6) rotate(-8deg);
}

.chain-target-change-leave-to {
  opacity: 0;
  transform: scale(0.85);
}

.chain-badge-change-enter-active {
  transition:
    opacity 0.3s ease 0.15s,
    transform 0.3s cubic-bezier(0.34, 1.56, 0.64, 1) 0.15s;
}

.chain-badge-change-leave-active {
  transition:
    opacity 0.15s ease,
    transform 0.15s ease;
}

.chain-badge-change-enter-from {
  opacity: 0;
  transform: scale(0.4) rotate(20deg);
}

.chain-badge-change-leave-to {
  opacity: 0;
  transform: scale(0.6);
}

@media (prefers-reduced-motion: reduce) {
  .chain-target-change-enter-active,
  .chain-target-change-leave-active,
  .chain-badge-change-enter-active,
  .chain-badge-change-leave-active {
    transition: none;
  }
}

.chain-board__target-word {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0.9rem 1.1rem;
  min-width: 6rem;
  border-radius: 255px 15px 225px 15px / 15px 225px 15px 255px;
  background: var(--color-accent-100);
  border: 3px solid var(--color-accent-500);
  box-shadow: 0 4px 0 color-mix(in srgb, var(--color-accent-500) 60%, var(--color-black) 15%);
  font-family: 'Fredoka', 'Outfit', sans-serif;
  font-weight: 700;
  font-size: 1.1rem;
  color: var(--color-accent-700);
  text-align: center;
  overflow-wrap: anywhere;
}

.chain-flash-enter-active {
  transition:
    opacity 0.2s ease,
    transform 0.2s ease;
}

.chain-flash-leave-active {
  transition:
    opacity 0.35s ease,
    transform 0.35s ease;
  position: relative;
}

.chain-flash-enter-from {
  opacity: 0;
  transform: translateY(-6px);
}

.chain-flash-leave-to {
  opacity: 0;
  transform: scale(0.9);
}

@media (prefers-reduced-motion: reduce) {
  .chain-flash-enter-active,
  .chain-flash-leave-active {
    transition: none;
  }
}
</style>
