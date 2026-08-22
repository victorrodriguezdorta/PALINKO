import apiClient from './apiClient'
import { toApiError } from './apiError'
import type {
  AuthUser,
  ChangePasswordPayload,
  LoginPayload,
  LoginResponse,
  RequestPasswordResetPayload,
  ResendVerificationPayload,
  ResetPasswordPayload,
  SignupPayload,
  UpdateProfilePayload,
  UpdateProfileResponse,
  VerifyAccountPayload,
} from '@/types/user'

export async function signup(payload: SignupPayload): Promise<void> {
  try {
    await apiClient.post('/auth/signup', payload)
  } catch (error) {
    throw toApiError(error)
  }
}

export async function login(payload: LoginPayload): Promise<LoginResponse> {
  try {
    const response = await apiClient.post<LoginResponse>('/auth/login', payload)
    return response.data
  } catch (error) {
    throw toApiError(error)
  }
}

export async function verifyAccount(payload: VerifyAccountPayload): Promise<void> {
  try {
    await apiClient.post('/auth/verify-account', payload)
  } catch (error) {
    throw toApiError(error)
  }
}

export async function resendVerification(payload: ResendVerificationPayload): Promise<void> {
  try {
    await apiClient.post('/auth/resend-verification', payload)
  } catch (error) {
    throw toApiError(error)
  }
}

export async function requestPasswordReset(payload: RequestPasswordResetPayload): Promise<void> {
  try {
    await apiClient.post('/password/request-reset', payload)
  } catch (error) {
    throw toApiError(error)
  }
}

export async function resetPassword(payload: ResetPasswordPayload): Promise<void> {
  try {
    await apiClient.post('/password/reset', payload)
  } catch (error) {
    throw toApiError(error)
  }
}

export async function getCurrentUser(): Promise<AuthUser> {
  try {
    const response = await apiClient.get<AuthUser>('/users/me')
    return response.data
  } catch (error) {
    throw toApiError(error)
  }
}

export async function updateProfile(payload: UpdateProfilePayload): Promise<UpdateProfileResponse> {
  try {
    const response = await apiClient.patch<UpdateProfileResponse>('/users/me', payload)
    return response.data
  } catch (error) {
    throw toApiError(error)
  }
}

export async function changePassword(payload: ChangePasswordPayload): Promise<void> {
  try {
    await apiClient.put('/users/me/password', payload)
  } catch (error) {
    throw toApiError(error)
  }
}
