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
router.beforeEach((to, from, next) => {
    const authStore = useAuthStore()

    // Se la rotta richiede autenticazione e l'utente non è loggato, vai al login
    if (to.meta.requiresAuth && !authStore.token) {
        next('/login')
    } else {
        next()
    }
})

export default router