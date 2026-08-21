<template>
  <div class="secret-results">
    <div class="secret-results__banner" :class="solved ? 'secret-results__banner--solved' : 'secret-results__banner--gaveup'">
      <component :is="solved ? Trophy : Flag" :size="28" aria-hidden="true" />
      <h2 class="secret-results__banner-title">
        {{ solved ? t('secret.room.result.solvedBanner') : t('secret.room.result.gaveUpBanner') }}
      </h2>
    </div>

    <div class="secret-results__section secret-results__section--quality">
      <h3 class="secret-results__heading">{{ t('secret.room.result.qualityHeading') }}</h3>
      <p class="secret-results__quality">{{ reveal.hiddenQuality }}</p>
    </div>

    <div class="secret-results__stats">
      <div class="secret-results__stat">
        <span class="secret-results__stat-value">{{ reveal.totalAttempts }}</span>
        <span class="secret-results__stat-label">{{ t('secret.room.result.totalAttempts') }}</span>
      </div>
      <div class="secret-results__stat secret-results__stat--good">
        <span class="secret-results__stat-value">{{ reveal.acceptedAttempts }}</span>
        <span class="secret-results__stat-label">{{ t('secret.room.result.acceptedAttempts') }}</span>
      </div>
      <div class="secret-results__stat secret-results__stat--bad">
        <span class="secret-results__stat-value">{{ reveal.rejectedAttempts }}</span>
        <span class="secret-results__stat-label">{{ t('secret.room.result.rejectedAttempts') }}</span>
      </div>
      <div class="secret-results__stat">
        <span class="secret-results__stat-value">{{ reveal.hintsUsed }}</span>
        <span class="secret-results__stat-label">{{ t('secret.room.result.hintsUsed') }}</span>
      </div>
    </div>

    <div v-if="reveal.finalScore !== null" class="secret-results__section secret-results__section--score">
      <h3 class="secret-results__heading">{{ t('secret.room.result.scoreHeading') }}</h3>
      <p class="secret-results__score">{{ reveal.finalScore }} <span class="secret-results__score-unit">{{ t('common.pts') }}</span></p>
    </div>

    <div class="secret-results__section">
      <h3 class="secret-results__heading">{{ t('secret.room.result.shareHeading') }}</h3>
      <div class="secret-results__share-box">
        <p class="secret-results__share-text">{{ shareText }}</p>
        <CartoonButton size="sm" :color="copied ? THEME_COLORS.success500 : THEME_COLORS.secondary500" @click="onCopy">
          <Check v-if="copied" :size="16" aria-hidden="true" />
          <Copy v-else :size="16" aria-hidden="true" />
          {{ copied ? t('room.reveal.copied') : t('room.reveal.copyButton') }}
        </CartoonButton>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { Check, Copy, Flag, Trophy } from 'lucide-vue-next'
import CartoonButton from '@/components/CartoonButton.vue'
import { THEME_COLORS } from '@/assets/theme'
import { useSound } from '@/composables/useSound'
import { copyToClipboard } from '@/utils/clipboard'
import type { SecretRevealView } from '@/types/secretGame'

const props = defineProps<{
  reveal: SecretRevealView
  solved: boolean
}>()

const { t } = useI18n()
const { play } = useSound()

const shareText = computed(() =>
  props.solved
    ? t('secret.room.result.shareTextSolved', {
        quality: props.reveal.hiddenQuality,
        attempts: props.reveal.totalAttempts,
        hints: props.reveal.hintsUsed,
        url: window.location.origin,
      })
    : t('secret.room.result.shareTextGaveUp', {
        quality: props.reveal.hiddenQuality,
        url: window.location.origin,
      }),
)

const copied = ref(false)
let copiedTimeout: ReturnType<typeof setTimeout> | undefined

async function onCopy() {
  const ok = await copyToClipboard(shareText.value)
  if (!ok) return
  play('menuSelect')
  copied.value = true
  if (copiedTimeout) clearTimeout(copiedTimeout)
  copiedTimeout = setTimeout(() => {
    copied.value = false
  }, 2000)
}

onMounted(() => {
  setTimeout(() => play('gameResultsShown'), 300)
})
</script>

<style scoped>
.secret-results {
  display: flex;
  flex-direction: column;
  gap: 1.25rem;
  font-family: 'Outfit', sans-serif;
}

.secret-results__banner {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 0.6rem;
  padding: 1rem 1.25rem;
  border-radius: 1.25rem;
  color: var(--color-white);
  animation: secret-results-pop 0.5s cubic-bezier(0.34, 1.56, 0.64, 1);
}

.secret-results__banner--solved {
  border: 3px solid var(--color-success-700);
  background: var(--color-success-500);
  box-shadow: 0 5px 0 var(--color-success-700);
}

.secret-results__banner--gaveup {
  border: 3px solid var(--color-gray-500);
  background: var(--color-gray-400);
  box-shadow: 0 5px 0 var(--color-gray-500);
}

.secret-results__banner-title {
  font-family: 'Fredoka', 'Outfit', sans-serif;
  font-size: 1.4rem;
  font-weight: 700;
  text-shadow: 2px 2px 0 color-mix(in srgb, var(--color-black) 20%, transparent);
  text-align: center;
}

@keyframes secret-results-pop {
  0% {
    transform: scale(0.6) translateY(-12px);
    opacity: 0;
  }
  60% {
    transform: scale(1.08) translateY(0);
    opacity: 1;
  }
  100% {
    transform: scale(1) translateY(0);
  }
}

.secret-results__section {
  border-radius: 1.1rem;
  background: var(--color-gray-25, #fcfcfd);
  border: 2px solid var(--color-gray-100, #f2f4f7);
  padding: 1rem 1.1rem;
  text-align: center;
}

.secret-results__heading {
  margin-bottom: 0.6rem;
  font-family: 'Fredoka', 'Outfit', sans-serif;
  font-size: 1.05rem;
  font-weight: 700;
  color: var(--color-gray-800);
}

.secret-results__quality {
  font-family: 'Fredoka', 'Outfit', sans-serif;
  font-size: 1.3rem;
  font-weight: 700;
  color: var(--color-error-600, #d92d20);
}

.secret-results__stats {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 0.6rem;
}

@media (min-width: 480px) {
  .secret-results__stats {
    grid-template-columns: repeat(4, 1fr);
  }
}

.secret-results__stat {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 0.15rem;
  padding: 0.7rem 0.4rem;
  border-radius: 0.9rem;
  background: var(--color-white);
  border: 2px solid var(--color-gray-200);
}

.secret-results__stat--good {
  border-color: var(--color-success-300);
  background: var(--color-success-50);
}

.secret-results__stat--bad {
  border-color: var(--color-secondary-300);
  background: var(--color-secondary-50);
}

.secret-results__stat-value {
  font-family: 'Fredoka', 'Outfit', sans-serif;
  font-size: 1.3rem;
  font-weight: 700;
  color: var(--color-gray-800);
}

.secret-results__stat-label {
  font-size: 0.7rem;
  color: var(--color-gray-500);
  text-align: center;
}

.secret-results__score {
  font-family: 'Fredoka', 'Outfit', sans-serif;
  font-size: 2rem;
  font-weight: 700;
  color: var(--color-warning-500);
}

.secret-results__score-unit {
  font-size: 1rem;
  color: var(--color-gray-500);
}

.secret-results__share-box {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: center;
  gap: 0.75rem;
  border-radius: 0.9rem;
  border: 2px dashed var(--color-secondary-400);
  background: var(--color-secondary-50);
  padding: 0.75rem 0.9rem;
}

.secret-results__share-text {
  flex: 1;
  min-width: 12rem;
  white-space: pre-wrap;
  font-size: 0.85rem;
  color: var(--color-gray-800);
  text-align: left;
}

@media (prefers-reduced-motion: reduce) {
  .secret-results__banner {
    animation: none;
  }
}
</style>
