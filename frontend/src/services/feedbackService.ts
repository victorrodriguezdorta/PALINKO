import apiClient from './apiClient'
import { toApiError } from './apiError'

export interface CreatorLetterFeedbackPayload {
  suggestion: string
  issues: string
}

const CREATOR_LETTER_PATH = '/feedback/creator-letter'

export async function getCreatorLetterVisibility(): Promise<boolean> {
  try {
    const response = await apiClient.get<{ visible: boolean }>(CREATOR_LETTER_PATH)
    return response.data.visible
  } catch (error) {
    throw toApiError(error)
  }
}

export async function submitCreatorLetterFeedback(payload: CreatorLetterFeedbackPayload): Promise<void> {
  try {
    await apiClient.post(CREATOR_LETTER_PATH, payload)
  } catch (error) {
    throw toApiError(error)
  }
}

export async function dismissCreatorLetter(): Promise<void> {
  try {
    await apiClient.post(`${CREATOR_LETTER_PATH}/dismiss`)
  } catch (error) {
    throw toApiError(error)
  }
}
