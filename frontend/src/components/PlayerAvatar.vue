<template>
  <button
    v-if="editable"
    type="button"
    class="player-avatar player-avatar--editable"
    :class="`player-avatar--${size}`"
    :aria-label="shuffleLabel"
    @click="$emit('shuffle')"
  >
    <span class="player-avatar__circle">
      <img :src="avatarUrl" :alt="alt" class="player-avatar__image" draggable="false" />
    </span>
    <span class="player-avatar__shuffle" aria-hidden="true">
      <Shuffle :size="shuffleIconSize" />
    </span>
  </button>

  <span v-else class="player-avatar" :class="`player-avatar--${size}`">
    <span class="player-avatar__circle">
      <img :src="avatarUrl" :alt="alt" class="player-avatar__image" draggable="false" />
    </span>
  </span>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { Shuffle } from 'lucide-vue-next'
import { avatarUrlForSeed } from '@/utils/avatar'

const props = withDefaults(
  defineProps<{
    seed: string
    alt?: string
    size?: 'sm' | 'md' | 'lg'
    editable?: boolean
    shuffleLabel?: string
  }>(),
  {
    alt: '',
    size: 'md',
    editable: false,
    shuffleLabel: 'Shuffle avatar',
  },
)

defineEmits<{
  shuffle: []
}>()

const avatarUrl = computed(() => avatarUrlForSeed(props.seed))

const shuffleIconSize = computed(() => (props.size === 'lg' ? 16 : props.size === 'sm' ? 11 : 13))
</script>

<style scoped>
.player-avatar {
  --avatar-size: 3.5rem;
  --avatar-overflow: 1.65;
  --avatar-shift: 0.34;

  position: relative;
  display: inline-flex;
  width: var(--avatar-size);
  height: var(--avatar-size);
  border: none;
  background: none;
  padding: 0;
  overflow: visible;
}

.player-avatar--sm {
  --avatar-size: 2.5rem;
}

.player-avatar--md {
  --avatar-size: 3.5rem;
}

.player-avatar--lg {
  --avatar-size: 6.5rem;
}

.player-avatar--editable {
  cursor: pointer;
}

.player-avatar__circle {
  position: relative;
  display: block;
  width: 100%;
  height: 100%;
  border-radius: 9999px;
  background: var(--color-white);
  border: 3px solid var(--color-gray-400);
  overflow: hidden;
  box-shadow: 0 3px 0 color-mix(in srgb, var(--color-gray-900) 15%, transparent);
  transition:
    transform 0.15s cubic-bezier(0.34, 1.56, 0.64, 1),
    border-color 0.15s ease;
}

.player-avatar--editable:hover .player-avatar__circle {
  transform: translateY(-2px);
  border-color: var(--color-gray-500);
}

.player-avatar--editable:active .player-avatar__circle {
  transform: translateY(1px);
}

/* The dicebear artwork has its own internal padding around the figure, so
   simply anchoring it to the container's bottom edge still leaves a gap
   below the chin. Oversizing it and then pushing it past the bottom edge
   (negative offset) drives the figure's body down to actually touch the
   rim, while growing large enough that the head clears the top and pokes
   out above the circle (transparent background lets it read as "popping
   out"); overflow:hidden on the circle clips everything below the rim. */
.player-avatar__image {
  position: absolute;
  left: 50%;
  bottom: calc(var(--avatar-size) * var(--avatar-shift) * -1);
  width: calc(var(--avatar-size) * var(--avatar-overflow));
  height: calc(var(--avatar-size) * var(--avatar-overflow));
  transform: translateX(-50%);
  pointer-events: none;
  user-select: none;
}

.player-avatar__shuffle {
  position: absolute;
  right: -0.1rem;
  bottom: -0.1rem;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 1.6rem;
  height: 1.6rem;
  border-radius: 9999px;
  background: var(--color-gray-500);
  color: var(--color-white);
  border: 2px solid var(--color-white);
  box-shadow: 0 2px 0 color-mix(in srgb, var(--color-gray-900) 20%, transparent);
}

.player-avatar--sm .player-avatar__shuffle {
  width: 1.3rem;
  height: 1.3rem;
}

.player-avatar--lg .player-avatar__shuffle {
  width: 2rem;
  height: 2rem;
}

@media (prefers-reduced-motion: reduce) {
  .player-avatar__circle {
    transition: none;
  }
}
</style>
