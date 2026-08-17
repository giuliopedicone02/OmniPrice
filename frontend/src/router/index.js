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
        component: () => import('../components/pages/SearchPage.vue'),
        meta: { requiresAuth: true }
    },
    {
        path: '/product/:id',
        name: 'ProductDetail',
        component: () => import('../components/pages/ProductDetailPage.vue'),
        meta: { requiresAuth: true }
    },
    {
        path: '/alerts',
        name: 'Alerts',
        component: () => import('../components/pages/AlertPage.vue'),
        meta: { requiresAuth: true }
    },
    {
        path: '/admin',
        name: 'Admin',
        component: () => import('../components/pages/AdminPage.vue'),
        meta: { requiresAuth: true, requiresAdmin: true }
    }
]

const router = createRouter({
    history: createWebHistory(),
    routes
})

router.beforeEach((to) => {
    const authStore = useAuthStore()
    if (to.meta.requiresAuth && !authStore.token) {
        return '/login'
    }
    if (to.meta.requiresAdmin && authStore.user?.role !== 'ADMIN') {
        return '/'
    }
    return true
})

export default router
