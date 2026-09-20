<template>
  <div class="leaderboard-card">
    <span class="leaderboard-card__badge">{{ t('leaderboard.heading') }}</span>

    <p v-if="loading" class="leaderboard-card__status">{{ t('home.daily.loading') }}</p>
    <p v-else-if="entries.length === 0" class="leaderboard-card__status">{{ t('leaderboard.empty') }}</p>

    <ol v-else class="leaderboard-card__rows">
      <li
        v-for="entry in entries"
        :key="entry.userId"
        class="leaderboard-card__row"
        :class="[`leaderboard-card__row--rank-${entry.rank}`, { 'leaderboard-card__row--viewer': entry.username === viewerUsername }]"
      >
        <span class="leaderboard-card__rank">
          <component :is="medalIcon(entry.rank)" v-if="medalIcon(entry.rank)" :size="20" aria-hidden="true" />
          <span v-else class="leaderboard-card__rank-number">{{ entry.rank }}</span>
        </span>
        <PlayerAvatar :seed="entry.avatarSeed" size="sm" />
        <StreakFlame v-if="entry.currentStreak > 0" compact :days="entry.currentStreak" />
        <span class="leaderboard-card__name">{{ entry.username }}</span>
        <span class="leaderboard-card__stats">
          {{ entry.wordCount + 1 }}
          <span class="leaderboard-card__words-label">{{ t('leaderboard.wordsLabel') }}</span>
          <span class="leaderboard-card__separator">·</span>
          <span class="leaderboard-card__accuracy">{{ entry.accuracyPercent }}%</span>
        </span>
      </li>
    </ol>
  </div>
</template>

<script setup lang="ts">
import PlayerAvatar from '@/components/PlayerAvatar.vue'
import StreakFlame from '@/components/StreakFlame.vue'
import type { DailyLeaderboardEntry } from '@/types/leaderboard'
import { Medal } from 'lucide-vue-next'
import { useI18n } from 'vue-i18n'

defineProps<{
  entries: DailyLeaderboardEntry[]
  loading: boolean
  viewerUsername: string | null
}>()

const { t } = useI18n()

function medalIcon(rank: number) {
  return rank <= 3 ? Medal : null
}
</script>

<style scoped>
.leaderboard-card {
  position: relative;
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
  border-radius: 1.1rem;
  background: var(--color-gray-25, #fcfcfd);
  border: 2px solid var(--color-gray-100, #f2f4f7);
  padding: 1.5rem 1.1rem 1rem;
  font-family: 'Outfit', sans-serif;
}

.leaderboard-card__badge {
  position: absolute;
  top: 0;
  left: 1.1rem;
  transform: translateY(-50%);
  display: inline-flex;
  align-items: center;
  padding: 0.5rem 1.4rem;
  border-radius: 0.7rem;
  background: var(--color-streak-500);
  border: 3px solid var(--color-streak-700);
  box-shadow: 0 4px 0 color-mix(in srgb, var(--color-streak-700) 70%, var(--color-black) 15%);
  font-family: 'Fredoka', 'Outfit', sans-serif;
  font-size: 0.95rem;
  font-weight: 700;
  letter-spacing: 0.03em;
  text-transform: uppercase;
  color: var(--color-white);
  white-space: nowrap;
  z-index: 1;
}

.leaderboard-card__status {
  font-size: 0.85rem;
  color: var(--color-gray-500);
}

.leaderboard-card__rows {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.leaderboard-card__row {
  display: flex;
  align-items: center;
  gap: 0.6rem;
  padding: 0.4rem 0.6rem;
  border-radius: 0.9rem;
  background: var(--color-white);
  border: 2px solid var(--color-gray-100, #f2f4f7);
}

.leaderboard-card__row--viewer {
  border-color: var(--color-secondary-400);
  background: var(--color-secondary-50);
}

.leaderboard-card__row--rank-1 {
  border-color: #ffd54f;
  background: color-mix(in srgb, #ffd54f 15%, var(--color-white));
}

.leaderboard-card__row--rank-2 {
  border-color: #cfd8dc;
  background: color-mix(in srgb, #cfd8dc 20%, var(--color-white));
}

.leaderboard-card__row--rank-3 {
  border-color: #d7a86e;
  background: color-mix(in srgb, #d7a86e 15%, var(--color-white));
}

.leaderboard-card__rank {
  display: flex;
  flex-shrink: 0;
  align-items: center;
  justify-content: center;
  width: 1.5rem;
}

.leaderboard-card__row--rank-1 .leaderboard-card__rank {
  color: #f0a500;
}

.leaderboard-card__row--rank-2 .leaderboard-card__rank {
  color: #90a4ae;
}

.leaderboard-card__row--rank-3 .leaderboard-card__rank {
  color: #a1662f;
}

.leaderboard-card__rank-number {
  font-family: 'Fredoka', 'Outfit', sans-serif;
  font-weight: 700;
  font-size: 0.9rem;
  color: var(--color-gray-400);
}

.leaderboard-card__name {
  flex: 1;
  min-width: 0;
  font-weight: 600;
  font-size: 0.9rem;
  color: var(--color-gray-800);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.leaderboard-card__stats {
  flex-shrink: 0;
  font-family: 'Fredoka', monospace;
  font-size: 1.05rem;
  font-weight: 700;
  color: var(--color-gray-900);
}

.leaderboard-card__words-label {
  font-family: 'Outfit', sans-serif;
  font-size: 0.62rem;
  font-weight: 600;
  text-transform: lowercase;
}

.leaderboard-card__separator {
  margin: 0 0.2rem;
}

.leaderboard-card__accuracy {
  font-size: 0.9rem;
}
</style>
