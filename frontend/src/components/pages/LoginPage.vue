<template>
  <div class="min-h-screen flex items-center justify-center bg-gray-50">
    <div class="max-w-md w-full bg-white p-8 rounded-lg shadow-md">
      <h2 class="text-2xl font-bold text-center text-gray-800 mb-6">Accedi a OmniPrice</h2>
      
      <form @submit.prevent="handleLogin" class="space-y-4">
        <div>
          <label class="block text-sm font-medium text-gray-700">Email</label>
          <input 
            v-model="email" 
            type="email" 
            required
            class="mt-1 block w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-indigo-500 focus:border-indigo-500"
          >
        </div>
        
        <div>
          <label class="block text-sm font-medium text-gray-700">Password</label>
          <input 
            v-model="password" 
            type="password" 
            required
            class="mt-1 block w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-indigo-500 focus:border-indigo-500"
          >
        </div>

        <div v-if="errorMessage" class="text-red-500 text-sm text-center">
          {{ errorMessage }}
        </div>

        <button 
          type="submit" 
          :disabled="isLoading"
          class="w-full flex justify-center py-2 px-4 border border-transparent rounded-md shadow-sm text-sm font-medium text-white bg-indigo-600 hover:bg-indigo-700 focus:outline-none"
        >
          {{ isLoading ? 'Accesso in corso...' : 'Accedi' }}
        </button>
      </form>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useAuthStore } from '../../store/auth'
import { useRouter } from 'vue-router'

const email = ref('mario@example.com')
const password = ref('PasswordSuperSicura123!')
const isLoading = ref(false)
const errorMessage = ref('')

const authStore = useAuthStore()
const router = useRouter()

const handleLogin = async () => {
  isLoading.value = true
  errorMessage.value = ''
  
  const success = await authStore.login(email.value, password.value)
  
  if (success) {
    alert("Login effettuato con successo! Guarda la console.")
    console.log("Token ricevuto:", authStore.token)
    // router.push('/') // Decommenteremo questo quando avremo la Home
  } else {
    errorMessage.value = "Credenziali non valide o server non raggiungibile."
  }
  
  isLoading.value = false
}
</script>