import { reactive, ref } from 'vue'
import { useAuthStore } from '@/stores/auth'
import { translateError } from '@/i18n'
import type { ApiError } from '@/types/game'
import { loadOrCreateAvatarSeed, persistAvatarSeed, randomAvatarSeed } from '@/utils/avatar'

/**
 * Shared login/signup form state and submit handlers, used by LoginView
 * (the only place this flow lives now — see its own comment for why the
 * earlier AuthModal was retired in favor of a full page).
 */
export function useAuthForm(onLoginSuccess: () => void) {
  const authStore = useAuthStore()

  const tab = ref<'login' | 'signup'>('login')
  const loading = ref(false)
  const errorMessage = ref('')
  const passwordRules = ref<string[]>([])
  const signupSuccess = ref(false)

  const loginForm = reactive({ username: '', password: '' })
  const signupForm = reactive({ username: '', email: '', password: '', avatarSeed: loadOrCreateAvatarSeed() })

  function shuffleSignupAvatar() {
    signupForm.avatarSeed = randomAvatarSeed()
    persistAvatarSeed(signupForm.avatarSeed)
  }

  function resetState() {
    errorMessage.value = ''
    passwordRules.value = []
    signupSuccess.value = false
    loading.value = false
  }

  function switchTab(next: 'login' | 'signup') {
    tab.value = next
    resetState()
  }

  function applyError(err: unknown) {
    const apiError = err as ApiError
    if (apiError?.code === 'WEAK_PASSWORD') {
      passwordRules.value = (apiError.args?.rules ?? '').split(',').filter(Boolean)
      errorMessage.value = ''
      return
    }
    passwordRules.value = []
    errorMessage.value = translateError(apiError)
  }

  async function onLogin() {
    loading.value = true
    errorMessage.value = ''
    try {
      await authStore.login({ username: loginForm.username, password: loginForm.password })
      onLoginSuccess()
    } catch (err) {
      applyError(err)
    } finally {
      loading.value = false
    }
  }

  async function onSignup() {
    loading.value = true
    errorMessage.value = ''
    passwordRules.value = []
    signupSuccess.value = false
    try {
      await authStore.signup({
        username: signupForm.username,
        email: signupForm.email,
        password: signupForm.password,
        avatarSeed: signupForm.avatarSeed,
      })
      signupSuccess.value = true
      // Signup does not auto-login: when email verification is enabled the
      // account isn't enabled yet, and when it's disabled the visitor still
      // has to consciously log in with the credentials they just chose.
      tab.value = 'login'
      loginForm.username = signupForm.username
    } catch (err) {
      applyError(err)
    } finally {
      loading.value = false
    }
  }

  return {
    tab,
    loading,
    errorMessage,
    passwordRules,
    signupSuccess,
    loginForm,
    signupForm,
    shuffleSignupAvatar,
    switchTab,
    onLogin,
    onSignup,
  }
}
