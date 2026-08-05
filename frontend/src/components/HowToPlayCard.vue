<template>
  <section class="how-to-play" :style="waveStyle">
    <div class="how-to-play__inner">
      <h2 class="how-to-play__title">{{ t('home.howToPlay.heading') }}</h2>
      <p class="how-to-play__hint">{{ t('home.howToPlay.hint') }}</p>

      <div class="how-to-play__chain">
        <template v-for="(step, index) in steps" :key="index">
          <div class="how-to-play__step" :style="{ '--how-to-play-delay': `${index * 0.4}s` }">
            <span class="how-to-play__emoji" aria-hidden="true">{{ step.emoji }}</span>
            <span class="how-to-play__word">{{ step.word }}</span>
          </div>
          <div
            v-if="index < steps.length - 1"
            class="how-to-play__arrow"
            :style="{ '--how-to-play-delay': `${index * 0.4 + 0.2}s` }"
            aria-hidden="true"
          >
            <ArrowDown class="how-to-play__arrow-icon how-to-play__arrow-icon--down" :size="22" />
            <ArrowRight class="how-to-play__arrow-icon how-to-play__arrow-icon--right" :size="22" />
          </div>
        </template>
      </div>

      <div class="how-to-play__ai">
        <h3 class="how-to-play__ai-title">{{ t('home.aiValidation.heading') }}</h3>
        <p class="how-to-play__ai-hint">{{ t('home.aiValidation.hint') }}</p>

        <div class="how-to-play__ai-stage">
          <TransitionGroup name="how-to-play-ai-fade">
            <div class="how-to-play__ai-row" :key="pair.left + pair.right">
              <span class="how-to-play__ai-col how-to-play__ai-col--left">
                <span class="how-to-play__ai-word">{{ pair.left }}</span>
              </span>

              <span class="how-to-play__ai-col how-to-play__ai-col--center">
                <span class="how-to-play__ai-brain" aria-hidden="true">
                  <img class="how-to-play__ai-brain-icon" :src="aiIcon" alt="" />
                </span>
                <span
                  class="how-to-play__ai-badge"
                  :class="pair.related ? 'how-to-play__ai-badge--yes' : 'how-to-play__ai-badge--no'"
                >
                  <Check v-if="pair.related" :size="16" />
                  <X v-else :size="16" />
                  {{
                    pair.related
                      ? t('home.aiValidation.correctLabel')
                      : t('home.aiValidation.incorrectLabel')
                  }}
                </span>
              </span>

              <span class="how-to-play__ai-col how-to-play__ai-col--right">
                <span class="how-to-play__ai-word">{{ pair.right }}</span>
              </span>
            </div>
          </TransitionGroup>
        </div>
      </div>
    </div>
  </section>
</template>

<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { ArrowDown, ArrowRight, Check, X } from 'lucide-vue-next'
import { THEME_COLORS } from '@/assets/theme'
import aiIcon from '@/assets/images/ai.svg'

interface WordPair {
  left: string
  right: string
  related: boolean
}

const { t, tm } = useI18n()

const steps = tm('home.howToPlay.steps') as { emoji: string; word: string }[]

const rawPairs = computed(() => tm('home.aiValidation.pairs') as WordPair[])

// Alternate related/unrelated pairs regardless of their order in the locale file,
// so the demo always shows one, then the other, then one, etc.
const pairs = computed(() => {
  const related = rawPairs.value.filter((p) => p.related)
  const unrelated = rawPairs.value.filter((p) => !p.related)
  const alternated: WordPair[] = []
  const max = Math.max(related.length, unrelated.length)
  for (let i = 0; i < max; i += 1) {
    if (i < related.length) alternated.push(related[i])
    if (i < unrelated.length) alternated.push(unrelated[i])
  }
  return alternated
})

const pairIndex = ref(0)
const pair = computed(() => pairs.value[pairIndex.value % pairs.value.length])

let pairTimer: ReturnType<typeof setInterval> | undefined

onMounted(() => {
  pairTimer = setInterval(() => {
    pairIndex.value += 1
  }, 2600)
})

onUnmounted(() => {
  if (pairTimer) clearInterval(pairTimer)
})

const waveStyle = {
  '--how-to-play-wave-bottom': `url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 200 20' preserveAspectRatio='none'%3E%3Cpath d='M0 10 C 25 20, 75 0, 100 10 C 125 20, 175 0, 200 10 L200 0 L0 0 Z' fill='%23${THEME_COLORS.secondary700.slice(1)}'/%3E%3C/svg%3E")`,
  '--how-to-play-wave-top': `url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 200 20' preserveAspectRatio='none'%3E%3Cpath d='M0 10 C 25 0, 75 20, 100 10 C 125 0, 175 20, 200 10 L200 20 L0 20 Z' fill='%23${THEME_COLORS.secondary700.slice(1)}'/%3E%3C/svg%3E")`,
}
</script>

<style scoped>
.how-to-play {
  position: relative;
  left: 50%;
  width: 100vw;
  margin-left: -50vw;
  box-sizing: border-box;
  margin-top: 4.5rem;
  margin-bottom: 1.1rem;
  padding: 2.5rem 1.25rem 3rem;
  background: var(--color-secondary-700);
  overflow: visible;
}

.how-to-play::before,
.how-to-play::after {
  content: '';
  position: absolute;
  left: 0;
  right: 0;
  height: 1.2rem;
  background-repeat: repeat-x;
  background-size: 200px 100%;
  animation: how-to-play-wave 8s linear infinite;
  pointer-events: none;
}

.how-to-play::before {
  top: -1.1rem;
  background-image: var(--how-to-play-wave-top);
}

.how-to-play::after {
  bottom: -1.1rem;
  background-image: var(--how-to-play-wave-bottom);
}

@keyframes how-to-play-wave {
  from {
    background-position-x: 0;
  }
  to {
    background-position-x: -200px;
  }
}

@media (prefers-reduced-motion: reduce) {
  .how-to-play::before,
  .how-to-play::after {
    animation: none;
  }
}

.how-to-play__inner {
  max-width: 48rem;
  margin: 0 auto;
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
}

.how-to-play__title {
  font-family: 'Fredoka', 'Outfit', sans-serif;
  font-size: 2rem;
  font-weight: 700;
  letter-spacing: 0.01em;
  color: var(--color-white);
  text-shadow: 3px 3px 0 color-mix(in srgb, var(--color-secondary-900, #0b1f4d) 55%, transparent);
}

.how-to-play__hint {
  margin-top: 0.5rem;
  margin-bottom: 2rem;
  font-family: 'Outfit', sans-serif;
  font-size: 0.9rem;
  color: color-mix(in srgb, var(--color-white) 80%, transparent);
}

.how-to-play__chain {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 0;
}

.how-to-play__step {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  padding: 0.65rem 1.5rem;
  border-radius: 255px 15px 225px 15px / 15px 225px 15px 255px;
  background: var(--color-white);
  border: 3px solid var(--color-accent-500);
  box-shadow: 0 5px 0 color-mix(in srgb, var(--color-accent-500) 55%, var(--color-black) 20%);
  min-width: 11rem;
  justify-content: center;
  opacity: 0;
  animation: how-to-play-pop 5s cubic-bezier(0.34, 1.56, 0.64, 1) infinite;
  animation-delay: var(--how-to-play-delay, 0s);
}

.how-to-play__emoji {
  font-size: 1.6rem;
  line-height: 1;
}

.how-to-play__word {
  font-family: 'Fredoka', 'Outfit', sans-serif;
  font-weight: 700;
  font-size: 1.05rem;
  color: var(--color-gray-800);
}

.how-to-play__arrow {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 2.2rem;
  color: color-mix(in srgb, var(--color-white) 85%, transparent);
  opacity: 0;
  animation: how-to-play-arrow-pop 5s ease infinite;
  animation-delay: var(--how-to-play-delay, 0s);
}

.how-to-play__arrow-icon--right {
  display: none;
}

/* Runs once per 5s cycle, starting after this element's own animation-delay:
   pop in (grow slightly past full size then settle), stay visible, then
   jump back to hidden right before the cycle repeats so every step pops
   in again from scratch instead of shrinking away. */
@keyframes how-to-play-pop {
  0% {
    opacity: 0;
    transform: scale(0.6);
  }
  10% {
    opacity: 1;
    transform: scale(1.12);
  }
  16%,
  99% {
    opacity: 1;
    transform: scale(1);
  }
  100% {
    opacity: 0;
    transform: scale(0.6);
  }
}

@keyframes how-to-play-arrow-pop {
  0% {
    opacity: 0;
  }
  10%,
  99% {
    opacity: 1;
  }
  100% {
    opacity: 0;
  }
}

@media (min-width: 640px) {
  .how-to-play__chain {
    flex-direction: row;
    flex-wrap: wrap;
    justify-content: center;
    gap: 0.25rem;
  }

  .how-to-play__arrow {
    height: auto;
    width: 2.2rem;
  }

  .how-to-play__arrow-icon--down {
    display: none;
  }

  .how-to-play__arrow-icon--right {
    display: block;
  }
}

@media (prefers-reduced-motion: reduce) {
  .how-to-play__step,
  .how-to-play__arrow {
    animation: none;
    opacity: 1;
  }
}

.how-to-play__ai {
  margin-top: 2.5rem;
  padding-top: 2rem;
  border-top: 2px dashed color-mix(in srgb, var(--color-white) 30%, transparent);
  width: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
}

.how-to-play__ai-title {
  font-family: 'Fredoka', 'Outfit', sans-serif;
  font-size: 1.4rem;
  font-weight: 700;
  letter-spacing: 0.01em;
  color: var(--color-white);
  text-shadow: 2px 2px 0 color-mix(in srgb, var(--color-secondary-900, #0b1f4d) 55%, transparent);
}

.how-to-play__ai-hint {
  margin-top: 0.4rem;
  margin-bottom: 1.75rem;
  max-width: 34rem;
  font-family: 'Outfit', sans-serif;
  font-size: 0.85rem;
  color: color-mix(in srgb, var(--color-white) 80%, transparent);
}

.how-to-play__ai-stage {
  position: relative;
  width: 100%;
  min-height: 16rem;
  display: flex;
  align-items: center;
  justify-content: center;
}

@media (min-width: 640px) {
  .how-to-play__ai-stage {
    min-height: 9.5rem;
  }
}

.how-to-play__ai-row {
  position: absolute;
  inset: 0;
  display: grid;
  grid-template-columns: 1fr auto 1fr;
  align-items: center;
  justify-items: center;
  column-gap: 1rem;
  row-gap: 0.5rem;
}

.how-to-play__ai-col {
  display: flex;
  align-items: center;
  justify-content: center;
  min-width: 0;
}

.how-to-play__ai-col--left {
  justify-self: end;
}

.how-to-play__ai-col--right {
  justify-self: start;
}

.how-to-play__ai-col--center {
  flex-direction: column;
  gap: 0.5rem;
}

.how-to-play__ai-word {
  font-family: 'Fredoka', 'Outfit', sans-serif;
  font-weight: 700;
  font-size: 1.2rem;
  color: var(--color-gray-800);
  background: var(--color-white);
  border-radius: 255px 15px 225px 15px / 15px 225px 15px 255px;
  border: 3px solid var(--color-accent-500);
  padding: 0.6rem 1.35rem;
  min-width: 7.5rem;
  text-align: center;
  box-shadow: 0 5px 0 color-mix(in srgb, var(--color-accent-500) 55%, var(--color-black) 20%);
}

.how-to-play__ai-brain {
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.how-to-play__ai-brain-icon {
  width: 5rem;
  height: 5rem;
  filter: drop-shadow(0 6px 10px color-mix(in srgb, var(--color-black) 35%, transparent));
}

.how-to-play__ai-badge {
  display: inline-flex;
  align-items: center;
  gap: 0.3rem;
  font-family: 'Outfit', sans-serif;
  font-weight: 700;
  font-size: 0.8rem;
  padding: 0.25rem 0.7rem;
  border-radius: 999px;
  white-space: nowrap;
}

.how-to-play__ai-badge--yes {
  background: color-mix(in srgb, var(--color-success-500, #12b76a) 20%, var(--color-white));
  color: var(--color-success-500, #12b76a);
  border: 2px solid var(--color-success-500, #12b76a);
}

.how-to-play__ai-badge--no {
  background: color-mix(in srgb, var(--color-error-500, #f04438) 20%, var(--color-white));
  color: var(--color-error-500, #f04438);
  border: 2px solid var(--color-error-500, #f04438);
}

.how-to-play-ai-fade-enter-active,
.how-to-play-ai-fade-leave-active {
  transition:
    opacity 0.5s ease,
    transform 0.5s ease;
}

.how-to-play-ai-fade-enter-from {
  opacity: 0;
  transform: translateY(10px) scale(0.96);
}

.how-to-play-ai-fade-leave-to {
  opacity: 0;
  transform: translateY(-10px) scale(0.96);
}

@media (max-width: 639px) {
  .how-to-play__ai-row {
    grid-template-columns: 1fr;
    grid-template-areas:
      'left'
      'center'
      'right';
    row-gap: 0.75rem;
  }

  .how-to-play__ai-col--left {
    grid-area: left;
    justify-self: center;
  }

  .how-to-play__ai-col--center {
    grid-area: center;
    justify-self: center;
  }

  .how-to-play__ai-col--right {
    grid-area: right;
    justify-self: center;
  }
}

@media (min-width: 640px) {
  .how-to-play__ai-word {
    font-size: 1.35rem;
  }

  .how-to-play__ai-brain-icon {
    width: 6rem;
    height: 6rem;
  }
}

@media (prefers-reduced-motion: reduce) {
  .how-to-play-ai-fade-enter-active,
  .how-to-play-ai-fade-leave-active {
    transition: none;
  }
}
</style>
