export type UserRole = 'ADMIN' | 'USER'

/**
 * Deliberately excludes email — no backend response ever includes it, even
 * for the account's own owner (see backend UserProfileResponse).
 */
export interface AuthUser {
  id: number
  username: string
  avatarSeed: string
  role: UserRole
  currentStreak: number
}

export interface LoginPayload {
  username: string
  password: string
}

export interface SignupPayload {
  username: string
  email: string
  password: string
  avatarSeed: string
}

export interface VerifyAccountPayload {
  email: string
  otp: string
}

export interface ResendVerificationPayload {
  email: string
}

export interface RequestPasswordResetPayload {
  email: string
}

export interface ResetPasswordPayload {
  email: string
  otp: string
  password: string
  confirmPassword: string
}

export interface LoginResponse {
  token: string
  user: AuthUser
}

/**
 * Either field may be omitted to mean "leave unchanged" — mirrors the
 * backend's UpdateProfileRequest.
 */
export interface UpdateProfilePayload {
  username?: string
  avatarSeed?: string
}

/**
 * Always carries a freshly issued token — the JWT subject is the username,
 * so a username change invalidates whatever token the caller was already
 * holding (see backend UpdateProfileResult).
 */
export interface UpdateProfileResponse {
  token: string
  user: AuthUser
}

export interface ChangePasswordPayload {
  currentPassword: string
  newPassword: string
  confirmNewPassword: string
}
