<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { useSecretGameStore } from '@/stores/secretGame'
import type { JudgementSource, SolutionMatchLevel } from '@/types/secretGame'

const store = useSecretGameStore()

const wordInput = ref('')
const formulationInput = ref('')
const loadingDailyRoom = ref(false)

const snapshot = computed(() => store.snapshot)
const hasIdentity = computed(() => !!store.roomCode)
const isFinished = computed(() => snapshot.value?.status === 'SOLVED' || snapshot.value?.status === 'GAVE_UP')

const acceptedAttempts = computed(() => snapshot.value?.attempts.filter((a) => a.outcome === 'ACCEPTED') ?? [])
const rejectedAttempts = computed(() => snapshot.value?.attempts.filter((a) => a.outcome === 'REJECTED') ?? [])

const sourceLabel: Record<JudgementSource, string> = {
  WORD_BANK: 'banco',
  AI: 'IA',
  HINT: 'pista',
}

const matchLevelLabel: Record<SolutionMatchLevel, string> = {
  CORRECT: 'Correcto',
  PARTIAL: 'Parcial',
  INCORRECT: 'Incorrecto',
}

async function playDaily() {
  loadingDailyRoom.value = true
  try {
    await store.createDailyRoom('SPANISH')
    store.connectSocket()
  } catch (e) {
    console.error(e)
  } finally {
    loadingDailyRoom.value = false
  }
}

function submitWord() {
  const text = wordInput.value.trim()
  if (!text) return
  store.submitWord(text)
  wordInput.value = ''
}

function useHint() {
  store.useHint()
}

function submitSolution() {
  const text = formulationInput.value.trim()
  if (!text) return
  store.submitSolution(text)
  formulationInput.value = ''
}

function giveUp() {
  if (confirm('¿Seguro que quieres rendirte?')) {
    store.giveUp()
  }
}

function leave() {
  store.leaveRoom()
}

onMounted(() => {
  if (hasIdentity.value) {
    store.connectSocket()
  }
})

onUnmounted(() => {
  // Only tear down the socket, never clear the stored identity here: a
  // route change/reload should still be able to reconnect to the same
  // room, exactly like the original mode's RoomView.
})
</script>

<template>
  <div class="secret-page">
    <header class="secret-header">
      <h1>PALINKO: CODE</h1>
      <p class="subtitle">Modo de prueba — descubre la cualidad secreta</p>
    </header>

    <section v-if="!hasIdentity" class="panel">
      <p>Cada día hay una cualidad secreta oculta. Prueba palabras para averiguar cuáles la cumplen.</p>
      <button :disabled="loadingDailyRoom" @click="playDaily">
        {{ loadingDailyRoom ? 'Cargando...' : 'Jugar el reto diario' }}
      </button>
    </section>

    <section v-else-if="snapshot" class="game">
      <div class="top-bar">
        <span>Sala: {{ snapshot.roomCode }}</span>
        <span>Concepto: {{ snapshot.conceptId }}</span>
        <button class="link" @click="leave">Salir</button>
      </div>

      <div v-if="store.errorMessage" class="error-banner">
        {{ store.errorMessage.code }}: {{ store.errorMessage.message }}
      </div>

      <div v-if="!isFinished" class="panels">
        <div class="panel">
          <h2>Probar palabra</h2>
          <form @submit.prevent="submitWord">
            <input v-model="wordInput" type="text" placeholder="Escribe una palabra..." autocomplete="off" />
            <button type="submit">Enviar</button>
          </form>

          <div class="hint-row">
            <span>Fallos: {{ snapshot.totalFailures }} (cada {{ snapshot.failuresPerHint }} desbloquea 1 pista)</span>
            <button :disabled="snapshot.hintsAvailable <= snapshot.hintsUsed" @click="useHint">
              Usar pista ({{ snapshot.hintsAvailable - snapshot.hintsUsed }} disponible(s))
            </button>
          </div>

          <div class="word-lists">
            <div class="word-list accepted">
              <h3>Aceptadas ({{ acceptedAttempts.length }})</h3>
              <ul>
                <li v-for="attempt in acceptedAttempts" :key="attempt.id">
                  {{ attempt.text }}
                  <span class="tag">{{ sourceLabel[attempt.source] }}</span>
                </li>
              </ul>
            </div>
            <div class="word-list rejected">
              <h3>Rechazadas ({{ rejectedAttempts.length }})</h3>
              <ul>
                <li v-for="attempt in rejectedAttempts" :key="attempt.id">
                  {{ attempt.text }}
                  <span class="tag">{{ sourceLabel[attempt.source] }}</span>
                </li>
              </ul>
            </div>
          </div>
        </div>

        <div class="panel">
          <h2>Declarar la cualidad secreta</h2>
          <form @submit.prevent="submitSolution">
            <input
              v-model="formulationInput"
              type="text"
              placeholder="Ej: países que empiezan por la letra A"
              autocomplete="off"
            />
            <button type="submit">Comprobar</button>
          </form>

          <ul class="solution-list">
            <li v-for="attempt in snapshot.solutionAttempts" :key="attempt.id">
              "{{ attempt.formulationText }}" — <strong>{{ matchLevelLabel[attempt.matchLevel] }}</strong>
            </li>
          </ul>

          <button class="danger" @click="giveUp">Rendirse</button>
        </div>
      </div>

      <div v-else class="panel result">
        <h2>{{ snapshot.status === 'SOLVED' ? '¡Resuelto!' : 'Partida terminada' }}</h2>
        <p class="hidden-quality">Cualidad secreta: <strong>{{ snapshot.reveal?.hiddenQuality }}</strong></p>
        <p>Puntuación final: <strong>{{ snapshot.reveal?.finalScore }}</strong></p>
        <ul>
          <li>Intentos totales: {{ snapshot.reveal?.totalAttempts }}</li>
          <li>Aceptados: {{ snapshot.reveal?.acceptedAttempts }}</li>
          <li>Rechazados: {{ snapshot.reveal?.rejectedAttempts }}</li>
          <li>Pistas usadas: {{ snapshot.reveal?.hintsUsed }}</li>
        </ul>
        <button @click="leave">Jugar de nuevo</button>
      </div>
    </section>
  </div>
</template>

<style scoped>
.secret-page {
  max-width: 960px;
  margin: 0 auto;
  padding: 2rem 1rem;
  font-family: system-ui, sans-serif;
}

.secret-header {
  text-align: center;
  margin-bottom: 2rem;
}

.subtitle {
  color: #666;
}

.panel {
  background: #f7f7f9;
  border: 1px solid #ddd;
  border-radius: 8px;
  padding: 1.5rem;
  margin-bottom: 1rem;
}

.panels {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 1rem;
}

@media (max-width: 720px) {
  .panels {
    grid-template-columns: 1fr;
  }
}

.top-bar {
  display: flex;
  gap: 1rem;
  align-items: center;
  margin-bottom: 1rem;
  font-size: 0.9rem;
  color: #444;
}

.link {
  background: none;
  border: none;
  color: #3a5;
  cursor: pointer;
  text-decoration: underline;
  margin-left: auto;
}

.error-banner {
  background: #fdecea;
  border: 1px solid #f5c2c0;
  color: #a12a2a;
  padding: 0.75rem 1rem;
  border-radius: 6px;
  margin-bottom: 1rem;
}

form {
  display: flex;
  gap: 0.5rem;
  margin-bottom: 1rem;
}

input {
  flex: 1;
  padding: 0.5rem;
  border: 1px solid #ccc;
  border-radius: 4px;
}

button {
  padding: 0.5rem 1rem;
  border-radius: 4px;
  border: 1px solid #333;
  background: #333;
  color: white;
  cursor: pointer;
}

button:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

button.danger {
  background: #a12a2a;
  border-color: #a12a2a;
}

.hint-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 1rem;
  margin-bottom: 1rem;
  font-size: 0.9rem;
}

.word-lists {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 1rem;
}

.word-list h3 {
  font-size: 0.9rem;
  margin-bottom: 0.5rem;
}

.word-list ul {
  list-style: none;
  padding: 0;
  margin: 0;
  max-height: 240px;
  overflow-y: auto;
}

.word-list li {
  padding: 0.25rem 0.5rem;
  border-radius: 4px;
  margin-bottom: 0.25rem;
  display: flex;
  justify-content: space-between;
}

.word-list.accepted li {
  background: #e6f4ea;
}

.word-list.rejected li {
  background: #fdecea;
}

.tag {
  font-size: 0.7rem;
  color: #666;
  text-transform: uppercase;
}

.solution-list {
  list-style: none;
  padding: 0;
  margin: 0 0 1rem;
}

.solution-list li {
  padding: 0.4rem 0;
  border-bottom: 1px solid #e0e0e0;
}

.result {
  text-align: center;
}

.hidden-quality {
  font-size: 1.1rem;
}
</style>
