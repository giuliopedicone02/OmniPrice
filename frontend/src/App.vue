<template>
  <div class="min-h-screen bg-slate-50">

    <!-- Navbar -->
    <header v-if="authStore.token" class="bg-white border-b border-slate-200 sticky top-0 z-50 shadow-sm">
      <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div class="flex items-center justify-between h-16">

          <!-- Logo + Nav -->
          <div class="flex items-center gap-8">
            <router-link to="/" class="flex items-center gap-2 group">
              <div class="w-8 h-8 rounded-lg bg-gradient-to-br from-violet-600 to-indigo-600 flex items-center justify-center shadow-sm">
                <svg class="w-5 h-5 text-white" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2">
                  <path stroke-linecap="round" stroke-linejoin="round" d="M7 7h.01M7 3h5c.512 0 1.024.195 1.414.586l7 7a2 2 0 010 2.828l-7 7a2 2 0 01-2.828 0l-7-7A1.994 1.994 0 013 12V7a4 4 0 014-4z" />
                </svg>
              </div>
              <span class="font-bold text-slate-900 text-lg tracking-tight group-hover:text-indigo-600 transition-colors">OmniPrice</span>
            </router-link>

            <nav class="hidden sm:flex items-center gap-1">
              <router-link
                to="/"
                class="px-3 py-2 rounded-lg text-sm font-medium text-slate-600 hover:text-indigo-600 hover:bg-indigo-50 transition-all"
                :class="{ 'text-indigo-600 bg-indigo-50': $route.path === '/' }"
              >
                Ricerca
              </router-link>
              <router-link
                to="/alerts"
                class="px-3 py-2 rounded-lg text-sm font-medium text-slate-600 hover:text-indigo-600 hover:bg-indigo-50 transition-all flex items-center gap-1.5"
                :class="{ 'text-indigo-600 bg-indigo-50': $route.path === '/alerts' }"
              >
                Alert
                <span v-if="alertsCount > 0" class="bg-indigo-600 text-white text-xs font-bold rounded-full w-4 h-4 flex items-center justify-center">
                  {{ alertsCount }}
                </span>
              </router-link>
            </nav>
          </div>

          <!-- User menu -->
          <div class="flex items-center gap-3">
            <div class="hidden sm:flex items-center gap-2">
              <div class="w-8 h-8 rounded-full bg-gradient-to-br from-violet-400 to-indigo-500 flex items-center justify-center text-white text-xs font-bold">
                {{ userInitials }}
              </div>
              <span class="text-sm text-slate-700 font-medium">{{ authStore.user?.name }}</span>
            </div>
            <button
              @click="handleLogout"
              class="flex items-center gap-1.5 text-sm text-slate-500 hover:text-red-500 hover:bg-red-50 px-3 py-2 rounded-lg transition-all font-medium"
            >
              <svg class="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2">
                <path stroke-linecap="round" stroke-linejoin="round" d="M17 16l4-4m0 0l-4-4m4 4H7m6 4v1a3 3 0 01-3 3H6a3 3 0 01-3-3V7a3 3 0 013-3h4a3 3 0 013 3v1" />
              </svg>
              Esci
            </button>
          </div>

        </div>
      </div>
    </header>

    <main>
      <router-view v-slot="{ Component }">
        <transition name="fade" mode="out-in">
          <component :is="Component" />
        </transition>
      </router-view>
    </main>

  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useAuthStore } from './store/auth'
import { useAlertsStore } from './store/alerts'
import { useRouter } from 'vue-router'

const authStore = useAuthStore()
const alertsStore = useAlertsStore()
const router = useRouter()

const userInitials = computed(() => {
  const name = authStore.user?.name || ''
  return name.split(' ').map(n => n[0]).join('').toUpperCase().slice(0, 2)
})

const alertsCount = computed(() => alertsStore.activeAlerts.length)

const handleLogout = () => {
  authStore.logout()
  router.push('/login')
}
</script>

<style>
.fade-enter-active, .fade-leave-active { transition: opacity 0.15s ease; }
.fade-enter-from, .fade-leave-to { opacity: 0; }
</style>
