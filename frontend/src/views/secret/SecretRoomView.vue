<template>
  <div class="mx-auto max-w-4xl p-6">
    <AppHeader
      :title="t('secret.room.pageTitle')"
      show-back
      center-logo
      :back-label="t('room.backToHome')"
      @back="onLeave"
    />

    <p v-if="store.errorMessage" class="mb-4 flex items-center gap-2 rounded bg-error-100 p-2 text-sm text-error-700">
      <span>{{ translateError(store.errorMessage) }}</span>
      <CartoonButton size="sm" :color="THEME_COLORS.error500" @click="store.dismissError">{{ t('common.close') }}</CartoonButton>
    </p>

    <div v-if="!snapshot" class="text-center text-gray-500">{{ t('room.connecting') }}</div>

    <template v-else>
      <div class="secret-room-topbar">
        <span class="secret-room-topbar__badge">
          <Sparkles :size="14" aria-hidden="true" />
          {{ t('secret.room.dailyBadge') }}
        </span>
        <button type="button" class="secret-room-topbar__giveup" :disabled="isFinished" @click="onGiveUp">
          <Flag :size="14" aria-hidden="true" />
          {{ t('secret.room.giveUp') }}
        </button>
      </div>

      <div class="relative rounded-2xl border-2 border-white/20 bg-white p-4 shadow-lg">
        <Transition name="secret-panel-swap" mode="out-in">
          <SecretBoardCard
            v-if="!isFinished"
            key="board"
            :accepted-attempts="acceptedAttempts"
            :rejected-attempts="rejectedAttempts"
            :solution-attempts="snapshot.solutionAttempts"
            :total-failures="snapshot.totalFailures"
            :failures-per-hint="snapshot.failuresPerHint"
            :hint-available-count="hintAvailableCount"
            v-model:word-draft="wordDraft"
            v-model:solution-draft="solutionDraft"
            @submit-word="onSubmitWord"
            @submit-solution="onSubmitSolution"
            @use-hint="onUseHint"
          />
          <SecretResultsCard
            v-else-if="snapshot.reveal"
            key="results"
            :reveal="snapshot.reveal"
            :solved="snapshot.status === 'SOLVED'"
          />
        </Transition>

        <div v-if="isFinished" class="mt-4 flex justify-center">
          <CartoonButton :color="THEME_COLORS.secondary500" @click="onLeave">
            {{ t('room.backToHome') }}
          </CartoonButton>
        </div>
      </div>
    </template>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { Flag, Sparkles } from 'lucide-vue-next'
import { useSecretGameStore } from '@/stores/secretGame'
import { translateError } from '@/i18n'
import AppHeader from '@/components/AppHeader.vue'
import CartoonButton from '@/components/CartoonButton.vue'
import SecretBoardCard from '@/components/SecretBoardCard.vue'
import SecretResultsCard from '@/components/SecretResultsCard.vue'
import { THEME_COLORS } from '@/assets/theme'
import { useSound } from '@/composables/useSound'

const { t } = useI18n()
const router = useRouter()
const store = useSecretGameStore()
const { play } = useSound()

const wordDraft = ref('')
const solutionDraft = ref('')

const snapshot = computed(() => store.snapshot)
const isFinished = computed(() => snapshot.value?.status === 'SOLVED' || snapshot.value?.status === 'GAVE_UP')

const acceptedAttempts = computed(() => snapshot.value?.attempts.filter((a) => a.outcome === 'ACCEPTED') ?? [])
const rejectedAttempts = computed(() => snapshot.value?.attempts.filter((a) => a.outcome === 'REJECTED') ?? [])

const hintAvailableCount = computed(() => {
  if (!snapshot.value) return 0
  return Math.max(0, snapshot.value.hintsAvailable - snapshot.value.hintsUsed)
})

function onSubmitWord() {
  const text = wordDraft.value.trim()
  if (!text) return
  play('wordSubmit')
  store.submitWord(text)
  wordDraft.value = ''
}

function onUseHint() {
  play('menuSelect')
  store.useHint()
}

function onSubmitSolution() {
  const text = solutionDraft.value.trim()
  if (!text) return
  play('wordSubmit')
  store.submitSolution(text)
  solutionDraft.value = ''
}

function onGiveUp() {
  if (confirm(t('secret.room.giveUpConfirm'))) {
    store.giveUp()
  }
}

function onLeave() {
  store.leaveRoom()
  router.push('/secret')
}

// Every new word attempt (accepted/rejected) gets its own cue, and every
// new solution guess gets a cue scaled to how close it was — mirrors
// RoomView's own "watch the shared snapshot, not local actions" pattern so
// a reconnecting tab still hears the right sound for state it just caught
// up on.
const seenWordAttemptIds = new Set<string>()
watch(
  () => snapshot.value?.attempts,
  (attempts) => {
    if (!attempts) return
    for (const attempt of attempts) {
      if (seenWordAttemptIds.has(attempt.id)) continue
      seenWordAttemptIds.add(attempt.id)
      play(attempt.outcome === 'ACCEPTED' ? 'wordAccepted' : 'wordRejected')
    }
  },
  { deep: true, immediate: true },
)

const seenSolutionAttemptIds = new Set<string>()
watch(
  () => snapshot.value?.solutionAttempts,
  (attempts) => {
    if (!attempts) return
    for (const attempt of attempts) {
      if (seenSolutionAttemptIds.has(attempt.id)) continue
      seenSolutionAttemptIds.add(attempt.id)
      if (attempt.matchLevel === 'CORRECT') play('targetReached')
      else if (attempt.matchLevel === 'PARTIAL') play('menuSelect')
      else play('wordRejected')
    }
  },
  { deep: true, immediate: true },
)

watch(
  () => snapshot.value?.status,
  (status, previousStatus) => {
    if (previousStatus === undefined || status === previousStatus) return
    if (status === 'SOLVED') play('crewWon')
    else if (status === 'GAVE_UP') play('infiltratorEscaped')
  },
)

watch(
  () => store.errorMessage,
  (error) => {
    if (error) play('gameError')
  },
)

onMounted(() => {
  if (store.roomCode) {
    store.connectSocket()
  } else {
    router.push('/secret')
  }
})

onUnmounted(() => {
  // Only tear down the socket, never clear the stored identity here: a
  // route change/reload should still be able to reconnect to the same
  // room, exactly like the original mode's RoomView.
})
</script>

<style scoped>
.secret-room-topbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 0.75rem;
  margin-bottom: 1rem;
}

.secret-room-topbar__badge {
  display: inline-flex;
  align-items: center;
  gap: 0.35rem;
  padding: 0.3rem 0.85rem;
  border-radius: 9999px;
  background: var(--color-warning-100, #fef0c7);
  border: 2px solid var(--color-warning-500);
  color: var(--color-warning-700, #b54708);
  font-family: 'Outfit', sans-serif;
  font-weight: 700;
  font-size: 0.7rem;
  letter-spacing: 0.03em;
  text-transform: uppercase;
}

.secret-room-topbar__giveup {
  display: inline-flex;
  align-items: center;
  gap: 0.35rem;
  padding: 0.3rem 0.75rem;
  border-radius: 9999px;
  border: 2px solid var(--color-error-300);
  background: var(--color-white);
  color: var(--color-error-600, #d92d20);
  font-family: 'Outfit', sans-serif;
  font-weight: 600;
  font-size: 0.75rem;
  cursor: pointer;
  transition:
    transform 0.15s ease,
    background 0.15s ease;
}

.secret-room-topbar__giveup:hover:not(:disabled) {
  background: var(--color-error-50);
  transform: translateY(-1px);
}

.secret-room-topbar__giveup:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}

.secret-panel-swap-enter-active,
.secret-panel-swap-leave-active {
  transition:
    opacity 0.25s ease,
    transform 0.25s ease;
}

.secret-panel-swap-enter-from {
  opacity: 0;
  transform: scale(0.98);
}

.secret-panel-swap-leave-to {
  opacity: 0;
  transform: scale(1.01);
}

@media (prefers-reduced-motion: reduce) {
  .secret-panel-swap-enter-active,
  .secret-panel-swap-leave-active,
  .secret-room-topbar__giveup {
    transition: none;
  }
}
</style>
