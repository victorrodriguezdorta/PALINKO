<template>
  <div class="mx-auto max-w-2xl p-6">
    <header class="mb-4 flex items-center justify-between">
      <h1 class="text-xl font-bold">Sala {{ roomCode }}</h1>
      <button class="text-sm text-blue-600 underline" @click="onLeave">Volver al inicio</button>
    </header>

    <p v-if="gameStore.errorMessage" class="mb-4 rounded bg-red-100 p-2 text-sm text-red-700">
      {{ gameStore.errorMessage }}
      <button class="ml-2 underline" @click="gameStore.dismissError">cerrar</button>
    </p>

    <div v-if="!snapshot" class="text-gray-500">Conectando...</div>

    <template v-else>
      <section class="mb-6">
        <h2 class="mb-2 font-semibold">Jugadores</h2>
        <ul class="flex flex-col gap-1">
          <li
            v-for="player in snapshot.players"
            :key="player.id"
            class="flex items-center justify-between rounded border border-gray-200 px-3 py-1 text-sm"
          >
            <span>
              {{ player.name }}
              <span v-if="player.host" class="text-xs text-blue-600">(host)</span>
              <span v-if="!player.connected" class="text-xs text-gray-400">(desconectado)</span>
            </span>
            <span class="font-mono">{{ player.score }} pts</span>
          </li>
        </ul>
      </section>

      <section v-if="snapshot.status === 'LOBBY'">
        <p class="mb-4">Comparte el código <strong>{{ roomCode }}</strong> con el resto de jugadores.</p>

        <div class="mb-4 rounded border border-gray-300 p-3">
          <h3 class="mb-2 font-semibold">Reglas de la partida</h3>
          <template v-if="gameStore.isHost">
            <label class="mb-2 block text-sm">
              Número de rondas
              <input
                v-model.number="editTotalRounds"
                type="number"
                min="1"
                max="50"
                class="w-full rounded border border-gray-300 p-2"
              />
            </label>
            <label class="mb-2 block text-sm">
              Segundos para responder
              <input
                v-model.number="editAnswerTimeSeconds"
                type="number"
                min="5"
                class="w-full rounded border border-gray-300 p-2"
              />
            </label>
            <label class="mb-2 block text-sm">
              Segundos para votar
              <input
                v-model.number="editVoteTimeSeconds"
                type="number"
                min="5"
                class="w-full rounded border border-gray-300 p-2"
              />
            </label>
            <button
              class="rounded bg-gray-700 px-3 py-1.5 text-sm font-semibold text-white"
              @click="onUpdateSettings"
            >
              Guardar reglas
            </button>
          </template>
          <ul v-else class="text-sm text-gray-600">
            <li>Rondas: {{ snapshot.settings.totalRounds }}</li>
            <li>Tiempo para responder: {{ snapshot.settings.answerTimeSeconds }}s</li>
            <li>Tiempo para votar: {{ snapshot.settings.voteTimeSeconds }}s</li>
          </ul>
        </div>

        <button v-if="gameStore.isHost" class="rounded bg-blue-600 px-4 py-2 font-semibold text-white" @click="gameStore.start">
          Iniciar partida
        </button>
        <p v-else class="text-gray-500">Esperando a que el host inicie la partida...</p>
      </section>

      <section v-else-if="snapshot.status === 'IN_PROGRESS' && round">
        <p class="mb-2 text-sm text-gray-500">
          Ronda {{ round.roundNumber }} / {{ snapshot.settings.totalRounds }} — fase: {{ round.phase }}
        </p>
        <h2 class="mb-4 text-lg font-semibold">{{ round.questionText }}</h2>

        <div v-if="round.phase === 'ANSWERING'">
          <textarea
            v-model="answerText"
            class="mb-2 w-full rounded border border-gray-300 p-2"
            rows="3"
            :disabled="hasAnswered"
            placeholder="Escribe tu respuesta..."
          ></textarea>
          <button
            v-if="!hasAnswered"
            class="rounded bg-blue-600 px-4 py-2 font-semibold text-white disabled:opacity-50"
            :disabled="!answerText.trim()"
            @click="onSubmitAnswer"
          >
            Listo
          </button>
          <template v-else>
            <p class="mb-2 text-sm text-gray-500">
              Respuesta enviada. Puedes cancelarla para editarla mientras siga esta fase.
            </p>
            <button
              class="rounded bg-gray-600 px-4 py-2 font-semibold text-white"
              @click="onCancelAnswer"
            >
              Cancelar y editar
            </button>
          </template>
        </div>

        <div v-else-if="round.phase === 'VOTING'">
          <p class="mb-2 text-sm text-gray-500">¿Cuál crees que es la respuesta de la IA?</p>
          <ul class="mb-3 flex flex-col gap-2">
            <li v-for="answer in round.answers" :key="answer.id">
              <button
                class="w-full rounded border p-2 text-left disabled:cursor-not-allowed disabled:opacity-50"
                :class="selectedAnswerId === answer.id ? 'border-blue-600 bg-blue-50' : 'border-gray-300'"
                :disabled="hasVoted || isMyAnswer(answer)"
                @click="selectedAnswerId = answer.id"
              >
                {{ answer.text }}
                <span v-if="isMyAnswer(answer)" class="text-xs text-gray-400">(tu respuesta)</span>
              </button>
              <p v-if="answer.voterPlayerIds.length > 0" class="mt-1 pl-2 text-xs text-gray-500">
                Han votado: {{ voterNames(answer) }}
              </p>
            </li>
          </ul>
          <button
            class="rounded bg-green-600 px-4 py-2 font-semibold text-white disabled:opacity-50"
            :disabled="hasVoted || !selectedAnswerId"
            @click="onSubmitVote"
          >
            {{ hasVoted ? 'Voto enviado' : 'Votar' }}
          </button>
        </div>

        <div v-else-if="round.phase === 'REVEAL'">
          <ul class="mb-4 flex flex-col gap-2">
            <li v-for="answer in round.answers" :key="answer.id" class="rounded border border-gray-300 p-2">
              <span class="font-semibold">{{ answer.isAi ? 'IA' : authorName(answer.authorPlayerId) }}:</span>
              {{ answer.text }}
            </li>
          </ul>
          <ul v-if="round.result" class="mb-4 text-sm text-gray-600">
            <li v-for="(delta, pId) in round.result.scoreDeltaByPlayerId" :key="pId">
              {{ authorName(String(pId)) }}: +{{ delta }} pts esta ronda
            </li>
          </ul>
          <button
            v-if="gameStore.isHost"
            class="rounded bg-blue-600 px-4 py-2 font-semibold text-white"
            @click="gameStore.nextRound"
          >
            Siguiente ronda
          </button>
          <p v-else class="text-gray-500">Esperando a que el host continúe...</p>
        </div>
      </section>

      <section v-else-if="snapshot.status === 'FINISHED'">
        <h2 class="mb-3 text-lg font-semibold">Partida terminada</h2>
        <ol class="mb-4 flex flex-col gap-1">
          <li
            v-for="player in sortedByScore"
            :key="player.id"
            class="flex justify-between rounded border border-gray-300 p-2"
          >
            <span>{{ player.name }}</span>
            <span class="font-mono">{{ player.score }} pts</span>
          </li>
        </ol>
        <div class="flex items-center gap-3">
          <button
            v-if="gameStore.isHost"
            class="rounded bg-green-600 px-4 py-2 font-semibold text-white"
            @click="gameStore.playAgain"
          >
            Jugar de nuevo
          </button>
          <button class="rounded bg-blue-600 px-4 py-2 font-semibold text-white" @click="onLeave">
            Volver al inicio
          </button>
        </div>
        <p v-if="!gameStore.isHost" class="mt-2 text-sm text-gray-500">
          Esperando a que el host decida si jugar de nuevo...
        </p>
      </section>

      <section v-else-if="snapshot.status === 'CLOSED'">
        <h2 class="mb-3 text-lg font-semibold">El host abandonó la sala</h2>
        <p class="mb-4 text-gray-500">La partida ha terminado. Volviendo al inicio...</p>
        <button class="rounded bg-blue-600 px-4 py-2 font-semibold text-white" @click="onLeave">
          Volver al inicio ahora
        </button>
      </section>
    </template>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useGameStore } from '@/stores/game'
import type { AnswerView } from '@/types/game'

const route = useRoute()
const router = useRouter()
const gameStore = useGameStore()

const roomCode = route.params.code as string

const answerText = ref('')
const selectedAnswerId = ref<string | null>(null)
const hasAnswered = ref(false)
const hasVoted = ref(false)

const editTotalRounds = ref(5)
const editAnswerTimeSeconds = ref(45)
const editVoteTimeSeconds = ref(30)

const snapshot = computed(() => gameStore.snapshot)
const round = computed(() => snapshot.value?.currentRound ?? null)

const sortedByScore = computed(() => [...(snapshot.value?.players ?? [])].sort((a, b) => b.score - a.score))

function authorName(playerId: string | null): string {
  if (!playerId) return 'IA'
  return snapshot.value?.players.find((p) => p.id === playerId)?.name ?? playerId
}

// Who voted for what is visible live during VOTING — this only exposes
// vote choices, never who WROTE an answer (authorPlayerId stays hidden
// until REVEAL), so the "guess the AI" mechanic is unaffected.
function voterNames(answer: AnswerView): string {
  return answer.voterPlayerIds
    .map((id) => snapshot.value?.players.find((p) => p.id === id)?.name ?? id)
    .join(', ')
}

// Answers are anonymized (authorPlayerId is null) until REVEAL, so the only
// way to know which one is mine during VOTING is to compare against the
// text I myself submitted this round — the backend never tells even me
// which id is mine ahead of time.
function isMyAnswer(answer: AnswerView): boolean {
  const myText = answerText.value.trim()
  return myText.length > 0 && answer.text === myText
}

watch(
  () => round.value?.roundNumber,
  () => {
    answerText.value = ''
    selectedAnswerId.value = null
    hasAnswered.value = false
    hasVoted.value = false
  },
)

watch(
  () => snapshot.value?.status,
  (status) => {
    if (status === 'CLOSED') {
      setTimeout(onLeave, 3000)
    }
    // Re-sync the editable fields whenever the room (re-)enters LOBBY —
    // covers the initial lobby and a "jugar de nuevo" reset alike, so the
    // host always sees the rules that are actually in effect rather than
    // stale values from a previous visit.
    if (status === 'LOBBY' && snapshot.value?.settings) {
      editTotalRounds.value = snapshot.value.settings.totalRounds
      editAnswerTimeSeconds.value = snapshot.value.settings.answerTimeSeconds
      editVoteTimeSeconds.value = snapshot.value.settings.voteTimeSeconds
    }
  },
  { immediate: true },
)

function onUpdateSettings() {
  gameStore.updateSettings(editTotalRounds.value, editAnswerTimeSeconds.value, editVoteTimeSeconds.value)
}

function onSubmitAnswer() {
  gameStore.submitAnswer(answerText.value.trim())
  hasAnswered.value = true
}

function onCancelAnswer() {
  gameStore.cancelAnswer()
  hasAnswered.value = false
}

function onSubmitVote() {
  if (!selectedAnswerId.value) return
  gameStore.submitVote(selectedAnswerId.value)
  hasVoted.value = true
}

function onLeave() {
  gameStore.leaveRoom()
  router.push('/')
}

onMounted(() => {
  if (!gameStore.hasIdentityFor(roomCode)) {
    router.push('/')
    return
  }
  gameStore.connectSocket()
})
</script>
