<template>
  <div class="min-h-screen bg-slate-50">

    <!-- Navbar desktop -->
    <header v-if="authStore.token" class="bg-white border-b border-slate-200 sticky top-0 z-50 shadow-sm">
      <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div class="flex items-center justify-between h-16">

          <!-- Logo + Nav -->
          <div class="flex items-center gap-8">
            <router-link to="/" class="flex items-center gap-2 group">
              <div class="w-8 h-8 rounded-lg bg-gradient-to-br from-teal-600 to-emerald-600 flex items-center justify-center shadow-sm">
                <svg class="w-5 h-5 text-white" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2">
                  <path stroke-linecap="round" stroke-linejoin="round" d="M7 7h.01M7 3h5c.512 0 1.024.195 1.414.586l7 7a2 2 0 010 2.828l-7 7a2 2 0 01-2.828 0l-7-7A1.994 1.994 0 013 12V7a4 4 0 014-4z" />
                </svg>
              </div>
              <span class="font-bold text-slate-900 text-lg tracking-tight group-hover:text-emerald-600 transition-colors">OmniPrice</span>
            </router-link>

            <nav class="hidden sm:flex items-center gap-1">
              <router-link
                to="/"
                class="px-3 py-2 rounded-lg text-sm font-medium text-slate-600 hover:text-emerald-600 hover:bg-emerald-50 transition-all flex items-center gap-1.5"
                :class="{ 'text-emerald-600 bg-emerald-50': $route.path === '/' }"
              >
                <svg class="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2">
                  <path stroke-linecap="round" stroke-linejoin="round" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
                </svg>
                Ricerca
              </router-link>
              <router-link
                to="/alerts"
                class="px-3 py-2 rounded-lg text-sm font-medium text-slate-600 hover:text-emerald-600 hover:bg-emerald-50 transition-all flex items-center gap-1.5"
                :class="{ 'text-emerald-600 bg-emerald-50': $route.path === '/alerts' }"
              >
                <svg class="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2">
                  <path stroke-linecap="round" stroke-linejoin="round" d="M15 17h5l-1.405-1.405A2.032 2.032 0 0118 14.158V11a6.002 6.002 0 00-4-5.659V5a2 2 0 10-4 0v.341C7.67 6.165 6 8.388 6 11v3.159c0 .538-.214 1.055-.595 1.436L4 17h5m6 0v1a3 3 0 11-6 0v-1m6 0H9" />
                </svg>
                Alert
                <span v-if="alertsCount > 0" class="bg-emerald-600 text-white text-xs font-bold rounded-full w-4 h-4 flex items-center justify-center leading-none">
                  {{ alertsCount }}
                </span>
              </router-link>
              <router-link
                v-if="userRole === 'ADMIN'"
                to="/admin"
                class="px-3 py-2 rounded-lg text-sm font-medium text-slate-600 hover:text-indigo-600 hover:bg-indigo-50 transition-all flex items-center gap-1.5"
                :class="{ 'text-indigo-600 bg-indigo-50 font-bold': $route.path === '/admin' }"
              >
                <svg class="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2">
                  <path stroke-linecap="round" stroke-linejoin="round" d="M9 12l2 2 4-4m5.618-4.016A11.955 11.955 0 0112 2.944a11.955 11.955 0 01-8.618 3.04A12.02 12.02 0 003 9c0 5.591 3.824 10.29 9 11.622 5.176-1.332 9-6.03 9-11.622 0-1.042-.133-2.052-.382-3.016z" />
                </svg>
                Cluster & Admin
              </router-link>
            </nav>
          </div>

          <!-- User menu -->
          <div class="flex items-center gap-3">
            <div class="hidden sm:flex items-center gap-2">
              <!-- Role badge -->
              <span v-if="userRole === 'PREMIUM'" class="text-xs font-bold bg-amber-100 text-amber-700 border border-amber-200 px-2 py-0.5 rounded-full">PRO</span>
              <span v-else-if="userRole === 'ADMIN'" class="text-xs font-bold bg-red-100 text-red-700 border border-red-200 px-2 py-0.5 rounded-full">ADMIN</span>
              <div class="w-8 h-8 rounded-full bg-gradient-to-br from-teal-400 to-emerald-500 flex items-center justify-center text-white text-xs font-bold">
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
              <span class="hidden sm:inline">Esci</span>
            </button>
          </div>

        </div>
      </div>
    </header>

    <!-- Main content (padding bottom on mobile for bottom nav) -->
    <main :class="authStore.token ? 'pb-16 sm:pb-0' : ''">
      <router-view v-slot="{ Component }">
        <transition name="fade" mode="out-in">
          <component :is="Component" />
        </transition>
      </router-view>
    </main>

    <!-- Mobile bottom navigation -->
    <nav v-if="authStore.token" class="fixed bottom-0 left-0 right-0 bg-white border-t border-slate-200 flex sm:hidden z-50 shadow-lg">
      <router-link
        to="/"
        class="flex-1 flex flex-col items-center py-2.5 gap-1 text-xs font-medium transition-colors"
        :class="$route.path === '/' ? 'text-emerald-600' : 'text-slate-500'"
      >
        <svg class="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2">
          <path stroke-linecap="round" stroke-linejoin="round" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
        </svg>
        Ricerca
      </router-link>
      <router-link
        to="/alerts"
        class="flex-1 flex flex-col items-center py-2.5 gap-1 text-xs font-medium transition-colors relative"
        :class="$route.path === '/alerts' ? 'text-emerald-600' : 'text-slate-500'"
      >
        <svg class="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2">
          <path stroke-linecap="round" stroke-linejoin="round" d="M15 17h5l-1.405-1.405A2.032 2.032 0 0118 14.158V11a6.002 6.002 0 00-4-5.659V5a2 2 0 10-4 0v.341C7.67 6.165 6 8.388 6 11v3.159c0 .538-.214 1.055-.595 1.436L4 17h5m6 0v1a3 3 0 11-6 0v-1m6 0H9" />
        </svg>
        Alert
        <span v-if="alertsCount > 0" class="absolute top-1.5 right-1/4 bg-emerald-600 text-white text-xs font-bold rounded-full w-4 h-4 flex items-center justify-center leading-none">
          {{ alertsCount }}
        </span>
      </router-link>
      <router-link
        v-if="userRole === 'ADMIN'"
        to="/admin"
        class="flex-1 flex flex-col items-center py-2.5 gap-1 text-xs font-medium transition-colors"
        :class="$route.path === '/admin' ? 'text-indigo-600 font-bold' : 'text-slate-500'"
      >
        <svg class="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2">
          <path stroke-linecap="round" stroke-linejoin="round" d="M9 12l2 2 4-4m5.618-4.016A11.955 11.955 0 0112 2.944a11.955 11.955 0 01-8.618 3.04A12.02 12.02 0 003 9c0 5.591 3.824 10.29 9 11.622 5.176-1.332 9-6.03 9-11.622 0-1.042-.133-2.052-.382-3.016z" />
        </svg>
        Admin
      </router-link>
      <button
        @click="handleLogout"
        class="flex-1 flex flex-col items-center py-2.5 gap-1 text-xs font-medium text-slate-500 hover:text-red-500 transition-colors"
      >
        <svg class="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2">
          <path stroke-linecap="round" stroke-linejoin="round" d="M17 16l4-4m0 0l-4-4m4 4H7m6 4v1a3 3 0 01-3 3H6a3 3 0 01-3-3V7a3 3 0 013-3h4a3 3 0 013 3v1" />
        </svg>
        Esci
      </button>
    </nav>

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

const userRole = computed(() => authStore.user?.role || 'STANDARD')

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
