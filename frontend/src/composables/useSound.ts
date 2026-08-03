// Playback engine for the effects declared in src/assets/sounds.ts. Kept
// deliberately dumb: components/stores never touch an Audio element or a
// file path directly, they just call play('wordAccepted') and this module
// resolves the name, respects the shared mute/volume state, and fires it.
import { ref, watch } from 'vue'
import { SOUND_EFFECTS, type SoundEffectName } from '@/assets/sounds'

const MUTED_KEY = 'human-or-ai:sound-muted'
const VOLUME_KEY = 'human-or-ai:sound-volume'

const isMuted = ref(localStorage.getItem(MUTED_KEY) === 'true')
const volume = ref(Number(localStorage.getItem(VOLUME_KEY) ?? '0.6'))

watch(isMuted, (value) => localStorage.setItem(MUTED_KEY, String(value)))
watch(volume, (value) => localStorage.setItem(VOLUME_KEY, String(value)))

// One pooled <audio> element per effect, cloned on each play() call so the
// same effect can overlap itself (e.g. rapid clicks) without cutting off
// the previous instance.
const audioCache = new Map<SoundEffectName, HTMLAudioElement>()

function getBaseAudio(name: SoundEffectName): HTMLAudioElement {
  let audio = audioCache.get(name)
  if (!audio) {
    audio = new Audio(SOUND_EFFECTS[name])
    audioCache.set(name, audio)
  }
  return audio
}

function play(name: SoundEffectName) {
  if (isMuted.value || volume.value <= 0) return
  const base = getBaseAudio(name)
  const instance = base.cloneNode(true) as HTMLAudioElement
  instance.volume = volume.value
  // Playback can be rejected by the browser if it fires before any user
  // gesture has unlocked audio — harmless, so just swallow it.
  instance.play().catch(() => {})
}

export function useSound() {
  return {
    play,
    isMuted,
    volume,
    toggleMuted: () => {
      isMuted.value = !isMuted.value
    },
  }
}
