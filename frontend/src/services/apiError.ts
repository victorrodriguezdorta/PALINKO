import { isAxiosError } from 'axios'
import type { ApiError } from '@/types/game'

/**
 * Normalizes any REST failure into an ApiError so callers can translate it
 * the same way regardless of source: the backend's own {code, message,
 * args} body when present (see GameErrorMessage/UserErrorMessage), or a
 * generic client-side fallback for anything else (network failure,
 * unexpected 5xx, a body that isn't shaped like an ApiError).
 */
export function toApiError(error: unknown): ApiError {
  if (isAxiosError(error) && error.response?.data?.code) {
    const data = error.response.data as ApiError
    return { code: data.code, message: data.message, args: data.args ?? {} }
  }
  return { code: 'GENERIC', message: 'Request failed', args: {} }
}
