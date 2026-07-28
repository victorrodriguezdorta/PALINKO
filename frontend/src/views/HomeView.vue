<template>
  <div class="mx-auto max-w-md p-6">
    <h1 class="mb-6 text-2xl font-bold">Human or AI</h1>

    <section class="mb-8 rounded-lg border border-gray-300 p-4">
      <h2 class="mb-3 font-semibold">Crear sala</h2>
      <form class="flex flex-col gap-2" @submit.prevent="onCreateRoom">
        <label class="text-sm">
          Tu nombre
          <input v-model="hostName" class="w-full rounded border border-gray-300 p-2" required maxlength="24" />
        </label>
        <label class="text-sm">
          Número de rondas
          <input v-model.number="totalRounds" type="number" min="1" max="50" class="w-full rounded border border-gray-300 p-2" />
        </label>
        <label class="text-sm">
          Segundos para responder
          <input v-model.number="answerTimeSeconds" type="number" min="5" class="w-full rounded border border-gray-300 p-2" />
        </label>
        <label class="text-sm">
          Segundos para votar
          <input v-model.number="voteTimeSeconds" type="number" min="5" class="w-full rounded border border-gray-300 p-2" />
        </label>
        <button type="submit" class="mt-2 rounded bg-blue-600 p-2 font-semibold text-white" :disabled="loading">
          Crear sala
        </button>
      </form>
    </section>

    <section class="rounded-lg border border-gray-300 p-4">
      <h2 class="mb-3 font-semibold">Unirse a sala</h2>
      <form class="flex flex-col gap-2" @submit.prevent="onJoinRoom">
        <label class="text-sm">
          Código de sala
          <input
            v-model="joinCode"
            class="w-full rounded border border-gray-300 p-2 uppercase"
            required
            maxlength="6"
          />
        </label>
        <label class="text-sm">
          Tu nombre
          <input v-model="joinName" class="w-full rounded border border-gray-300 p-2" required maxlength="24" />
        </label>
        <button type="submit" class="mt-2 rounded bg-green-600 p-2 font-semibold text-white" :disabled="loading">
          Unirse
        </button>
      </form>
    </section>

    <p v-if="error" class="mt-4 rounded bg-red-100 p-2 text-sm text-red-700">{{ error }}</p>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useGameStore } from '@/stores/game'

const router = useRouter()
const gameStore = useGameStore()

const hostName = ref('')
const totalRounds = ref(5)
const answerTimeSeconds = ref(45)
const voteTimeSeconds = ref(30)

const joinCode = ref('')
const joinName = ref('')

const loading = ref(false)
const error = ref<string | null>(null)

async function onCreateRoom() {
  loading.value = true
  error.value = null
  try {
    await gameStore.createRoom(hostName.value, totalRounds.value, answerTimeSeconds.value, voteTimeSeconds.value)
    router.push(`/room/${gameStore.roomCode}`)
  } catch {
    error.value = 'No se pudo crear la sala.'
  } finally {
    loading.value = false
  }
}

async function onJoinRoom() {
  loading.value = true
  error.value = null
  try {
    await gameStore.joinRoom(joinCode.value.trim().toUpperCase(), joinName.value)
    router.push(`/room/${gameStore.roomCode}`)
  } catch {
    error.value = 'No se pudo unir a la sala. Revisa el código y el nombre.'
  } finally {
    loading.value = false
  }
}
</script>
