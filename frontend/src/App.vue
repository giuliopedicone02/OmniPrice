<template>
  <div class="min-h-screen bg-gray-100">
    <nav v-if="authStore.token" class="bg-indigo-600 p-4 text-white flex justify-between">
      <h1 class="font-bold">OmniPrice</h1>
      <button @click="handleLogout" class="text-sm bg-indigo-700 px-3 py-1 rounded">
        Logout
      </button>
    </nav>

    <router-view />
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