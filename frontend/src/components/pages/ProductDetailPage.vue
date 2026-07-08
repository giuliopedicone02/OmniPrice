<template>
  <div class="max-w-5xl mx-auto px-4 sm:px-6 lg:px-8 py-8">

    <!-- Back -->
    <router-link to="/" class="inline-flex items-center gap-1.5 text-sm text-slate-500 hover:text-emerald-600 transition-colors mb-6 group">
      <svg class="w-4 h-4 group-hover:-translate-x-0.5 transition-transform" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2">
        <path stroke-linecap="round" stroke-linejoin="round" d="M15 19l-7-7 7-7" />
      </svg>
      Torna ai risultati
    </router-link>

    <!-- Loading -->
    <div v-if="loading" class="space-y-4">
      <div class="bg-white rounded-2xl border border-slate-100 p-6 animate-pulse">
        <div class="h-4 bg-slate-100 rounded w-20 mb-3"></div>
        <div class="h-7 bg-slate-100 rounded w-2/3 mb-2"></div>
        <div class="h-4 bg-slate-100 rounded w-full mb-1"></div>
        <div class="h-4 bg-slate-100 rounded w-3/4"></div>
      </div>
    </div>

    <!-- Not found -->
    <div v-else-if="!product" class="text-center py-24">
      <p class="text-slate-500 font-medium">Prodotto non trovato.</p>
    </div>

    <div v-else class="space-y-5">

      <!-- Header prodotto -->
      <div class="bg-white rounded-2xl border border-slate-100 shadow-sm p-6">
        <div class="flex flex-wrap gap-4 justify-between">
          <div class="flex-1 min-w-0">
            <span class="inline-block text-xs font-semibold bg-emerald-50 text-emerald-700 border border-emerald-100 px-2.5 py-0.5 rounded-full mb-3">
              {{ product.category }}
            </span>
            <h1 class="text-2xl font-bold text-slate-900 mb-2">{{ product.name }}</h1>
            <p class="text-slate-500 text-sm leading-relaxed max-w-2xl">{{ product.description }}</p>
          </div>
          <div class="text-right bg-emerald-50 border border-emerald-100 rounded-xl px-5 py-4 self-start">
            <p class="text-xs text-emerald-600 font-medium mb-0.5">Miglior prezzo</p>
            <p class="text-3xl font-bold text-emerald-700 tabular-nums">{{ product.minPrice?.toFixed(2) }}€</p>
            <p class="text-xs text-slate-400 mt-1">su {{ bestStore }}</p>
          </div>
        </div>
      </div>

      <!-- Confronto prezzi -->
      <div class="bg-white rounded-2xl border border-slate-100 shadow-sm overflow-hidden">
        <div class="px-6 py-4 border-b border-slate-50">
          <h2 class="font-bold text-slate-900 flex items-center gap-2">
            <svg class="w-5 h-5 text-emerald-500" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2">
              <path stroke-linecap="round" stroke-linejoin="round" d="M9 19v-6a2 2 0 00-2-2H5a2 2 0 00-2 2v6a2 2 0 002 2h2a2 2 0 002-2zm0 0V9a2 2 0 012-2h2a2 2 0 012 2v10m-6 0a2 2 0 002 2h2a2 2 0 002-2m0 0V5a2 2 0 012-2h2a2 2 0 012 2v14a2 2 0 01-2 2h-2a2 2 0 01-2-2z" />
            </svg>
            Confronto prezzi store
          </h2>
        </div>

        <!-- Store cards -->
        <div class="divide-y divide-slate-50">
          <div
            v-for="price in sortedPrices"
            :key="price.storeId"
            class="px-6 py-4 flex items-center gap-4 hover:bg-slate-50 transition-colors"
            :class="{ 'bg-emerald-50/50 hover:bg-emerald-50': price.finalPrice === product.minPrice }"
          >
            <!-- Store avatar -->
            <div class="w-10 h-10 rounded-xl flex items-center justify-center font-bold text-sm flex-shrink-0"
              :class="price.finalPrice === product.minPrice
                ? 'bg-emerald-100 text-emerald-700'
                : 'bg-slate-100 text-slate-600'"
            >
              {{ price.store.slice(0, 2).toUpperCase() }}
            </div>

            <!-- Info store -->
            <div class="flex-1 min-w-0">
              <div class="flex items-center gap-2 mb-1">
                <span class="font-semibold text-slate-800 text-sm">{{ price.store }}</span>
                <span v-if="price.finalPrice === product.minPrice"
                  class="text-xs bg-emerald-100 text-emerald-700 font-bold px-2 py-0.5 rounded-full">
                  Miglior prezzo
                </span>
                <span v-if="price.availability === 'low_stock'"
                  class="text-xs bg-amber-50 text-amber-600 border border-amber-100 px-2 py-0.5 rounded-full">
                  Scorte basse
                </span>
                <span v-if="price.availability === 'on_order'"
                  class="text-xs bg-slate-100 text-slate-500 px-2 py-0.5 rounded-full">
                  Su ordine
                </span>
              </div>
              <div class="flex items-center gap-3 text-xs text-slate-400">
                <span class="flex items-center gap-1">
                  <svg class="w-3.5 h-3.5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M20 7l-8-4-8 4m16 0l-8 4m8-4v10l-8 4m0-10L4 7m8 4v10M4 7v10l8 4" />
                  </svg>
                  {{ price.estimatedDelivery }}
                </span>
                <span class="flex items-center gap-1">
                  <svg class="w-3.5 h-3.5 text-amber-400" fill="currentColor" viewBox="0 0 20 20">
                    <path d="M9.049 2.927c.3-.921 1.603-.921 1.902 0l1.07 3.292a1 1 0 00.95.69h3.462c.969 0 1.371 1.24.588 1.81l-2.8 2.034a1 1 0 00-.364 1.118l1.07 3.292c.3.921-.755 1.688-1.54 1.118l-2.8-2.034a1 1 0 00-1.175 0l-2.8 2.034c-.784.57-1.838-.197-1.539-1.118l1.07-3.292a1 1 0 00-.364-1.118L2.98 8.72c-.783-.57-.38-1.81.588-1.81h3.461a1 1 0 00.951-.69l1.07-3.292z" />
                  </svg>
                  {{ price.rating }} ({{ price.reviews.toLocaleString() }})
                </span>
              </div>
            </div>

            <!-- Prezzo -->
            <div class="text-right flex-shrink-0">
              <div v-if="price.discount > 0" class="text-xs text-slate-400 line-through tabular-nums mb-0.5">
                {{ price.price.toFixed(2) }}€
              </div>
              <div class="text-xl font-bold tabular-nums"
                :class="price.finalPrice === product.minPrice ? 'text-emerald-600' : 'text-slate-800'"
              >
                {{ price.finalPrice.toFixed(2) }}€
              </div>
              <div v-if="price.discount > 0" class="text-xs font-medium text-emerald-600">
                -{{ price.discount }}% risparmi {{ (price.price - price.finalPrice).toFixed(2) }}€
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- Alert + Storico in grid -->
      <div class="grid grid-cols-1 lg:grid-cols-5 gap-5">

        <!-- Crea Alert (2/5) -->
        <div class="lg:col-span-2 bg-white rounded-2xl border border-slate-100 shadow-sm p-6">
          <h2 class="font-bold text-slate-900 mb-1 flex items-center gap-2">
            <svg class="w-5 h-5 text-amber-500" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2">
              <path stroke-linecap="round" stroke-linejoin="round" d="M15 17h5l-1.405-1.405A2.032 2.032 0 0118 14.158V11a6.002 6.002 0 00-4-5.659V5a2 2 0 10-4 0v.341C7.67 6.165 6 8.388 6 11v3.159c0 .538-.214 1.055-.595 1.436L4 17h5m6 0v1a3 3 0 11-6 0v-1m6 0H9" />
            </svg>
            Crea Alert
          </h2>
          <p class="text-xs text-slate-400 mb-4">Ti avvisiamo quando il prezzo scende sotto la soglia.</p>

          <div class="space-y-3">
            <div>
              <label class="block text-xs font-semibold text-slate-600 mb-1.5">Prezzo soglia (€)</label>
              <input
                v-model.number="alertTargetPrice"
                type="number"
                step="0.01"
                :placeholder="product.minPrice?.toFixed(2)"
                class="w-full px-3 py-2.5 border border-slate-200 rounded-xl text-sm text-slate-900 focus:outline-none focus:ring-2 focus:ring-emerald-500 focus:border-transparent bg-slate-50 focus:bg-white transition-all"
              >
            </div>

            <!-- Preset buttons -->
            <div class="flex gap-2">
              <button
                v-for="pct in [5, 10, 15]"
                :key="pct"
                @click="alertTargetPrice = +(product.minPrice * (1 - pct/100)).toFixed(2)"
                class="flex-1 text-xs py-1.5 border border-slate-200 rounded-lg text-slate-600 hover:border-emerald-300 hover:text-emerald-600 hover:bg-emerald-50 transition-all font-medium"
              >
                -{{ pct }}%
              </button>
            </div>

            <button
              @click="handleCreateAlert"
              :disabled="alertLoading || !alertTargetPrice"
              class="w-full py-2.5 bg-amber-500 hover:bg-amber-600 disabled:opacity-50 text-white text-sm font-semibold rounded-xl transition-colors flex items-center justify-center gap-2"
            >
              <svg v-if="alertLoading" class="animate-spin w-4 h-4" fill="none" viewBox="0 0 24 24">
                <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"/>
                <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z"/>
              </svg>
              {{ alertLoading ? 'Salvataggio...' : 'Attiva Alert' }}
            </button>
          </div>

          <transition name="slide">
            <div v-if="alertSuccess" class="mt-3 flex items-center gap-2 bg-emerald-50 border border-emerald-200 rounded-xl px-3 py-2.5">
              <svg class="w-4 h-4 text-emerald-500 flex-shrink-0" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z" />
              </svg>
              <p class="text-xs text-emerald-700 font-medium">Alert attivato! Ti avvisiamo appena il prezzo scende.</p>
            </div>
            <div v-else-if="alertError" class="mt-3 flex items-center gap-2 bg-red-50 border border-red-200 rounded-xl px-3 py-2.5">
              <svg class="w-4 h-4 text-red-500 flex-shrink-0" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 8v4m0 4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
              </svg>
              <p class="text-xs text-red-700">{{ alertError }}</p>
            </div>
          </transition>
        </div>

        <!-- Storico prezzi (3/5) -->
        <div class="lg:col-span-3 bg-white rounded-2xl border border-slate-100 shadow-sm p-6">
          <div class="flex items-center justify-between mb-4">
            <h2 class="font-bold text-slate-900 flex items-center gap-2">
              <svg class="w-5 h-5 text-emerald-500" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2">
                <path stroke-linecap="round" stroke-linejoin="round" d="M7 12l3-3 3 3 4-4M8 21l4-4 4 4M3 4h18M4 4h16v12a1 1 0 01-1 1H5a1 1 0 01-1-1V4z" />
              </svg>
              Storico Prezzi
            </h2>
            <button
              v-if="!historyLoaded && !historyLoading"
              @click="loadHistory"
              class="text-xs font-semibold text-emerald-600 border border-emerald-200 hover:bg-emerald-50 px-3 py-1.5 rounded-lg transition-all flex items-center gap-1.5"
            >
              <svg class="w-3.5 h-3.5" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2">
                <path stroke-linecap="round" stroke-linejoin="round" d="M4 16v1a3 3 0 003 3h10a3 3 0 003-3v-1m-4-4l-4 4m0 0l-4-4m4 4V4" />
              </svg>
              Carica storico
            </button>
          </div>

          <div v-if="!historyLoaded && !historyLoading" class="flex flex-col items-center justify-center py-10 text-slate-400">
            <svg class="w-12 h-12 mb-3 opacity-40" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M9 19v-6a2 2 0 00-2-2H5a2 2 0 00-2 2v6a2 2 0 002 2h2a2 2 0 002-2zm0 0V9a2 2 0 012-2h2a2 2 0 012 2v10m-6 0a2 2 0 002 2h2a2 2 0 002-2m0 0V5a2 2 0 012-2h2a2 2 0 012 2v14a2 2 0 01-2 2h-2a2 2 0 01-2-2z" />
            </svg>
            <p class="text-sm text-center">Carica lo storico per vedere l'andamento dei prezzi nel tempo.</p>
            <p class="text-xs mt-1 text-slate-300 italic">Pattern Serialized LOB: caricato on-demand</p>
          </div>

          <div v-if="historyLoading" class="flex items-center justify-center py-12">
            <div class="text-center">
              <svg class="animate-spin w-8 h-8 text-emerald-500 mx-auto mb-2" fill="none" viewBox="0 0 24 24">
                <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"/>
                <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z"/>
              </svg>
              <p class="text-xs text-slate-400">Caricamento storico...</p>
            </div>
          </div>

          <div v-if="historyLoaded">
            <canvas ref="chartCanvas"></canvas>
            <p class="text-xs text-slate-400 mt-3 text-center">
              {{ history?.records?.length || 0 }} rilevazioni · {{ uniqueStoresInHistory.join(' · ') }}
            </p>
          </div>
        </div>

      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, nextTick } from 'vue'
import { useRoute } from 'vue-router'
import { getProductById, getPriceHistory } from '../../services/productService'
import { useAlertsStore } from '../../store/alerts'
import Chart from 'chart.js/auto'

const route = useRoute()
const alertsStore = useAlertsStore()

const product = ref(null)
const loading = ref(true)
const alertTargetPrice = ref(null)
const alertLoading = ref(false)
const alertSuccess = ref(false)
const alertError = ref('')
const history = ref(null)
const historyLoaded = ref(false)
const historyLoading = ref(false)
const chartCanvas = ref(null)
let chartInstance = null

const sortedPrices = computed(() =>
  [...(product.value?.prices || [])].sort((a, b) => a.finalPrice - b.finalPrice)
)

const bestStore = computed(() => sortedPrices.value[0]?.store || '')

const uniqueStoresInHistory = computed(() =>
  [...new Set((history.value?.records || []).map(r => r.storeName))]
)

onMounted(async () => {
  product.value = await getProductById(route.params.id)
  loading.value = false
  if (product.value?.minPrice) {
    alertTargetPrice.value = +(product.value.minPrice * 0.95).toFixed(2)
  }
})

const handleCreateAlert = async () => {
  alertLoading.value = true
  alertSuccess.value = false
  alertError.value = ''
  try {
    await alertsStore.addAlert(product.value.id, product.value.name, alertTargetPrice.value)
    alertSuccess.value = true
    setTimeout(() => { alertSuccess.value = false }, 5000)
  } catch {
    alertError.value = 'Errore nella creazione dell\'alert.'
  } finally {
    alertLoading.value = false
  }
}

const loadHistory = async () => {
  historyLoading.value = true
  try {
    history.value = await getPriceHistory(product.value.id)
    historyLoaded.value = true
    await nextTick()
    renderChart()
  } finally {
    historyLoading.value = false
  }
}

const renderChart = () => {
  if (!chartCanvas.value || !history.value?.records) return
  if (chartInstance) chartInstance.destroy()

  const records = [...history.value.records].sort((a, b) => a.date.localeCompare(b.date))
  const stores = [...new Set(records.map(r => r.storeName))]
  const dates = [...new Set(records.map(r => r.date))].sort()
  const palette = ['#6366f1', '#f59e0b', '#10b981', '#f43f5e']

  chartInstance = new Chart(chartCanvas.value, {
    type: 'line',
    data: {
      labels: dates,
      datasets: stores.map((store, i) => ({
        label: store,
        data: dates.map(date => {
          const r = records.find(r => r.storeName === store && r.date === date)
          return r ? r.finalPrice : null
        }),
        borderColor: palette[i % palette.length],
        backgroundColor: palette[i % palette.length] + '15',
        spanGaps: true,
        tension: 0.4,
        pointRadius: 5,
        pointHoverRadius: 7,
        borderWidth: 2,
        fill: true,
      }))
    },
    options: {
      responsive: true,
      interaction: { mode: 'index', intersect: false },
      plugins: {
        legend: { position: 'bottom', labels: { boxWidth: 12, padding: 16, font: { size: 12 } } },
        tooltip: {
          callbacks: { label: ctx => ` ${ctx.dataset.label}: ${ctx.parsed.y?.toFixed(2)}€` }
        }
      },
      scales: {
        y: {
          ticks: { callback: v => v + '€', font: { size: 11 } },
          grid: { color: '#f1f5f9' }
        },
        x: {
          ticks: { font: { size: 11 } },
          grid: { display: false }
        }
      }
    }
  })
}
</script>

<style scoped>
.slide-enter-active, .slide-leave-active { transition: all 0.2s ease; }
.slide-enter-from, .slide-leave-to { opacity: 0; transform: translateY(-4px); }
</style>
