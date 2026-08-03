<template>
  <div class="results-card">
    <div class="results-card__banner">
      <h2 class="results-card__banner-title">{{ t('room.reveal.completedBanner') }}</h2>
    </div>

    <div v-if="chain.reveal.acceptedWordChain.length > 0" class="results-card__section">
      <h3 class="results-card__heading">{{ t('room.reveal.wordChainHeading') }}</h3>
      <div class="results-card__chain">
        <template v-for="(word, index) in fullWordSequence" :key="index">
          <span class="results-card__word" :class="isMilestone(index) ? 'results-card__word--milestone' : 'results-card__word--regular'">
            {{ word }}
          </span>
          <ArrowRight v-if="index < fullWordSequence.length - 1" :size="18" class="results-card__arrow" aria-hidden="true" />
        </template>
      </div>

      <p v-if="averageAccuracyPercent !== null" class="results-card__accuracy-label">
        {{ t('room.reveal.averageAccuracy', { value: averageAccuracyPercent }) }}
      </p>
    </div>

    <div class="results-card__section">
      <h3 class="results-card__heading">{{ t('room.reveal.shareHeading') }}</h3>
      <p class="results-card__share-hint">{{ t('room.reveal.shareHint') }}</p>
      <div class="results-card__share-box">
        <p class="results-card__share-text">{{ shareText }}</p>
        <CartoonButton size="sm" :color="copied ? THEME_COLORS.success500 : THEME_COLORS.secondary500" @click="onCopy">
          <Check v-if="copied" :size="16" aria-hidden="true" />
          <Copy v-else :size="16" aria-hidden="true" />
          {{ copied ? t('room.reveal.copied') : t('room.reveal.copyButton') }}
        </CartoonButton>
      </div>
    </div>

    <div class="results-card__section">
      <h3 class="results-card__heading">{{ t('room.reveal.finalScoreHeading') }}</h3>
      <ol class="results-card__scores">
        <li
          v-for="(player, index) in sortedByScore"
          :key="player.id"
          class="results-card__score-row"
          :class="`results-card__score-row--rank-${index + 1}`"
        >
          <span class="results-card__medal">
            <component :is="medalIcon(index)" v-if="medalIcon(index)" :size="22" aria-hidden="true" />
            <span v-else class="results-card__rank-number">{{ index + 1 }}</span>
          </span>
          <span class="results-card__player-name">{{ player.name }}</span>
          <span class="results-card__player-score">{{ player.score }} {{ t('common.pts') }}</span>
        </li>
      </ol>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { ArrowRight, Check, Copy, Medal } from 'lucide-vue-next'
import CartoonButton from '@/components/CartoonButton.vue'
import { THEME_COLORS } from '@/assets/theme'
import { useSound } from '@/composables/useSound'
import { copyToClipboard } from '@/utils/clipboard'
import type { ChainView, PlayerView } from '@/types/game'

const props = defineProps<{
  chain: ChainView & { reveal: NonNullable<ChainView['reveal']> }
  sortedByScore: PlayerView[]
  averageAccuracyPercent: number | null
}>()

const { t } = useI18n()
const { play } = useSound()

// The full walked path, origin word first: gameStartWord is the very first
// step and is never itself part of acceptedWordChain (see RevealView.from
// on the backend), so it's prepended here to show the true start -> ... ->
// end sequence.
const fullWordSequence = computed(() => [props.chain.reveal.gameStartWord, ...props.chain.reveal.acceptedWordChain])

// "Milestones" = the origin word plus every phase's own target word (the
// word that actually reached that phase's goal) — the boundary words a
// player would call "the words we had to connect", as opposed to the
// filler words attempted in between. acceptedWordCountByPhase[i] is how
// many accepted words it took to finish phase i+1, so the cumulative sum
// lands exactly on that phase's final (target-reaching) word.
const milestoneIndices = computed(() => {
  const indices = new Set<number>([0])
  let cumulative = 0
  for (const count of props.chain.reveal.acceptedWordCountByPhase) {
    cumulative += count
    if (cumulative > 0) indices.add(cumulative)
  }
  return indices
})

function isMilestone(index: number): boolean {
  return milestoneIndices.value.has(index)
}

const milestoneWords = computed(() =>
  fullWordSequence.value.filter((_, index) => milestoneIndices.value.has(index)),
)

const shareText = computed(() => {
  const words = milestoneWords.value.join(' → ')
  const wordCount = fullWordSequence.value.length
  const accuracy = props.averageAccuracyPercent ?? 0
  return t('room.reveal.shareText', {
    words,
    wordCount,
    accuracy,
    url: window.location.origin,
  })
})

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

function medalIcon(index: number) {
  return index < 3 ? Medal : null
}

onMounted(() => {
  setTimeout(() => play('gameResultsShown'), 500)
})
</script>

<style scoped>
.results-card {
  display: flex;
  flex-direction: column;
  gap: 1.25rem;
  font-family: 'Outfit', sans-serif;
}

.results-card__banner {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 1rem 1.25rem;
  border-radius: 1.25rem;
  border: 3px solid var(--color-success-700);
  background: var(--color-success-500);
  box-shadow: 0 5px 0 var(--color-success-700);
  animation: results-banner-pop 0.5s cubic-bezier(0.34, 1.56, 0.64, 1);
}

.results-card__banner-title {
  font-family: 'Fredoka', 'Outfit', sans-serif;
  font-size: 1.6rem;
  font-weight: 700;
  color: var(--color-white);
  text-shadow: 2px 2px 0 color-mix(in srgb, var(--color-black) 20%, transparent);
  text-align: center;
}

@keyframes results-banner-pop {
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

.results-card__section {
  border-radius: 1.1rem;
  background: var(--color-gray-25, #fcfcfd);
  border: 2px solid var(--color-gray-100, #f2f4f7);
  padding: 1rem 1.1rem;
}

.results-card__heading {
  margin-bottom: 0.75rem;
  font-family: 'Fredoka', 'Outfit', sans-serif;
  font-size: 1.05rem;
  font-weight: 700;
  color: var(--color-gray-800);
}

.results-card__chain {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 0.4rem;
  margin-bottom: 0.75rem;
}

.results-card__word {
  display: inline-flex;
  align-items: center;
  border-radius: 9999px;
  padding: 0.3rem 0.85rem;
  font-weight: 600;
  font-size: 0.9rem;
  border: 2px solid transparent;
}

.results-card__word--regular {
  background: var(--color-success-50);
  border-color: var(--color-success-500);
  color: var(--color-success-700);
}

.results-card__word--milestone {
  background: var(--color-accent-100);
  border-color: var(--color-accent-500);
  color: var(--color-accent-700);
  font-weight: 700;
}

.results-card__arrow {
  flex-shrink: 0;
  color: var(--color-gray-400);
}

.results-card__accuracy-label {
  font-size: 0.85rem;
  font-weight: 600;
  color: var(--color-gray-600);
}

.results-card__share-hint {
  margin-bottom: 0.6rem;
  font-size: 0.85rem;
  color: var(--color-gray-500);
}

.results-card__share-box {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 0.75rem;
  border-radius: 0.9rem;
  border: 2px dashed var(--color-secondary-400);
  background: var(--color-secondary-50);
  padding: 0.75rem 0.9rem;
}

.results-card__share-text {
  flex: 1;
  min-width: 12rem;
  white-space: pre-wrap;
  font-size: 0.85rem;
  color: var(--color-gray-800);
}

.results-card__scores {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.results-card__score-row {
  display: flex;
  align-items: center;
  gap: 0.7rem;
  padding: 0.5rem 0.75rem;
  border-radius: 0.9rem;
  background: var(--color-white);
  border: 2px solid var(--color-gray-100, #f2f4f7);
}

.results-card__score-row--rank-1 {
  border-color: #ffd54f;
  background: color-mix(in srgb, #ffd54f 15%, var(--color-white));
}

.results-card__score-row--rank-2 {
  border-color: #cfd8dc;
  background: color-mix(in srgb, #cfd8dc 20%, var(--color-white));
}

.results-card__score-row--rank-3 {
  border-color: #d7a86e;
  background: color-mix(in srgb, #d7a86e 15%, var(--color-white));
}

.results-card__medal {
  display: flex;
  flex-shrink: 0;
  align-items: center;
  justify-content: center;
  width: 1.75rem;
}

.results-card__score-row--rank-1 .results-card__medal {
  color: #f0a500;
}

.results-card__score-row--rank-2 .results-card__medal {
  color: #90a4ae;
}

.results-card__score-row--rank-3 .results-card__medal {
  color: #a1662f;
}

.results-card__rank-number {
  font-family: 'Fredoka', 'Outfit', sans-serif;
  font-weight: 700;
  color: var(--color-gray-400);
}

.results-card__player-name {
  flex: 1;
  min-width: 0;
  font-weight: 600;
  color: var(--color-gray-800);
  overflow-wrap: anywhere;
}

.results-card__player-score {
  flex-shrink: 0;
  font-family: monospace;
  font-size: 0.9rem;
  color: var(--color-gray-600);
}

@media (prefers-reduced-motion: reduce) {
  .results-card__banner {
    animation: none;
  }
}
</style>
