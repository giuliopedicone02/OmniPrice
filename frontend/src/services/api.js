import axios from 'axios'
import { useAuthStore } from '../store/auth'

// Aggiunto /api prima di /auth !
const API_BASE_URL = 'http://localhost:8080/api'

const api = axios.create({
    baseURL: API_BASE_URL,
    timeout: 10000
})

// Interceptor per aggiungere JWT header
api.interceptors.request.use((config) => {
    const authStore = useAuthStore()
    if (authStore.token) {
        config.headers.Authorization = `Bearer ${authStore.token}`
    }
    return config
})

export default api