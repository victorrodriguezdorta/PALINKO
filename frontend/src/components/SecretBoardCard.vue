<template>
  <div class="secret-board">
    <div class="secret-board__quality">
      <p class="secret-board__quality-label">
        <EyeOff :size="14" aria-hidden="true" />
        {{ t('secret.room.hiddenQualityLabel') }}
      </p>
      <div class="secret-board__quality-word">???</div>

      <form class="secret-board__solve-form" @submit.prevent="$emit('submitSolution')">
        <input
          :value="solutionDraft"
          type="text"
          class="secret-board__solve-input"
          :placeholder="t('secret.room.solveInputPlaceholder')"
          autocomplete="off"
          @input="onSolveInput"
        />
        <CartoonButton
          type="submit"
          size="sm"
          :color="THEME_COLORS.warning500"
          :disabled="!solutionDraft.trim()"
        >
          {{ t('secret.room.solveSubmit') }}
        </CartoonButton>
      </form>

      <TransitionGroup name="secret-solve-fade" tag="ul" class="secret-board__solve-list">
        <li
          v-for="attempt in recentSolutionAttempts"
          :key="attempt.id"
          class="secret-board__solve-pill"
          :class="`secret-board__solve-pill--${attempt.matchLevel.toLowerCase()}`"
        >
          <component :is="matchIcon(attempt.matchLevel)" :size="14" aria-hidden="true" />
          <span class="secret-board__solve-text">"{{ attempt.formulationText }}"</span>
          <span class="secret-board__solve-level">{{ matchLabel(attempt.matchLevel) }}</span>
        </li>
      </TransitionGroup>
    </div>

    <div class="secret-board__columns">
      <div class="secret-board__column secret-board__column--accepted">
        <h3 class="secret-board__column-title">
          <CheckCircle2 :size="16" aria-hidden="true" />
          {{ t('secret.room.acceptedHeading', { count: acceptedAttempts.length }) }}
        </h3>
        <TransitionGroup name="secret-word-pop" tag="ul" class="secret-board__word-list">
          <li v-for="attempt in acceptedAttempts" :key="attempt.id" class="secret-board__word-pill secret-board__word-pill--accepted">
            <span class="secret-board__word-text">{{ attempt.text }}</span>
            <span v-if="attempt.source === 'AI'" class="secret-board__ai-tag">
              <Sparkles :size="10" aria-hidden="true" />
              {{ t('secret.room.aiVerifiedTag') }}
            </span>
          </li>
        </TransitionGroup>
        <p v-if="acceptedAttempts.length === 0" class="secret-board__empty">{{ t('secret.room.noAcceptedYet') }}</p>
      </div>

      <div class="secret-board__column secret-board__column--rejected">
        <h3 class="secret-board__column-title">
          <XCircle :size="16" aria-hidden="true" />
          {{ t('secret.room.rejectedHeading', { count: rejectedAttempts.length }) }}
        </h3>
        <TransitionGroup name="secret-word-pop" tag="ul" class="secret-board__word-list">
          <li v-for="attempt in rejectedAttempts" :key="attempt.id" class="secret-board__word-pill secret-board__word-pill--rejected">
            <span class="secret-board__word-text">{{ attempt.text }}</span>
            <span v-if="attempt.source === 'AI'" class="secret-board__ai-tag">
              <Sparkles :size="10" aria-hidden="true" />
              {{ t('secret.room.aiVerifiedTag') }}
            </span>
          </li>
        </TransitionGroup>
        <p v-if="rejectedAttempts.length === 0" class="secret-board__empty">{{ t('secret.room.noRejectedYet') }}</p>
      </div>
    </div>

    <div class="secret-board__input-zone">
      <p class="secret-board__hint-row">
        {{ t('secret.room.failuresLabel', { count: totalFailures, perHint: failuresPerHint }) }}
      </p>

      <form class="secret-board__word-form" @submit.prevent="$emit('submitWord')">
        <input
          :value="wordDraft"
          type="text"
          class="secret-board__word-input"
          :placeholder="t('secret.room.wordInputPlaceholder')"
          autocomplete="off"
          autofocus
          @input="onWordInput"
        />
        <CartoonButton type="submit" :color="THEME_COLORS.secondary500" :disabled="!wordDraft.trim()">
          {{ t('secret.room.wordSubmit') }}
        </CartoonButton>
      </form>

      <button
        type="button"
        class="secret-board__hint-btn"
        :class="{ 'secret-board__hint-btn--available': hintAvailableCount > 0 }"
        :disabled="hintAvailableCount <= 0"
        @click="$emit('useHint')"
      >
        <Lightbulb :size="18" aria-hidden="true" />
        {{ t('secret.room.useHint') }}
        <span v-if="hintAvailableCount > 0" class="secret-board__hint-badge">{{ hintAvailableCount }}</span>
      </button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import { CheckCircle2, EyeOff, Lightbulb, Sparkles, XCircle, CircleHelp, TriangleAlert } from 'lucide-vue-next'
import CartoonButton from '@/components/CartoonButton.vue'
import { THEME_COLORS } from '@/assets/theme'
import type { SecretSolutionAttemptView, SecretWordAttemptView, SolutionMatchLevel } from '@/types/secretGame'

const props = defineProps<{
  acceptedAttempts: SecretWordAttemptView[]
  rejectedAttempts: SecretWordAttemptView[]
  solutionAttempts: SecretSolutionAttemptView[]
  totalFailures: number
  failuresPerHint: number
  hintAvailableCount: number
  wordDraft: string
  solutionDraft: string
}>()

const emit = defineEmits<{
  'update:wordDraft': [value: string]
  'update:solutionDraft': [value: string]
  submitWord: []
  submitSolution: []
  useHint: []
}>()

const { t } = useI18n()

// Most recent attempts first, capped so a long guessing session doesn't
// push the word-entry zone below the fold — same "recent only" pattern as
// ChainBoardCard's rejected-flash list, just persistent instead of timed.
const recentSolutionAttempts = computed(() => [...props.solutionAttempts].reverse().slice(0, 4))

function matchIcon(level: SolutionMatchLevel) {
  if (level === 'CORRECT') return CheckCircle2
  if (level === 'PARTIAL') return TriangleAlert
  return CircleHelp
}

function matchLabel(level: SolutionMatchLevel): string {
  if (level === 'CORRECT') return t('secret.room.matchLevel.correct')
  if (level === 'PARTIAL') return t('secret.room.matchLevel.partial')
  return t('secret.room.matchLevel.incorrect')
}

function onWordInput(event: Event) {
  emit('update:wordDraft', (event.target as HTMLInputElement).value)
}

function onSolveInput(event: Event) {
  emit('update:solutionDraft', (event.target as HTMLInputElement).value)
}
</script>

<style scoped>
.secret-board {
  display: grid;
  grid-template-columns: 1fr;
  gap: 1rem;
  font-family: 'Outfit', sans-serif;
}

/* Mobile reading order per design: hidden-quality box first, then the
   accepted/rejected columns, then the word-entry + hint zone last. */
.secret-board__quality {
  order: 1;
}

.secret-board__columns {
  order: 2;
}

.secret-board__input-zone {
  order: 3;
}

@media (min-width: 900px) {
  .secret-board {
    grid-template-columns: 1fr 16rem;
    grid-template-rows: auto auto;
  }

  .secret-board__quality {
    order: initial;
    grid-column: 2;
    grid-row: 1 / span 2;
  }

  .secret-board__columns {
    order: initial;
    grid-column: 1;
    grid-row: 1;
  }

  .secret-board__input-zone {
    order: initial;
    grid-column: 1;
    grid-row: 2;
  }
}

/* --- Hidden quality card (red, top) --- */
.secret-board__quality {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 0.6rem;
  padding: 1rem;
  border-radius: 1.25rem;
  border: 3px solid var(--color-error-500);
  background: var(--color-error-50);
  box-shadow: 0 4px 0 color-mix(in srgb, var(--color-error-500) 55%, var(--color-black) 15%);
  text-align: center;
}

.secret-board__quality-label {
  display: inline-flex;
  align-items: center;
  gap: 0.35rem;
  margin: 0;
  font-size: 0.7rem;
  font-weight: 700;
  letter-spacing: 0.04em;
  text-transform: uppercase;
  color: var(--color-error-700);
}

.secret-board__quality-word {
  display: flex;
  align-items: center;
  justify-content: center;
  min-width: 8rem;
  padding: 0.85rem 1.2rem;
  border-radius: 255px 15px 225px 15px / 15px 225px 15px 255px;
  background: var(--color-white);
  border: 3px dashed var(--color-error-500);
  font-family: 'Fredoka', 'Outfit', sans-serif;
  font-weight: 700;
  font-size: 1.6rem;
  letter-spacing: 0.3em;
  color: var(--color-error-500);
}

.secret-board__solve-form {
  display: flex;
  width: 100%;
  gap: 0.5rem;
}

.secret-board__solve-input {
  flex: 1;
  min-width: 0;
  border-radius: 0.9rem;
  border: 3px solid var(--color-error-300);
  padding: 0.5rem 0.75rem;
  font-family: 'Outfit', sans-serif;
  background: var(--color-white);
}

.secret-board__solve-input:focus {
  outline: none;
  border-color: var(--color-error-500);
}

.secret-board__solve-list {
  display: flex;
  flex-direction: column;
  gap: 0.4rem;
  width: 100%;
  list-style: none;
  margin: 0;
  padding: 0;
  max-height: 9rem;
  overflow-y: auto;
}

.secret-board__solve-pill {
  display: flex;
  align-items: center;
  gap: 0.4rem;
  padding: 0.35rem 0.6rem;
  border-radius: 0.75rem;
  font-size: 0.75rem;
  text-align: left;
  border: 2px solid transparent;
}

.secret-board__solve-pill--correct {
  background: var(--color-success-50);
  border-color: var(--color-success-500);
  color: var(--color-success-700);
}

.secret-board__solve-pill--partial {
  background: var(--color-warning-50, #fffaeb);
  border-color: var(--color-warning-500);
  color: var(--color-warning-700, #b54708);
}

.secret-board__solve-pill--incorrect {
  background: var(--color-gray-50);
  border-color: var(--color-gray-300);
  color: var(--color-gray-600);
}

.secret-board__solve-text {
  flex: 1;
  min-width: 0;
  overflow-wrap: anywhere;
}

.secret-board__solve-level {
  flex-shrink: 0;
  font-weight: 700;
  white-space: nowrap;
}

/* --- Accepted / rejected columns (center) --- */
.secret-board__columns {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 0.75rem;
}

.secret-board__column {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
  padding: 0.85rem;
  border-radius: 1.1rem;
  min-height: 10rem;
}

.secret-board__column--accepted {
  border: 3px solid var(--color-success-500);
  background: var(--color-success-50);
  box-shadow: 0 4px 0 color-mix(in srgb, var(--color-success-500) 55%, var(--color-black) 15%);
}

.secret-board__column--rejected {
  border: 3px solid var(--color-secondary-500);
  background: var(--color-secondary-50);
  box-shadow: 0 4px 0 color-mix(in srgb, var(--color-secondary-500) 55%, var(--color-black) 15%);
}

.secret-board__column-title {
  display: flex;
  align-items: center;
  gap: 0.35rem;
  margin: 0;
  font-family: 'Fredoka', 'Outfit', sans-serif;
  font-size: 0.8rem;
  font-weight: 700;
}

.secret-board__column--accepted .secret-board__column-title {
  color: var(--color-success-700);
}

.secret-board__column--rejected .secret-board__column-title {
  color: var(--color-secondary-700);
}

.secret-board__word-list {
  display: flex;
  flex-direction: column;
  gap: 0.4rem;
  list-style: none;
  margin: 0;
  padding: 0;
  max-height: 16rem;
  overflow-y: auto;
}

.secret-board__word-pill {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 0.35rem;
  padding: 0.4rem 0.65rem;
  border-radius: 0.85rem;
  background: var(--color-white);
  font-size: 0.85rem;
}

.secret-board__word-pill--accepted {
  border: 2px solid var(--color-success-300);
}

.secret-board__word-pill--rejected {
  border: 2px solid var(--color-secondary-300);
}

.secret-board__word-text {
  font-family: 'Fredoka', 'Outfit', sans-serif;
  font-weight: 600;
  color: var(--color-gray-800);
  overflow-wrap: anywhere;
}

.secret-board__ai-tag {
  display: inline-flex;
  align-items: center;
  gap: 0.2rem;
  padding: 0.1rem 0.4rem;
  border-radius: 9999px;
  background: var(--color-accent-100);
  color: var(--color-accent-700);
  font-size: 0.6rem;
  font-weight: 700;
  letter-spacing: 0.02em;
  text-transform: uppercase;
  white-space: nowrap;
}

.secret-board__empty {
  font-size: 0.8rem;
  font-style: italic;
  color: var(--color-gray-400);
}

/* --- Word entry + hint zone (gray, bottom) --- */
.secret-board__input-zone {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 0.6rem;
  padding: 1rem;
  border-radius: 1.25rem;
  border: 3px solid var(--color-gray-400);
  background: var(--color-gray-50);
}

.secret-board__hint-row {
  margin: 0;
  font-size: 0.75rem;
  color: var(--color-gray-500);
  text-align: center;
}

.secret-board__word-form {
  display: flex;
  width: 100%;
  gap: 0.5rem;
}

.secret-board__word-input {
  flex: 1;
  min-width: 0;
  border-radius: 0.9rem;
  border: 3px solid var(--color-secondary-400);
  padding: 0.55rem 0.85rem;
  font-family: 'Outfit', sans-serif;
  text-align: center;
  background: var(--color-white);
}

.secret-board__word-input:focus {
  outline: none;
  border-color: var(--color-secondary-600);
}

.secret-board__hint-btn {
  position: relative;
  display: inline-flex;
  align-items: center;
  gap: 0.45rem;
  padding: 0.5rem 1.1rem;
  border-radius: 0.9rem;
  border: 3px solid var(--color-gray-400);
  background: var(--color-gray-200);
  color: var(--color-gray-500);
  font-family: 'Fredoka', 'Outfit', sans-serif;
  font-weight: 600;
  cursor: not-allowed;
  transition:
    transform 0.15s cubic-bezier(0.34, 1.56, 0.64, 1),
    box-shadow 0.15s ease;
}

.secret-board__hint-btn--available {
  border-color: var(--color-warning-500);
  background: var(--color-warning-500);
  color: var(--color-white);
  box-shadow: 0 4px 0 color-mix(in srgb, var(--color-warning-500) 55%, var(--color-black) 20%);
  cursor: pointer;
}

.secret-board__hint-btn--available:hover {
  transform: translateY(-2px) scale(1.03);
  box-shadow: 0 6px 0 color-mix(in srgb, var(--color-warning-500) 55%, var(--color-black) 20%);
}

.secret-board__hint-btn--available:active {
  transform: translateY(1px) scale(0.98);
  box-shadow: 0 2px 0 color-mix(in srgb, var(--color-warning-500) 55%, var(--color-black) 20%);
}

.secret-board__hint-badge {
  display: flex;
  align-items: center;
  justify-content: center;
  min-width: 1.15rem;
  height: 1.15rem;
  padding: 0 0.3rem;
  border-radius: 9999px;
  background: var(--color-error-500);
  border: 2px solid var(--color-white);
  color: var(--color-white);
  font-size: 0.65rem;
  font-weight: 700;
}

/* --- Transitions --- */
.secret-word-pop-enter-active {
  transition:
    opacity 0.3s ease,
    transform 0.3s cubic-bezier(0.34, 1.56, 0.64, 1);
}

.secret-word-pop-enter-from {
  opacity: 0;
  transform: scale(0.7) translateY(-6px);
}

.secret-solve-fade-enter-active,
.secret-solve-fade-leave-active {
  transition:
    opacity 0.25s ease,
    transform 0.25s ease;
}

.secret-solve-fade-enter-from {
  opacity: 0;
  transform: translateY(-6px);
}

.secret-solve-fade-leave-to {
  opacity: 0;
  transform: scale(0.9);
}

@media (prefers-reduced-motion: reduce) {
  .secret-word-pop-enter-active,
  .secret-solve-fade-enter-active,
  .secret-solve-fade-leave-active,
  .secret-board__hint-btn--available {
    transition: none;
  }
}
</style>
