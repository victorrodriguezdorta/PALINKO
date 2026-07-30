<template>
  <p class="text-xs text-gray-500">{{ t('home.daily.countdown', { value: formatted }) }}</p>
</template>

<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { useI18n } from 'vue-i18n'

const { t } = useI18n()

// The "server day" is the UTC calendar day (see backend DailySeed), so the
// countdown always targets the next UTC midnight regardless of the
// visitor's own timezone.
function nextUtcMidnight(): number {
  const now = new Date()
  return Date.UTC(now.getUTCFullYear(), now.getUTCMonth(), now.getUTCDate() + 1, 0, 0, 0)
}

const now = ref(Date.now())
let tickInterval: ReturnType<typeof setInterval> | undefined

const formatted = computed(() => {
  const remainingMs = Math.max(0, nextUtcMidnight() - now.value)
  const totalSeconds = Math.floor(remainingMs / 1000)
  const hours = Math.floor(totalSeconds / 3600)
  const minutes = Math.floor((totalSeconds % 3600) / 60)
  const seconds = totalSeconds % 60
  const pad = (value: number) => value.toString().padStart(2, '0')
  return `${pad(hours)}:${pad(minutes)}:${pad(seconds)}`
})

onMounted(() => {
  tickInterval = setInterval(() => {
    now.value = Date.now()
  }, 1000)
})

onUnmounted(() => {
  if (tickInterval) clearInterval(tickInterval)
})
</script>
