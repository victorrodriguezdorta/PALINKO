<template>
  <div class="player-card" :class="{ 'player-card--self': isSelf }">
    <PlayerAvatar :seed="player.avatarSeed" size="sm" />
    <div class="player-card__info">
      <p class="player-card__name">
        {{ player.name }}
        <span v-if="isSelf" class="player-card__tag player-card__tag--self">{{ t('room.players.you') }}</span>
        <span v-if="player.host" class="player-card__tag">{{ t('room.players.host') }}</span>
        <span v-if="!player.connected" class="player-card__tag player-card__tag--muted">
          {{ t('room.players.disconnected') }}
        </span>
      </p>
    </div>

    <div v-if="canKick" class="player-card__actions">
      <template v-if="confirmingKick">
        <span class="player-card__confirm-text">{{ t('room.players.kickConfirm', { name: player.name }) }}</span>
        <CartoonButton size="sm" :color="THEME_COLORS.error500" @click="onConfirmKick">
          {{ t('room.players.kickConfirmYes') }}
        </CartoonButton>
        <CartoonButton size="sm" :color="THEME_COLORS.gray400" @click="confirmingKick = false">
          {{ t('room.players.kickConfirmCancel') }}
        </CartoonButton>
      </template>
      <button
        v-else
        type="button"
        class="player-card__kick"
        :aria-label="t('room.players.kickAriaLabel', { name: player.name })"
        @click="confirmingKick = true"
      >
        <X :size="14" aria-hidden="true" />
      </button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { X } from 'lucide-vue-next'
import PlayerAvatar from '@/components/PlayerAvatar.vue'
import CartoonButton from '@/components/CartoonButton.vue'
import { THEME_COLORS } from '@/assets/theme'
import type { PlayerView } from '@/types/game'

defineProps<{
  player: PlayerView
  isSelf: boolean
  canKick: boolean
}>()

const emit = defineEmits<{
  kick: []
}>()

const { t } = useI18n()

const confirmingKick = ref(false)

function onConfirmKick() {
  confirmingKick.value = false
  emit('kick')
}
</script>

<style scoped>
.player-card {
  position: relative;
  display: flex;
  align-items: center;
  gap: 0.75rem;
  padding: 0.6rem 0.85rem;
  border-radius: 1.1rem;
  background: var(--color-white);
  border: 3px solid var(--color-gray-300);
  box-shadow: 0 4px 0 color-mix(in srgb, var(--color-gray-300) 65%, var(--color-black) 15%);
  transition:
    transform 0.15s cubic-bezier(0.34, 1.56, 0.64, 1),
    border-color 0.15s ease,
    box-shadow 0.15s ease;
}

.player-card:hover {
  transform: translateY(-2px);
}

.player-card--self {
  border-color: var(--color-success-500);
  box-shadow: 0 4px 0 color-mix(in srgb, var(--color-success-500) 65%, var(--color-black) 15%);
  background: color-mix(in srgb, var(--color-success-50) 60%, var(--color-white));
}

.player-card__info {
  min-width: 0;
  flex: 1;
}

.player-card__name {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 0.35rem;
  font-family: 'Fredoka', 'Outfit', sans-serif;
  font-weight: 600;
  font-size: 0.95rem;
  color: var(--color-gray-800);
  overflow-wrap: anywhere;
}

.player-card__tag {
  display: inline-flex;
  align-items: center;
  padding: 0.1rem 0.45rem;
  border-radius: 9999px;
  font-size: 0.65rem;
  font-weight: 700;
  letter-spacing: 0.02em;
  background: var(--color-secondary-50);
  color: var(--color-secondary-700);
}

.player-card__tag--self {
  background: var(--color-success-100);
  color: var(--color-success-700);
}

.player-card__tag--muted {
  background: var(--color-gray-200);
  color: var(--color-gray-500);
}

.player-card__actions {
  display: flex;
  align-items: center;
  gap: 0.4rem;
  flex-shrink: 0;
}

.player-card__confirm-text {
  font-size: 0.75rem;
  color: var(--color-gray-600);
  white-space: nowrap;
}

.player-card__kick {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 1.75rem;
  height: 1.75rem;
  border-radius: 9999px;
  border: 2px solid var(--color-error-500);
  background: var(--color-error-50);
  color: var(--color-error-700);
  cursor: pointer;
  transition:
    transform 0.15s ease,
    background 0.15s ease;
}

.player-card__kick:hover {
  background: var(--color-error-100);
  transform: scale(1.08);
}

@media (prefers-reduced-motion: reduce) {
  .player-card,
  .player-card__kick {
    transition: none;
  }
}
</style>
