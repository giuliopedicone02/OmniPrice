<template>
  <div class="min-h-screen bg-gray-100">
    <nav v-if="authStore.token" class="bg-indigo-600 shadow-md">
      <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div class="flex items-center justify-between h-16">
          <div class="flex-shrink-0 flex items-center">
            <h1 class="text-white font-bold text-xl tracking-tight">OmniPrice</h1>
          </div>
          <div>
            <button 
              @click="handleLogout" 
              class="text-sm bg-indigo-500 hover:bg-indigo-400 text-white px-4 py-2 rounded-md font-medium transition-colors"
            >
              Esci
            </button>
          </div>
        </div>
      </div>
    </nav>

    <main>
      <router-view />
    </main>
  </div>
</template>

<script setup>
import { useAuthStore } from './store/auth'
import { useRouter } from 'vue-router'

const authStore = useAuthStore()
const router = useRouter()

const handleLogout = () => {
  authStore.logout()
  router.push('/login')
}
</script>