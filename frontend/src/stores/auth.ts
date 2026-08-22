import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import * as authService from '@/services/authService'
import type { AuthUser, ChangePasswordPayload, LoginPayload, SignupPayload, UpdateProfilePayload } from '@/types/user'

export const AUTH_TOKEN_STORAGE_KEY = 'palinko:auth-token'

// localStorage, not sessionStorage: unlike a game room's ephemeral identity
// (see stores/game.ts), a logged-in account is a cross-tab, cross-session
// concept - opening a new tab should see the same signed-in user.
function loadToken(): string | null {
  return localStorage.getItem(AUTH_TOKEN_STORAGE_KEY)
}

export const useAuthStore = defineStore('auth', () => {
  const token = ref<string | null>(loadToken())
  const user = ref<AuthUser | null>(null)
  // Tracked so the router guard (see router/index.ts) can await the
  // in-flight rehydration from main.ts instead of racing it on first load.
  let initializePromise: Promise<void> | null = null

  const isAuthenticated = computed(() => !!token.value && !!user.value)

  function persistToken(newToken: string) {
    token.value = newToken
    localStorage.setItem(AUTH_TOKEN_STORAGE_KEY, newToken)
  }

  function clear() {
    token.value = null
    user.value = null
    localStorage.removeItem(AUTH_TOKEN_STORAGE_KEY)
  }

  async function login(payload: LoginPayload): Promise<void> {
    const result = await authService.login(payload)
    persistToken(result.token)
    user.value = result.user
  }

  async function signup(payload: SignupPayload): Promise<void> {
    await authService.signup(payload)
  }

  function logout(): void {
    clear()
  }

  async function fetchCurrentUser(): Promise<void> {
    user.value = await authService.getCurrentUser()
  }

  // Always re-persists the token: the JWT subject is the username, so a
  // username change invalidates the token the store already holds (see
  // backend UpdateProfileResult).
  async function updateProfile(payload: UpdateProfilePayload): Promise<void> {
    const result = await authService.updateProfile(payload)
    persistToken(result.token)
    user.value = result.user
  }

  async function changePassword(payload: ChangePasswordPayload): Promise<void> {
    await authService.changePassword(payload)
  }

  // Re-hydrates the session from a stored token on app startup. If the
  // token turns out to be missing/expired/invalid, fetchCurrentUser()
  // throws (401) and the stale token is silently cleared rather than
  // surfaced as an error the visitor never asked to see. Idempotent and
  // safe to call multiple times - later callers just await the same
  // in-flight (or already-settled) promise instead of re-running it.
  function initialize(): Promise<void> {
    if (!initializePromise) {
      initializePromise = token.value
        ? fetchCurrentUser().catch(() => clear())
        : Promise.resolve()
    }
    return initializePromise
  }

  return {
    token,
    user,
    isAuthenticated,
    login,
    signup,
    logout,
    fetchCurrentUser,
    updateProfile,
    changePassword,
    initialize,
  }
})
