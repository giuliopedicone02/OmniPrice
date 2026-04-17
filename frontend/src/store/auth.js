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
                // Chiamiamo il nostro backend!
                const response = await api.post('/login', { email, password })

                // Salviamo i dati nello state
                this.token = response.data.token
                this.user = response.data.user

                // Salviamo nel localStorage per farli sopravvivere al refresh
                localStorage.setItem('token', this.token)
                localStorage.setItem('user', JSON.stringify(this.user))

                return true
            } catch (error) {
                console.error("Errore di login:", error)
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