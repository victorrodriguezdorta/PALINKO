<template>
  <Teleport to="body">
    <Transition name="letter-fade">
      <div v-if="open" class="letter-backdrop" @keydown.esc="onClose">
        <Transition name="letter-pop" appear>
          <section
            ref="dialogRef"
            class="letter"
            role="dialog"
            aria-modal="true"
            aria-labelledby="creator-letter-title"
            tabindex="-1"
          >
            <button type="button" class="letter__close" :aria-label="t('creatorLetter.close')" @click="onClose">
              <X :size="22" :stroke-width="3" aria-hidden="true" />
            </button>

            <div class="letter__stamp" aria-hidden="true">💌</div>

            <template v-if="!sent">
              <h2 id="creator-letter-title" class="letter__title">
                {{ t('creatorLetter.greeting', { username: authStore.user?.username ?? '' }) }}
              </h2>
              <p class="letter__text letter__text--strong">{{ t('creatorLetter.intro') }}</p>
              <p class="letter__text">{{ t('creatorLetter.body') }}</p>
              <p class="letter__text">{{ t('creatorLetter.ask') }}</p>

              <form class="letter__form" @submit.prevent="onSubmit">
                <label class="letter__label" for="creator-letter-suggestion">
                  {{ t('creatorLetter.suggestionLabel') }}
                </label>
                <textarea
                  id="creator-letter-suggestion"
                  ref="firstFieldRef"
                  v-model="suggestion"
                  class="letter__field"
                  rows="3"
                  :maxlength="MAX_LENGTH"
                  :placeholder="t('creatorLetter.suggestionPlaceholder')"
                />

                <label class="letter__label" for="creator-letter-issues">
                  {{ t('creatorLetter.issuesLabel') }}
                </label>
                <textarea
                  id="creator-letter-issues"
                  v-model="issues"
                  class="letter__field"
                  rows="3"
                  :maxlength="MAX_LENGTH"
                  :placeholder="t('creatorLetter.issuesPlaceholder')"
                />

                <p v-if="errorMessage" class="letter__error" role="alert">{{ errorMessage }}</p>

                <div class="letter__footer">
                  <p class="letter__signature">
                    {{ t('creatorLetter.signOff') }}<br />
                    <span class="letter__signature-name">— {{ t('creatorLetter.signature') }}</span>
                  </p>
                  <CartoonButton
                    size="lg"
                    :color="THEME_COLORS.accent500"
                    :disabled="!canSubmit"
                    :loading="submitting"
                    @click="onSubmit"
                  >
                    {{ t('creatorLetter.submit') }}
                  </CartoonButton>
                </div>
              </form>
            </template>

            <div v-else class="letter__thanks">
              <h2 id="creator-letter-title" class="letter__title">{{ t('creatorLetter.thanksTitle') }}</h2>
              <p class="letter__text">{{ t('creatorLetter.thanksBody') }}</p>
              <CartoonButton size="lg" :color="THEME_COLORS.success500" @click="hide">
                {{ t('creatorLetter.thanksClose') }}
              </CartoonButton>
            </div>
          </section>
        </Transition>
      </div>
    </Transition>
  </Teleport>
</template>

<script setup lang="ts">
import { computed, nextTick, onUnmounted, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRoute } from 'vue-router'
import { X } from 'lucide-vue-next'
import CartoonButton from '@/components/CartoonButton.vue'
import { THEME_COLORS } from '@/assets/theme'
import { useAuthStore } from '@/stores/auth'
import {
  dismissCreatorLetter,
  getCreatorLetterVisibility,
  submitCreatorLetterFeedback,
} from '@/services/feedbackService'
import type { ApiError } from '@/types/game'

// Mirrors backend CreatorLetterPolicy.MIN_DAILY_GAMES_EXCLUSIVE / SubmitFeedbackRequest.MAX_LENGTH.
// The backend stays the source of truth; this only skips pointless requests.
const MIN_DAILY_GAMES_EXCLUSIVE = 5
const MAX_LENGTH = 2000

const { t } = useI18n()
const route = useRoute()
const authStore = useAuthStore()

const pending = ref(false)
const sent = ref(false)
const suggestion = ref('')
const issues = ref('')
const submitting = ref(false)
const errorMessage = ref('')
const dialogRef = ref<HTMLElement | null>(null)
const firstFieldRef = ref<HTMLTextAreaElement | null>(null)

// Never interrupt a game in progress: the letter waits until the player
// leaves the room (e.g. back to Home after finishing the daily challenge).
const open = computed(() => pending.value && !!route.name && route.name !== 'Room')
const canSubmit = computed(() => !submitting.value && (suggestion.value.trim() !== '' || issues.value.trim() !== ''))

// Re-checked whenever the account or its game count changes (RoomView
// re-fetches the profile after each daily challenge).
watch(
  () => {
    const user = authStore.user
    return user && user.dailyGamesPlayed > MIN_DAILY_GAMES_EXCLUSIVE ? `${user.id}:${user.dailyGamesPlayed}` : null
  },
  async (key) => {
    if (!key) {
      pending.value = false
      return
    }
    try {
      const visible = await getCreatorLetterVisibility()
      if (visible && !pending.value) {
        sent.value = false
        suggestion.value = ''
        issues.value = ''
        errorMessage.value = ''
      }
      pending.value = visible
    } catch {
      pending.value = false
    }
  },
  { immediate: true },
)

watch(open, async (isOpen) => {
  document.body.style.overflow = isOpen ? 'hidden' : ''
  if (isOpen) {
    await nextTick()
    ;(firstFieldRef.value ?? dialogRef.value)?.focus()
  }
})

onUnmounted(() => {
  document.body.style.overflow = ''
})

function hide() {
  pending.value = false
}

function onClose() {
  if (sent.value) {
    hide()
    return
  }
  hide()
  // Fire-and-forget: the backend remembers today's date so the letter
  // stays hidden until the next UTC day, on every device.
  dismissCreatorLetter().catch(() => {})
}

async function onSubmit() {
  if (!canSubmit.value) return
  submitting.value = true
  errorMessage.value = ''
  try {
    await submitCreatorLetterFeedback({ suggestion: suggestion.value, issues: issues.value })
    sent.value = true
  } catch (error) {
    const code = (error as ApiError).code
    if (code === 'FEEDBACK_ALREADY_SUBMITTED') {
      errorMessage.value = t('creatorLetter.alreadySent')
    } else {
      errorMessage.value = t('creatorLetter.error')
    }
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped>
.letter-backdrop {
  position: fixed;
  inset: 0;
  z-index: 100;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 1rem;
  overflow-y: auto;
  background: color-mix(in srgb, var(--color-bg-navy-950) 75%, transparent);
  backdrop-filter: blur(3px);
}

.letter {
  position: relative;
  width: 100%;
  max-width: 34rem;
  max-height: calc(100dvh - 2rem);
  overflow-y: auto;
  padding: 2rem 1.75rem 1.5rem;
  border: 3px solid var(--color-gray-900);
  border-radius: 28px 10px 24px 12px / 12px 24px 10px 28px;
  /* Lined writing paper */
  background:
    repeating-linear-gradient(
      to bottom,
      transparent 0,
      transparent 1.9rem,
      color-mix(in srgb, var(--color-secondary-400) 18%, transparent) 1.9rem,
      color-mix(in srgb, var(--color-secondary-400) 18%, transparent) calc(1.9rem + 1px)
    ),
    #fffaf0;
  box-shadow:
    0 8px 0 var(--color-accent-700),
    0 24px 48px -12px color-mix(in srgb, var(--color-black) 60%, transparent);
  transform: rotate(-1deg);
  font-family: 'Outfit', sans-serif;
  color: var(--color-gray-800);
}

.letter:focus {
  outline: none;
}

.letter__close {
  position: absolute;
  top: 0.75rem;
  right: 0.75rem;
  display: grid;
  place-items: center;
  width: 2.5rem;
  height: 2.5rem;
  border: 3px solid var(--color-gray-900);
  border-radius: 9999px;
  background: var(--color-error-500);
  color: var(--color-white);
  box-shadow: 0 3px 0 var(--color-error-700);
  cursor: pointer;
  transition: transform 0.15s cubic-bezier(0.34, 1.56, 0.64, 1);
}

.letter__close:hover {
  transform: rotate(90deg) scale(1.08);
}

.letter__close:active {
  transform: translateY(2px);
  box-shadow: 0 1px 0 var(--color-error-700);
}

.letter__close:focus-visible {
  outline: 3px solid var(--color-accent-500);
  outline-offset: 2px;
}

.letter__stamp {
  position: absolute;
  top: -1.4rem;
  left: 1.25rem;
  display: grid;
  place-items: center;
  width: 3.25rem;
  height: 3.25rem;
  border: 3px solid var(--color-gray-900);
  border-radius: 0.75rem;
  background: var(--color-accent-200);
  font-size: 1.75rem;
  transform: rotate(-10deg);
  box-shadow: 0 4px 0 var(--color-accent-700);
}

.letter__title {
  margin: 0.25rem 2.5rem 0.75rem 0;
  font-family: 'Fredoka', 'Outfit', sans-serif;
  font-size: 1.75rem;
  font-weight: 700;
  line-height: 1.15;
  color: var(--color-gray-900);
  text-shadow: 2px 2px 0 color-mix(in srgb, var(--color-accent-500) 35%, transparent);
}

.letter__text {
  margin-bottom: 0.6rem;
  font-size: 1rem;
  line-height: 1.5;
}

.letter__text--strong {
  font-weight: 600;
}

.letter__form {
  display: flex;
  flex-direction: column;
  gap: 0.4rem;
  margin-top: 0.5rem;
}

.letter__label {
  margin-top: 0.4rem;
  font-family: 'Fredoka', 'Outfit', sans-serif;
  font-size: 1.05rem;
  font-weight: 600;
  color: var(--color-gray-900);
}

.letter__field {
  width: 100%;
  resize: vertical;
  min-height: 4.5rem;
  padding: 0.6rem 0.8rem;
  border: 3px solid var(--color-gray-900);
  border-radius: 14px 6px 14px 6px;
  background: var(--color-white);
  font: inherit;
  color: var(--color-gray-900);
  box-shadow: 0 3px 0 var(--color-gray-300);
  transition:
    border-color 0.15s ease,
    box-shadow 0.15s ease;
}

.letter__field:focus {
  outline: none;
  border-color: var(--color-accent-500);
  box-shadow: 0 3px 0 var(--color-accent-700);
}

.letter__error {
  margin-top: 0.4rem;
  padding: 0.5rem 0.75rem;
  border-radius: 0.5rem;
  background: var(--color-error-100);
  color: var(--color-error-700);
  font-size: 0.9rem;
}

.letter__footer {
  display: flex;
  flex-wrap: wrap;
  align-items: flex-end;
  justify-content: space-between;
  gap: 1rem;
  margin-top: 1rem;
}

.letter__signature {
  font-size: 0.95rem;
  line-height: 1.4;
}

.letter__signature-name {
  font-family: 'Fredoka', 'Outfit', sans-serif;
  font-size: 1.35rem;
  font-weight: 700;
  color: var(--color-accent-700);
  display: inline-block;
  transform: rotate(-4deg);
}

.letter__thanks {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 0.75rem;
  padding: 1rem 0 0.5rem;
  text-align: center;
}

.letter__thanks .letter__title {
  margin-right: 0;
}

.letter-fade-enter-active,
.letter-fade-leave-active {
  transition: opacity 0.25s ease;
}

.letter-fade-enter-from,
.letter-fade-leave-to {
  opacity: 0;
}

.letter-pop-enter-active {
  transition:
    transform 0.4s cubic-bezier(0.34, 1.56, 0.64, 1),
    opacity 0.25s ease;
}

.letter-pop-enter-from {
  opacity: 0;
  transform: translateY(40px) rotate(4deg) scale(0.85);
}

@media (prefers-reduced-motion: reduce) {
  .letter-fade-enter-active,
  .letter-fade-leave-active,
  .letter-pop-enter-active,
  .letter__close {
    transition: none;
  }
}
</style>
