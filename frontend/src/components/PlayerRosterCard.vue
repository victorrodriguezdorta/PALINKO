<template>
  <CartoonCard :accent="THEME_COLORS.secondary500">
    <template #title>
      {{ t('room.players.heading') }}
      <span class="player-roster__capacity">
        {{ t('room.players.capacity', { count: players.length, max: maxPlayers }) }}
      </span>
    </template>

    <div class="player-roster__list">
      <PlayerCard
        v-for="player in players"
        :key="player.id"
        :player="player"
        :is-self="player.id === viewerPlayerId"
        :can-kick="isHost && player.id !== viewerPlayerId && player.id !== hostPlayerId"
        @kick="$emit('kick', player.id)"
      />
    </div>
  </CartoonCard>
</template>

<script setup lang="ts">
import { useI18n } from 'vue-i18n'
import CartoonCard from '@/components/CartoonCard.vue'
import PlayerCard from '@/components/PlayerCard.vue'
import { THEME_COLORS } from '@/assets/theme'
import type { PlayerView } from '@/types/game'

defineProps<{
  players: PlayerView[]
  hostPlayerId: string
  viewerPlayerId: string
  isHost: boolean
  maxPlayers: number
}>()

defineEmits<{
  kick: [playerId: string]
}>()

const { t } = useI18n()
</script>

<style scoped>
.player-roster__capacity {
  display: block;
  font-family: 'Outfit', sans-serif;
  font-size: 0.8rem;
  font-weight: 500;
  color: var(--color-gray-500);
  text-shadow: none;
}

.player-roster__list {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}
</style>
