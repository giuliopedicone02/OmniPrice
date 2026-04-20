import { createRouter, createWebHistory } from 'vue-router'
import LoginPage from '../components/pages/LoginPage.vue'
import { useAuthStore } from '../store/auth'

const routes = [
    {
        path: '/login',
        name: 'Login',
        component: LoginPage
    },
    {
        path: '/',
        name: 'Home',
        // Per ora mandiamo alla ricerca, la implementeremo nella Fase 2
        component: () => import('../components/pages/SearchPage.vue'),
        meta: { requiresAuth: true }
    }
]

const router = createRouter({
    history: createWebHistory(),
    routes
})

// Protezione delle rotte (Middleware)
// Protezione delle rotte (Middleware) - Versione Moderna!
router.beforeEach((to, from) => {
    const authStore = useAuthStore()

    // Se la rotta richiede autenticazione e l'utente non è loggato, vai al login
    if (to.meta.requiresAuth && !authStore.token) {
        return '/login' // Invece di usare next('/login')
    }

    // Se non ritorniamo nulla (o ritorniamo true), la navigazione procede normale
    return true
})

export default router

