import axios from 'axios'
import { AUTH_TOKEN_STORAGE_KEY } from '@/stores/auth'

const apiClient = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL,
  headers: {
    'Content-Type': 'application/json',
  },
})

// Reads the token straight from localStorage rather than importing
// useAuthStore() here: the store is a Pinia store, which needs
// app.use(createPinia()) to have run first, while apiClient is a plain
// module that other services import at load time - going through the
// store would risk a "no active Pinia" error depending on import order.
apiClient.interceptors.request.use((config) => {
  const token = localStorage.getItem(AUTH_TOKEN_STORAGE_KEY)
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

export default apiClient
