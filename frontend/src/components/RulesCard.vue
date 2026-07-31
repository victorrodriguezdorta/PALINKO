<template>
  <CartoonCard v-if="isHost" :accent="THEME_COLORS.accent500">
    <template #title>{{ t('room.lobby.rulesHeading') }}</template>

    <label class="rules-card__field">
      {{ t('room.lobby.wordTimeLabel') }}
      <input v-model.number="editWordTimeSeconds" type="number" min="5" class="rules-card__input" @change="emitUpdate" />
    </label>

    <label class="rules-card__field">
      {{ t('room.lobby.languageLabel') }}
      <select v-model="editLanguage" class="rules-card__input" @change="emitUpdate">
        <option v-for="option in SUPPORTED_LANGUAGES" :key="option.locale" :value="option.gameLanguage">
          {{ option.label }}
        </option>
      </select>
    </label>

    <label class="rules-card__field">
      {{ t('room.lobby.phaseCountLabel') }}
      <input
        v-model.number="editPhaseCount"
        type="number"
        min="1"
        :max="maxPhaseCount"
        class="rules-card__input"
        @change="emitUpdate"
      />
    </label>

    <label class="rules-card__field">
      {{ t('room.lobby.infiltratorsToggleLabel') }}
      <select v-model="infiltratorsEnabledSelect" class="rules-card__input" :disabled="!canEnableInfiltrators" @change="onToggleInfiltrators">
        <option value="off">{{ t('room.lobby.infiltratorsToggleOff') }}</option>
        <option value="on">{{ t('room.lobby.infiltratorsToggleOn') }}</option>
      </select>
    </label>
    <p v-if="!canEnableInfiltrators" class="rules-card__hint">{{ t('room.lobby.infiltratorsToggleMinPlayers') }}</p>

    <template v-if="infiltratorsEnabledSelect === 'on'">
      <label class="rules-card__field">
        {{ t('room.lobby.infiltratorCountLabel') }}
        <input
          v-model.number="editInfiltratorCount"
          type="number"
          min="1"
          :max="maxInfiltratorCount"
          class="rules-card__input"
          :class="{ 'rules-card__input--error': infiltratorCountError }"
          @input="onInfiltratorCountInput"
        />
      </label>
      <p v-if="infiltratorCountError" class="rules-card__error">
        {{ t('room.lobby.infiltratorCountError', { max: maxInfiltratorCount }) }}
      </p>

      <label class="rules-card__field">
        {{ t('room.lobby.voteTimeLabel') }}
        <input v-model.number="editVoteTimeSeconds" type="number" min="5" class="rules-card__input" @change="emitUpdate" />
      </label>
    </template>
  </CartoonCard>

  <div v-else class="rules-card-flat">
    <h2 class="rules-card-flat__title">{{ t('room.lobby.rulesHeading') }}</h2>
    <ul class="rules-card-flat__list">
      <li>{{ t('room.lobby.wordTimeReadonly', { value: settings.wordTimeSeconds }) }}</li>
      <li>{{ t('room.lobby.languageReadonly', { value: t(`languages.${settings.language}`) }) }}</li>
      <li>{{ t('room.lobby.phaseCountReadonly', { value: settings.phaseCount }) }}</li>
      <li>{{ t('room.lobby.infiltratorCountReadonly', { value: settings.infiltratorCount }) }}</li>
      <li v-if="settings.infiltratorCount > 0">
        {{ t('room.lobby.voteTimeReadonly', { value: settings.voteTimeSeconds }) }}
      </li>
    </ul>
  </div>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import CartoonCard from '@/components/CartoonCard.vue'
import { SUPPORTED_LANGUAGES } from '@/i18n/languages'
import { THEME_COLORS } from '@/assets/theme'
import type { GameLanguage, RoomSettingsView } from '@/types/game'

const props = defineProps<{
  settings: RoomSettingsView
  isHost: boolean
  maxInfiltratorCount: number
  maxPhaseCount: number
  playerCount: number
}>()

const emit = defineEmits<{
  update: [
    wordTimeSeconds: number,
    voteTimeSeconds: number,
    language: GameLanguage,
    infiltratorCount: number,
    phaseCount: number,
  ]
  'validity-change': [valid: boolean]
}>()

const { t } = useI18n()

const editWordTimeSeconds = ref(props.settings.wordTimeSeconds)
const editVoteTimeSeconds = ref(props.settings.voteTimeSeconds)
const editLanguage = ref<GameLanguage>(props.settings.language)
const editInfiltratorCount = ref(props.settings.infiltratorCount)
const editPhaseCount = ref(props.settings.phaseCount)
const infiltratorsEnabledSelect = ref<'on' | 'off'>(props.settings.infiltratorCount > 0 ? 'on' : 'off')
const infiltratorCountError = ref(false)

// Activating the infiltrator role only makes sense once there are enough
// players for at least one non-infiltrator crew member per infiltrator
// (a third of the room), matching the backend's own Room.maxInfiltratorCount.
const canEnableInfiltrators = computed(() => props.playerCount >= 3)

// Re-syncs the editable fields whenever the room's own settings change
// (initial lobby entry, or another update landing in from the server) so
// the host always edits from the rules actually in effect rather than
// stale local values.
watch(
  () => props.settings,
  (settings) => {
    editWordTimeSeconds.value = settings.wordTimeSeconds
    editVoteTimeSeconds.value = settings.voteTimeSeconds
    editLanguage.value = settings.language
    editInfiltratorCount.value = settings.infiltratorCount
    editPhaseCount.value = settings.phaseCount
    infiltratorsEnabledSelect.value = settings.infiltratorCount > 0 ? 'on' : 'off'
  },
  { immediate: true },
)

// If the room drops below the 3-player minimum while infiltrators were
// enabled, force the toggle off and the count back to 0 rather than leaving
// a setting active that the host can no longer edit or re-confirm.
watch(canEnableInfiltrators, (canEnable) => {
  if (!canEnable && infiltratorsEnabledSelect.value === 'on') {
    infiltratorsEnabledSelect.value = 'off'
    editInfiltratorCount.value = 0
    infiltratorCountError.value = false
    emitUpdate()
  }
})

function onToggleInfiltrators() {
  if (infiltratorsEnabledSelect.value === 'on') {
    editInfiltratorCount.value = Math.max(1, Math.min(editInfiltratorCount.value, props.maxInfiltratorCount))
  } else {
    editInfiltratorCount.value = 0
    infiltratorCountError.value = false
  }
  emitUpdate()
}

// The host sees the error the instant they exceed the cap, and the field
// resets to 0 immediately rather than letting an invalid value linger.
function onInfiltratorCountInput() {
  if (editInfiltratorCount.value > props.maxInfiltratorCount) {
    infiltratorCountError.value = true
    editInfiltratorCount.value = 0
    return
  }
  infiltratorCountError.value = false
  emitUpdate()
}

watch(infiltratorCountError, (hasError) => emit('validity-change', !hasError), { immediate: true })

function emitUpdate() {
  emit(
    'update',
    editWordTimeSeconds.value,
    editVoteTimeSeconds.value,
    editLanguage.value,
    editInfiltratorCount.value,
    editPhaseCount.value,
  )
}
</script>

<style scoped>
.rules-card__field {
  display: block;
  margin-bottom: 0.7rem;
  font-family: 'Outfit', sans-serif;
  font-size: 0.85rem;
  font-weight: 600;
  color: var(--color-gray-700);
}

.rules-card__input {
  display: block;
  width: 100%;
  margin-top: 0.3rem;
  padding: 0.5rem 0.7rem;
  border: 2px solid var(--color-gray-300);
  border-radius: 0.75rem;
  font-family: 'Outfit', sans-serif;
  font-weight: 500;
  color: var(--color-gray-800);
  background: var(--color-white);
  transition: border-color 0.15s ease;
}

.rules-card__input:focus {
  outline: none;
  border-color: var(--color-accent-500);
}

.rules-card__input:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.rules-card__input--error {
  border-color: var(--color-error-500);
}

.rules-card__hint {
  margin: -0.35rem 0 0.7rem;
  font-size: 0.72rem;
  color: var(--color-gray-500);
}

.rules-card__error {
  margin: -0.35rem 0 0.7rem;
  font-size: 0.72rem;
  font-weight: 600;
  color: var(--color-error-500);
}

.rules-card-flat {
  padding: 1.1rem 1.25rem;
  border-radius: 1rem;
  border: 2px dashed var(--color-gray-300);
  background: var(--color-gray-50);
}

.rules-card-flat__title {
  margin-bottom: 0.6rem;
  font-family: 'Outfit', sans-serif;
  font-size: 1.05rem;
  font-weight: 600;
  color: var(--color-gray-600);
}

.rules-card-flat__list {
  display: flex;
  flex-direction: column;
  gap: 0.35rem;
  font-family: 'Outfit', sans-serif;
  font-size: 0.85rem;
  color: var(--color-gray-500);
}
</style>
