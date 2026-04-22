<template>
  <div>

    <!-- Header banner -->
    <div class="bg-gradient-to-br from-violet-700 via-indigo-700 to-indigo-800 py-10 px-4">
      <div class="max-w-4xl mx-auto">
        <div class="flex items-center justify-between">
          <div>
            <h1 class="text-2xl font-bold text-white mb-1">I miei Alert Prezzi</h1>
            <p class="text-indigo-200 text-sm">Ricevi una notifica quando il prezzo scende sotto la soglia</p>
          </div>
          <div class="flex items-center gap-3">
            <div class="text-center bg-white/10 border border-white/20 rounded-2xl px-4 py-2.5">
              <p class="text-2xl font-bold text-white">{{ activeAlerts.length }}</p>
              <p class="text-xs text-indigo-200">attivi</p>
            </div>
            <div class="text-center bg-white/10 border border-white/20 rounded-2xl px-4 py-2.5">
              <p class="text-2xl font-bold text-emerald-300">{{ triggeredAlerts.length }}</p>
              <p class="text-xs text-indigo-200">scattati</p>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- Content -->
    <div class="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8 py-8">

      <!-- Loading skeleton -->
      <div v-if="loading" class="space-y-3">
        <div v-for="n in 3" :key="n" class="bg-white rounded-2xl border border-slate-100 p-5 animate-pulse">
          <div class="flex justify-between items-center">
            <div class="flex-1">
              <div class="h-4 bg-slate-100 rounded w-48 mb-2"></div>
              <div class="h-3 bg-slate-100 rounded w-64"></div>
            </div>
            <div class="h-8 bg-slate-100 rounded-xl w-20"></div>
          </div>
        </div>
      </div>

      <div v-else>

        <!-- Alert scattati -->
        <div v-if="triggeredAlerts.length > 0" class="mb-8">
          <div class="flex items-center gap-2 mb-4">
            <div class="w-2 h-2 rounded-full bg-emerald-400"></div>
            <h2 class="text-xs font-bold text-emerald-700 uppercase tracking-widest">Alert Scattati</h2>
          </div>
          <div class="space-y-3">
            <div
              v-for="alert in triggeredAlerts"
              :key="alert.id"
              class="bg-white rounded-2xl border border-emerald-100 shadow-sm overflow-hidden"
            >
              <div class="flex items-stretch">
                <!-- Stripe laterale -->
                <div class="w-1 bg-emerald-400 flex-shrink-0"></div>
                <div class="flex-1 p-5">
                  <div class="flex items-start justify-between gap-4">
                    <div class="flex-1">
                      <div class="flex items-center gap-2 mb-1">
                        <span class="inline-flex items-center gap-1 bg-emerald-50 text-emerald-700 text-xs font-bold px-2.5 py-0.5 rounded-full border border-emerald-100">
                          <svg class="w-3 h-3" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2.5">
                            <path stroke-linecap="round" stroke-linejoin="round" d="M5 13l4 4L19 7" />
                          </svg>
                          SCATTATO
                        </span>
                      </div>
                      <p class="font-bold text-slate-900">{{ alert.productName }}</p>
                      <div class="flex flex-wrap items-center gap-x-4 gap-y-1 mt-2">
                        <p class="text-sm text-slate-500">
                          Target:
                          <span class="font-semibold text-slate-700">{{ alert.targetPrice?.toFixed(2) }}€</span>
                        </p>
                        <p class="text-sm text-emerald-700 font-medium">
                          <svg class="w-3.5 h-3.5 inline mr-0.5 -mt-0.5" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2">
                            <path stroke-linecap="round" stroke-linejoin="round" d="M19 9l-7 7-7-7" />
                          </svg>
                          {{ alert.triggeredPrice?.toFixed(2) }}€ su {{ alert.triggeredStore }}
                        </p>
                      </div>
                      <p class="text-xs text-slate-400 mt-1.5">{{ formatDate(alert.triggeredAt) }}</p>
                    </div>
                    <div class="text-right flex-shrink-0">
                      <p class="text-2xl font-bold text-emerald-600">{{ alert.triggeredPrice?.toFixed(2) }}€</p>
                      <p class="text-xs text-slate-400">prezzo raggiunto</p>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- Alert attivi -->
        <div v-if="activeAlerts.length > 0" class="mb-8">
          <div class="flex items-center gap-2 mb-4">
            <div class="w-2 h-2 rounded-full bg-indigo-400"></div>
            <h2 class="text-xs font-bold text-indigo-700 uppercase tracking-widest">Alert Attivi</h2>
          </div>
          <div class="space-y-3">
            <div
              v-for="alert in activeAlerts"
              :key="alert.id"
              class="bg-white rounded-2xl border border-slate-100 shadow-sm hover:shadow-md hover:-translate-y-0.5 transition-all duration-200 overflow-hidden"
            >
              <div class="flex items-stretch">
                <div class="w-1 bg-indigo-400 flex-shrink-0"></div>
                <div class="flex-1 p-5">
                  <div class="flex items-center justify-between gap-4">
                    <div class="flex-1 min-w-0">
                      <p class="font-bold text-slate-900 truncate">{{ alert.productName }}</p>
                      <p class="text-sm text-slate-500 mt-1">
                        Avvisami sotto
                        <span class="font-bold text-indigo-600">{{ alert.targetPrice?.toFixed(2) }}€</span>
                      </p>
                      <p class="text-xs text-slate-400 mt-1">Creato il {{ formatDate(alert.createdAt) }}</p>
                    </div>
                    <div class="flex items-center gap-3 flex-shrink-0">
                      <div class="text-right">
                        <p class="text-xl font-bold text-indigo-600">{{ alert.targetPrice?.toFixed(2) }}€</p>
                        <p class="text-xs text-slate-400">soglia</p>
                      </div>
                      <button
                        @click="handleCancel(alert.id)"
                        :disabled="cancellingId === alert.id"
                        class="flex items-center gap-1.5 text-sm font-medium text-slate-400 hover:text-red-500 hover:bg-red-50 px-3 py-2 rounded-xl transition-all disabled:opacity-50"
                      >
                        <svg v-if="cancellingId !== alert.id" class="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2">
                          <path stroke-linecap="round" stroke-linejoin="round" d="M6 18L18 6M6 6l12 12" />
                        </svg>
                        <svg v-else class="animate-spin w-4 h-4" fill="none" viewBox="0 0 24 24">
                          <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"/>
                          <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z"/>
                        </svg>
                        {{ cancellingId === alert.id ? 'Annullo...' : 'Annulla' }}
                      </button>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- Alert annullati -->
        <div v-if="cancelledAlerts.length > 0" class="mb-8">
          <div class="flex items-center gap-2 mb-4">
            <div class="w-2 h-2 rounded-full bg-slate-300"></div>
            <h2 class="text-xs font-bold text-slate-400 uppercase tracking-widest">Alert Annullati</h2>
          </div>
          <div class="space-y-2">
            <div
              v-for="alert in cancelledAlerts"
              :key="alert.id"
              class="bg-slate-50 rounded-2xl border border-slate-100 px-5 py-3.5 opacity-60"
            >
              <div class="flex items-center justify-between">
                <p class="font-medium text-slate-600 truncate">{{ alert.productName }}</p>
                <span class="text-sm text-slate-400 flex-shrink-0 ml-4">{{ alert.targetPrice?.toFixed(2) }}€ — Annullato</span>
              </div>
            </div>
          </div>
        </div>

        <!-- Nessun alert -->
        <div v-if="alerts.length === 0" class="text-center py-20 bg-white rounded-2xl border border-slate-100 shadow-sm">
          <div class="w-16 h-16 bg-indigo-50 rounded-2xl flex items-center justify-center mx-auto mb-4">
            <svg class="w-8 h-8 text-indigo-300" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M15 17h5l-1.405-1.405A2.032 2.032 0 0118 14.158V11a6.002 6.002 0 00-4-5.659V5a2 2 0 10-4 0v.341C7.67 6.165 6 8.388 6 11v3.159c0 .538-.214 1.055-.595 1.436L4 17h5m6 0v1a3 3 0 11-6 0v-1m6 0H9" />
            </svg>
          </div>
          <h3 class="text-lg font-semibold text-slate-700 mb-1">Nessun alert configurato</h3>
          <p class="text-slate-400 text-sm">
            <router-link to="/" class="text-indigo-600 font-medium hover:underline">Cerca un prodotto</router-link>
            e crea il tuo primo alert di prezzo
          </p>
        </div>

      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useAlertsStore } from '../../store/alerts'

const alertsStore = useAlertsStore()
const loading = ref(true)
const cancellingId = ref(null)

const alerts = computed(() => alertsStore.alerts)
const activeAlerts = computed(() => alertsStore.activeAlerts)
const triggeredAlerts = computed(() => alertsStore.triggeredAlerts)
const cancelledAlerts = computed(() => alerts.value.filter(a => a.status === 'CANCELLED'))

onMounted(async () => {
  await alertsStore.fetchAlerts()
  loading.value = false
})

const handleCancel = async (alertId) => {
  cancellingId.value = alertId
  try {
    await alertsStore.removeAlert(alertId)
  } catch (e) {
    console.error('Errore annullamento alert:', e)
  } finally {
    cancellingId.value = null
  }
}

const formatDate = (dateString) => {
  if (!dateString) return '-'
  return new Date(dateString).toLocaleDateString('it-IT', {
    day: '2-digit', month: 'short', year: 'numeric', hour: '2-digit', minute: '2-digit'
  })
}
</script>
