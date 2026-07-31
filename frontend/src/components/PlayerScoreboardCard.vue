<template>
  <div class="scoreboard-card">
    <h2 class="scoreboard-card__heading">
      {{ t('room.players.heading') }}
      <span class="scoreboard-card__capacity">
        {{ t('room.players.capacity', { count: players.length, max: maxPlayers }) }}
      </span>
    </h2>
    <ul class="scoreboard-card__list">
      <li
        v-for="player in sortedByScore"
        :key="player.id"
        class="scoreboard-card__row"
        :class="{ 'scoreboard-card__row--self': player.id === viewerPlayerId }"
      >
        <PlayerAvatar :seed="player.avatarSeed" size="sm" />
        <span class="scoreboard-card__name">
          {{ player.name }}
          <span v-if="player.id === viewerPlayerId" class="scoreboard-card__tag scoreboard-card__tag--self">
            {{ t('room.players.you') }}
          </span>
          <span v-if="player.host" class="scoreboard-card__tag">{{ t('room.players.host') }}</span>
          <span v-if="player.kicked" class="scoreboard-card__tag scoreboard-card__tag--muted">
            {{ t('room.players.kicked') }}
          </span>
          <span v-else-if="!player.connected" class="scoreboard-card__tag scoreboard-card__tag--muted">
            {{ t('room.players.disconnected') }}
          </span>
        </span>
        <span class="scoreboard-card__score">{{ player.score }} {{ t('common.pts') }}</span>

        <div v-if="canKick(player)" class="scoreboard-card__actions">
          <template v-if="confirmingKickId === player.id">
            <button
              type="button"
              class="scoreboard-card__kick scoreboard-card__kick--confirm"
              :aria-label="t('room.players.kickConfirmYes')"
              @click="onConfirmKick(player.id)"
            >
              <Check :size="13" aria-hidden="true" />
            </button>
            <button
              type="button"
              class="scoreboard-card__kick"
              :aria-label="t('room.players.kickConfirmCancel')"
              @click="confirmingKickId = null"
            >
              <X :size="13" aria-hidden="true" />
            </button>
          </template>
          <button
            v-else
            type="button"
            class="scoreboard-card__kick"
            :aria-label="t('room.players.kickAriaLabel', { name: player.name })"
            @click="confirmingKickId = player.id"
          >
            <X :size="13" aria-hidden="true" />
          </button>
        </div>
      </li>
    </ul>
  </div>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { Check, X } from 'lucide-vue-next'
import PlayerAvatar from '@/components/PlayerAvatar.vue'
import type { PlayerView } from '@/types/game'

const props = defineProps<{
  players: PlayerView[]
  viewerPlayerId: string
  hostPlayerId: string
  isHost: boolean
  maxPlayers: number
}>()

const emit = defineEmits<{
  kick: [playerId: string]
}>()

const { t } = useI18n()

const sortedByScore = computed(() => [...props.players].sort((a, b) => b.score - a.score))

const confirmingKickId = ref<string | null>(null)

function canKick(player: PlayerView): boolean {
  return props.isHost && !player.kicked && player.id !== props.viewerPlayerId && player.id !== props.hostPlayerId
}

function onConfirmKick(playerId: string) {
  confirmingKickId.value = null
  emit('kick', playerId)
}
</script>

<style scoped>
.scoreboard-card {
  height: 100%;
  padding: 1.25rem;
  border-radius: 1.25rem;
  background: var(--color-white);
  font-family: 'Outfit', sans-serif;
}

.scoreboard-card__heading {
  margin-bottom: 0.85rem;
  font-family: 'Fredoka', 'Outfit', sans-serif;
  font-size: 1.15rem;
  font-weight: 700;
  color: var(--color-gray-800);
}

.scoreboard-card__capacity {
  display: block;
  margin-top: 0.15rem;
  font-family: 'Outfit', sans-serif;
  font-size: 0.75rem;
  font-weight: 500;
  color: var(--color-gray-500);
}

.scoreboard-card__list {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.scoreboard-card__row {
  display: flex;
  align-items: center;
  gap: 0.6rem;
  padding: 0.5rem 0.6rem;
  border-radius: 0.9rem;
  background: var(--color-gray-50, #f9fafb);
}

.scoreboard-card__row--self {
  background: color-mix(in srgb, var(--color-success-50) 60%, var(--color-white));
}

.scoreboard-card__name {
  display: flex;
  flex: 1;
  min-width: 0;
  flex-wrap: wrap;
  align-items: center;
  gap: 0.3rem;
  font-size: 0.85rem;
  font-weight: 600;
  color: var(--color-gray-800);
  overflow-wrap: anywhere;
}

.scoreboard-card__tag {
  display: inline-flex;
  align-items: center;
  padding: 0.05rem 0.4rem;
  border-radius: 9999px;
  font-size: 0.6rem;
  font-weight: 700;
  letter-spacing: 0.02em;
  background: var(--color-secondary-50);
  color: var(--color-secondary-700);
}

.scoreboard-card__tag--self {
  background: var(--color-success-100);
  color: var(--color-success-700);
}

.scoreboard-card__tag--muted {
  background: var(--color-gray-200);
  color: var(--color-gray-500);
}

.scoreboard-card__score {
  flex-shrink: 0;
  font-family: monospace;
  font-size: 0.85rem;
  color: var(--color-gray-600);
}

.scoreboard-card__actions {
  display: flex;
  align-items: center;
  gap: 0.3rem;
  flex-shrink: 0;
}

.scoreboard-card__kick {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 1.5rem;
  height: 1.5rem;
  border-radius: 9999px;
  border: 2px solid var(--color-error-500);
  background: var(--color-error-50);
  color: var(--color-error-700);
  cursor: pointer;
  transition:
    transform 0.15s ease,
    background 0.15s ease;
}

.scoreboard-card__kick:hover {
  background: var(--color-error-100);
  transform: scale(1.08);
}

.scoreboard-card__kick--confirm {
  border-color: var(--color-success-500);
  background: var(--color-success-50);
  color: var(--color-success-700);
}

.scoreboard-card__kick--confirm:hover {
  background: var(--color-success-100);
}
</style>
