<template>
  <div class="mx-auto max-w-md p-6">
    <div class="mb-6 flex items-center justify-between">
      <h1 class="text-2xl font-bold">{{ t('debug.wordRelation.heading') }}</h1>
      <RouterLink to="/" class="text-sm text-blue-600 underline">{{ t('debug.back') }}</RouterLink>
    </div>

    <section class="rounded-lg border border-gray-300 p-4">
      <form class="flex flex-col gap-2" @submit.prevent="onCalculate">
        <label class="text-sm">
          {{ t('debug.wordRelation.wordALabel') }}
          <input v-model="wordA" class="w-full rounded border border-gray-300 p-2" required maxlength="64" />
        </label>
        <label class="text-sm">
          {{ t('debug.wordRelation.wordBLabel') }}
          <input v-model="wordB" class="w-full rounded border border-gray-300 p-2" required maxlength="64" />
        </label>
        <label class="text-sm">
          {{ t('debug.wordRelation.languageLabel') }}
          <select v-model="language" class="w-full rounded border border-gray-300 p-2">
            <option v-for="option in SUPPORTED_LANGUAGES" :key="option.gameLanguage" :value="option.gameLanguage">
              {{ option.label }}
            </option>
          </select>
        </label>
        <button type="submit" class="mt-2 rounded bg-blue-600 p-2 font-semibold text-white" :disabled="loading">
          {{ t('debug.wordRelation.submit') }}
        </button>
      </form>

      <p v-if="result !== null" class="mt-4 text-center text-3xl font-bold">{{ result.relatednessPercentage }}%</p>
      <p v-if="result?.justification" class="mt-2 text-center text-sm text-gray-600">
        <span class="font-semibold">{{ t('debug.wordRelation.justificationLabel') }}:</span>
        {{ result.justification }}
      </p>
      <p v-if="error" class="mt-4 rounded bg-red-100 p-2 text-sm text-red-700">{{ error }}</p>
    </section>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { calculateWordRelation, type WordRelationResult } from '@/services/debugService'
import { SUPPORTED_LANGUAGES } from '@/i18n/languages'
import { translateError } from '@/i18n'
import type { ApiError, GameLanguage } from '@/types/game'

const { t } = useI18n()

const wordA = ref('')
const wordB = ref('')
const language = ref<GameLanguage>('SPANISH')

const loading = ref(false)
const error = ref<string | null>(null)
const result = ref<WordRelationResult | null>(null)

async function onCalculate() {
  loading.value = true
  error.value = null
  result.value = null
  try {
    result.value = await calculateWordRelation({
      wordA: wordA.value,
      wordB: wordB.value,
      language: language.value,
    })
  } catch (err) {
    error.value = translateError(err as ApiError)
  } finally {
    loading.value = false
  }
}
</script>
