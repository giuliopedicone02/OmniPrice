<template>
  <div class="min-h-screen flex">

    <!-- Pannello sinistro: branding -->
    <div class="hidden lg:flex lg:w-1/2 bg-gradient-to-br from-violet-700 via-indigo-700 to-indigo-800 flex-col justify-between p-12 relative overflow-hidden">
      <!-- Cerchi decorativi sfondo -->
      <div class="absolute -top-24 -left-24 w-96 h-96 bg-white/5 rounded-full"></div>
      <div class="absolute -bottom-32 -right-16 w-[500px] h-[500px] bg-white/5 rounded-full"></div>
      <div class="absolute top-1/2 left-1/2 -translate-x-1/2 -translate-y-1/2 w-64 h-64 bg-white/5 rounded-full"></div>

      <!-- Logo -->
      <div class="flex items-center gap-3 relative">
        <div class="w-10 h-10 bg-white/20 rounded-xl flex items-center justify-center backdrop-blur-sm">
          <svg class="w-6 h-6 text-white" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2">
            <path stroke-linecap="round" stroke-linejoin="round" d="M7 7h.01M7 3h5c.512 0 1.024.195 1.414.586l7 7a2 2 0 010 2.828l-7 7a2 2 0 01-2.828 0l-7-7A1.994 1.994 0 013 12V7a4 4 0 014-4z" />
          </svg>
        </div>
        <span class="text-white font-bold text-2xl tracking-tight">OmniPrice</span>
      </div>

      <!-- Pitch centrale -->
      <div class="relative">
        <h1 class="text-4xl font-bold text-white leading-tight mb-4">
          Confronta i prezzi su tutti gli store in un click
        </h1>
        <p class="text-indigo-200 text-lg leading-relaxed mb-8">
          4 store monitorati in tempo reale. Alert automatici quando il prezzo scende. Storico completo per ogni prodotto.
        </p>

        <!-- Feature list -->
        <div class="space-y-3">
          <div v-for="feature in features" :key="feature" class="flex items-center gap-3 text-indigo-100">
            <div class="w-5 h-5 rounded-full bg-green-400/30 flex items-center justify-center flex-shrink-0">
              <svg class="w-3 h-3 text-green-300" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="3">
                <path stroke-linecap="round" stroke-linejoin="round" d="M5 13l4 4L19 7" />
              </svg>
            </div>
            <span class="text-sm">{{ feature }}</span>
          </div>
        </div>
      </div>

      <!-- Footer branding -->
      <div class="text-indigo-300 text-sm relative">
        Sistema distribuito · Spring Boot + Vue 3
      </div>
    </div>

    <!-- Pannello destro: form -->
    <div class="flex-1 flex flex-col justify-center items-center p-8 bg-white">
      <div class="w-full max-w-sm">

        <!-- Logo mobile -->
        <div class="flex items-center gap-2 mb-8 lg:hidden">
          <div class="w-8 h-8 rounded-lg bg-gradient-to-br from-violet-600 to-indigo-600 flex items-center justify-center">
            <svg class="w-5 h-5 text-white" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2">
              <path stroke-linecap="round" stroke-linejoin="round" d="M7 7h.01M7 3h5c.512 0 1.024.195 1.414.586l7 7a2 2 0 010 2.828l-7 7a2 2 0 01-2.828 0l-7-7A1.994 1.994 0 013 12V7a4 4 0 014-4z" />
            </svg>
          </div>
          <span class="font-bold text-slate-900 text-xl">OmniPrice</span>
        </div>

        <h2 class="text-2xl font-bold text-slate-900 mb-1">Bentornato</h2>
        <p class="text-slate-500 text-sm mb-8">Accedi al tuo account per continuare.</p>

        <form @submit.prevent="handleLogin" class="space-y-5">

          <div>
            <label class="block text-sm font-semibold text-slate-700 mb-1.5">Email</label>
            <input
              v-model="email"
              type="email"
              required
              autocomplete="email"
              placeholder="mario@example.com"
              class="w-full px-4 py-3 border border-slate-200 rounded-xl text-sm text-slate-900 placeholder-slate-400 focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:border-transparent transition-all bg-slate-50 focus:bg-white"
            >
          </div>

          <div>
            <label class="block text-sm font-semibold text-slate-700 mb-1.5">Password</label>
            <div class="relative">
              <input
                v-model="password"
                :type="showPassword ? 'text' : 'password'"
                required
                autocomplete="current-password"
                placeholder="••••••••"
                class="w-full px-4 py-3 border border-slate-200 rounded-xl text-sm text-slate-900 placeholder-slate-400 focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:border-transparent transition-all bg-slate-50 focus:bg-white pr-12"
              >
              <button
                type="button"
                @click="showPassword = !showPassword"
                class="absolute right-3 top-1/2 -translate-y-1/2 text-slate-400 hover:text-slate-600 transition-colors"
              >
                <svg v-if="!showPassword" class="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" />
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M2.458 12C3.732 7.943 7.523 5 12 5c4.478 0 8.268 2.943 9.542 7-1.274 4.057-5.064 7-9.542 7-4.477 0-8.268-2.943-9.542-7z" />
                </svg>
                <svg v-else class="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13.875 18.825A10.05 10.05 0 0112 19c-4.478 0-8.268-2.943-9.543-7a9.97 9.97 0 011.563-3.029m5.858.908a3 3 0 114.243 4.243M9.878 9.878l4.242 4.242M9.88 9.88l-3.29-3.29m7.532 7.532l3.29 3.29M3 3l3.59 3.59m0 0A9.953 9.953 0 0112 5c4.478 0 8.268 2.943 9.543 7a10.025 10.025 0 01-4.132 5.411m0 0L21 21" />
                </svg>
              </button>
            </div>
          </div>

          <!-- Credenziali demo -->
          <div class="bg-indigo-50 border border-indigo-100 rounded-xl p-3">
            <p class="text-xs font-semibold text-indigo-700 mb-1.5">Account demo precompilato</p>
            <div class="space-y-0.5 text-xs text-indigo-600">
              <p><span class="font-medium">Email:</span> mario@example.com</p>
              <p><span class="font-medium">Password:</span> PasswordSuperSicura123!</p>
            </div>
          </div>

          <!-- Errore -->
          <div v-if="errorMessage" class="flex items-center gap-2 bg-red-50 border border-red-200 rounded-xl px-4 py-3">
            <svg class="w-4 h-4 text-red-500 flex-shrink-0" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 8v4m0 4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
            </svg>
            <p class="text-sm text-red-700">{{ errorMessage }}</p>
          </div>

          <button
            type="submit"
            :disabled="isLoading"
            class="w-full py-3 px-4 bg-gradient-to-r from-violet-600 to-indigo-600 hover:from-violet-700 hover:to-indigo-700 text-white text-sm font-semibold rounded-xl shadow-sm shadow-indigo-200 focus:outline-none disabled:opacity-60 transition-all flex items-center justify-center gap-2"
          >
            <svg v-if="isLoading" class="animate-spin w-4 h-4" fill="none" viewBox="0 0 24 24">
              <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"/>
              <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z"/>
            </svg>
            {{ isLoading ? 'Accesso in corso...' : 'Accedi' }}
          </button>

        </form>

        <p class="mt-6 text-center text-xs text-slate-400">
          Non hai un account?
          <span class="text-indigo-600 font-medium cursor-pointer hover:underline" @click="showRegister = true">Registrati</span>
        </p>

      </div>
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
const showPassword = ref(false)

const features = [
  'Confronto prezzi su 4 store in parallelo',
  'Alert automatici al calo di prezzo',
  'Storico prezzi con grafici interattivi',
  'Architettura distribuita con resilienza',
]

const authStore = useAuthStore()
const router = useRouter()

const handleLogin = async () => {
  isLoading.value = true
  errorMessage.value = ''
  try {
    const success = await authStore.login(email.value, password.value)
    if (success) {
      router.push('/')
    } else {
      errorMessage.value = 'Credenziali non valide. Controlla email e password.'
    }
  } catch {
    errorMessage.value = 'Errore di rete. Il backend è avviato?'
  } finally {
    isLoading.value = false
  }
}
</script>
