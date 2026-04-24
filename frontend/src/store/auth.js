import { defineStore } from 'pinia'
import api from '../services/api'

export const useAuthStore = defineStore('auth', {
    state: () => ({
        user: JSON.parse(localStorage.getItem('user')) || null,
        token: localStorage.getItem('token') || null,
    }),
    actions: {
        async login(email, password) {
            try {
                const response = await api.post('/auth/login', { email, password })
                this.token = response.data.token
                this.user = response.data.user
                localStorage.setItem('token', this.token)
                localStorage.setItem('user', JSON.stringify(this.user))
                return true
            } catch (error) {
                console.error('Errore di login:', error)
                return false
            }
        },
        async register(name, email, password, role = 'STANDARD') {
            try {
                const response = await api.post('/auth/register', { name, email, password, role })
                this.token = response.data.token
                this.user = response.data.user
                localStorage.setItem('token', this.token)
                localStorage.setItem('user', JSON.stringify(this.user))
                return true
            } catch (error) {
                console.error('Errore di registrazione:', error)
                return false
            }
        },
        logout() {
            this.user = null
            this.token = null
            localStorage.removeItem('token')
            localStorage.removeItem('user')
        }
    }
})
