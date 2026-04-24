<template>
  <div>

    <!-- Hero search -->
    <div class="bg-gradient-to-br from-violet-700 via-indigo-700 to-indigo-800 py-10 px-4">
      <div class="max-w-3xl mx-auto text-center">
        <h1 class="text-3xl font-bold text-white mb-2">Trova il miglior prezzo</h1>
        <p class="text-indigo-200 text-sm mb-6">Confrontiamo 4 store in tempo reale per te</p>

        <!-- Search bar -->
        <div class="flex gap-2 bg-white rounded-2xl p-2 shadow-xl shadow-indigo-900/20">
          <div class="flex-1 flex items-center gap-3 px-3">
            <svg class="w-5 h-5 text-slate-400 flex-shrink-0" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2">
              <path stroke-linecap="round" stroke-linejoin="round" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
            </svg>
            <input
              v-model="searchQuery"
              @keyup.enter="handleSearch"
              type="text"
              placeholder="Cerca un prodotto... (laptop, iphone, ps5)"
              class="flex-1 text-sm text-slate-900 placeholder-slate-400 focus:outline-none py-1"
            >
            <button
              v-if="searchQuery"
              @click="clearSearch"
              class="text-slate-400 hover:text-slate-600 transition-colors"
            >
              <svg class="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12" />
              </svg>
            </button>
          </div>
          <button
            @click="handleSearch"
            :disabled="isLoading"
            class="px-6 py-2.5 bg-gradient-to-r from-violet-600 to-indigo-600 hover:from-violet-700 hover:to-indigo-700 text-white text-sm font-semibold rounded-xl disabled:opacity-60 transition-all flex items-center gap-2"
          >
            <svg v-if="isLoading" class="animate-spin w-4 h-4" fill="none" viewBox="0 0 24 24">
              <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"/>
              <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z"/>
            </svg>
            {{ isLoading ? 'Ricerca...' : 'Cerca' }}
          </button>
        </div>

        <!-- Suggerimenti rapidi -->
        <div class="flex flex-wrap gap-2 justify-center mt-4">
          <button
            v-for="tag in quickSearches"
            :key="tag"
            @click="quickSearch(tag)"
            class="px-3 py-1 bg-white/10 hover:bg-white/20 text-white/80 hover:text-white text-xs rounded-full border border-white/20 transition-all"
          >
            {{ tag }}
          </button>
        </div>
      </div>
    </div>

    <!-- Contenuto principale -->
    <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">

      <!-- Toolbar: filtri + sort + vista -->
      <div v-if="results.length > 0" class="flex flex-wrap items-center justify-between gap-3 mb-5">
        <div>
          <h2 class="text-lg font-bold text-slate-900">
            {{ filteredAndSorted.length }} risultati
            <span class="text-slate-400 font-normal text-base">per "<span class="text-indigo-600 font-semibold">{{ lastQuery }}</span>"</span>
          </h2>
          <p class="text-sm text-slate-500 mt-0.5">Prezzi aggiornati in tempo reale da 4 store</p>
        </div>

        <div class="flex items-center gap-2 flex-wrap">
          <!-- Filtro categoria -->
          <div v-if="categories.length > 1" class="flex items-center gap-1.5 flex-wrap">
            <button
              @click="activeCategory = ''"
              class="px-3 py-1.5 rounded-lg text-xs font-semibold transition-all border"
              :class="activeCategory === '' ? 'bg-indigo-600 text-white border-indigo-600' : 'bg-white text-slate-600 border-slate-200 hover:border-indigo-300 hover:text-indigo-600'"
            >
              Tutte
            </button>
            <button
              v-for="cat in categories"
              :key="cat"
              @click="activeCategory = activeCategory === cat ? '' : cat"
              class="px-3 py-1.5 rounded-lg text-xs font-semibold transition-all border"
              :class="activeCategory === cat ? 'bg-indigo-600 text-white border-indigo-600' : 'bg-white text-slate-600 border-slate-200 hover:border-indigo-300 hover:text-indigo-600'"
            >
              {{ cat }}
            </button>
          </div>

          <!-- Sort -->
          <select v-model="sortBy" class="text-sm border border-slate-200 rounded-lg px-3 py-1.5 text-slate-700 focus:outline-none focus:ring-2 focus:ring-indigo-500 bg-white">
            <option value="name">Nome</option>
            <option value="price_asc">Prezzo ↑</option>
            <option value="price_desc">Prezzo ↓</option>
            <option value="discount">Sconto</option>
          </select>

          <!-- View toggle -->
          <div class="flex items-center bg-slate-100 rounded-lg p-1">
            <button
              @click="viewMode = 'grid'"
              class="p-1.5 rounded transition-all"
              :class="viewMode === 'grid' ? 'bg-white shadow-sm text-indigo-600' : 'text-slate-400 hover:text-slate-600'"
            >
              <svg class="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2">
                <path stroke-linecap="round" stroke-linejoin="round" d="M4 6a2 2 0 012-2h2a2 2 0 012 2v2a2 2 0 01-2 2H6a2 2 0 01-2-2V6zM14 6a2 2 0 012-2h2a2 2 0 012 2v2a2 2 0 01-2 2h-2a2 2 0 01-2-2V6zM4 16a2 2 0 012-2h2a2 2 0 012 2v2a2 2 0 01-2 2H6a2 2 0 01-2-2v-2zM14 16a2 2 0 012-2h2a2 2 0 012 2v2a2 2 0 01-2 2h-2a2 2 0 01-2-2v-2z" />
              </svg>
            </button>
            <button
              @click="viewMode = 'list'"
              class="p-1.5 rounded transition-all"
              :class="viewMode === 'list' ? 'bg-white shadow-sm text-indigo-600' : 'text-slate-400 hover:text-slate-600'"
            >
              <svg class="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2">
                <path stroke-linecap="round" stroke-linejoin="round" d="M4 6h16M4 12h16M4 18h16" />
              </svg>
            </button>
          </div>
        </div>
      </div>

      <!-- Griglia prodotti -->
      <div v-if="filteredAndSorted.length > 0 && viewMode === 'grid'" class="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-3 gap-5">
        <div
          v-for="product in filteredAndSorted"
          :key="product.id"
          class="bg-white rounded-2xl border border-slate-100 shadow-sm hover:shadow-md hover:-translate-y-0.5 transition-all duration-200 flex flex-col overflow-hidden"
        >
          <div class="p-5 pb-4 flex-1">
            <div class="flex items-start justify-between mb-3 gap-2">
              <h3 class="font-bold text-slate-900 text-base leading-snug">{{ product.name }}</h3>
              <div class="flex flex-col items-end gap-1 flex-shrink-0">
                <span class="text-xs font-medium bg-indigo-50 text-indigo-700 px-2 py-0.5 rounded-full border border-indigo-100">
                  {{ product.category }}
                </span>
                <span v-if="bestDiscount(product) > 0" class="text-xs font-bold bg-emerald-50 text-emerald-700 px-2 py-0.5 rounded-full border border-emerald-100">
                  -{{ bestDiscount(product) }}% OFF
                </span>
              </div>
            </div>

            <p v-if="product.description" class="text-xs text-slate-500 line-clamp-2 mb-4">
              {{ product.description }}
            </p>

            <!-- Prezzi con barre comparative -->
            <div class="space-y-2.5">
              <div
                v-for="price in sortedPrices(product)"
                :key="price.storeId"
              >
                <div class="flex items-center gap-2 mb-1">
                  <div class="w-2 h-2 rounded-full flex-shrink-0"
                    :class="price.finalPrice === product.minPrice ? 'bg-emerald-400' : 'bg-slate-200'">
                  </div>
                  <span class="text-xs text-slate-600 flex-1 truncate">{{ price.store }}</span>
                  <span v-if="price.discount > 0" class="text-xs bg-emerald-50 text-emerald-700 border border-emerald-100 px-1.5 py-0.5 rounded-md font-medium">-{{ price.discount }}%</span>
                  <span v-if="price.availability === 'low_stock'" class="text-xs text-amber-500 font-bold">!</span>
                  <span class="text-sm font-bold text-slate-900 tabular-nums">{{ price.finalPrice.toFixed(2) }}€</span>
                </div>
                <!-- Price bar -->
                <div class="h-1 bg-slate-100 rounded-full overflow-hidden">
                  <div
                    class="h-full rounded-full transition-all duration-500"
                    :class="price.finalPrice === product.minPrice ? 'bg-emerald-400' : 'bg-slate-300'"
                    :style="{ width: priceBarWidth(price.finalPrice, product) + '%' }"
                  ></div>
                </div>
              </div>
            </div>
          </div>

          <div class="px-5 py-3 bg-slate-50 border-t border-slate-100 flex items-center justify-between">
            <div>
              <p class="text-xs text-slate-400">Miglior prezzo</p>
              <p class="text-xl font-bold text-emerald-600 tabular-nums">{{ product.minPrice?.toFixed(2) }}€</p>
            </div>
            <router-link
              :to="`/product/${product.id}`"
              class="flex items-center gap-1.5 text-sm font-semibold text-white bg-indigo-600 hover:bg-indigo-700 px-4 py-2 rounded-xl transition-colors shadow-sm"
            >
              Dettagli
              <svg class="w-3.5 h-3.5" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2.5">
                <path stroke-linecap="round" stroke-linejoin="round" d="M9 5l7 7-7 7" />
              </svg>
            </router-link>
          </div>
        </div>
      </div>

      <!-- Vista lista -->
      <div v-else-if="filteredAndSorted.length > 0 && viewMode === 'list'" class="space-y-3">
        <div
          v-for="product in filteredAndSorted"
          :key="product.id"
          class="bg-white rounded-2xl border border-slate-100 shadow-sm hover:shadow-md transition-all duration-200 overflow-hidden"
        >
          <div class="p-5 flex items-start gap-5">
            <!-- Info prodotto -->
            <div class="flex-1 min-w-0">
              <div class="flex items-start gap-2 mb-1">
                <h3 class="font-bold text-slate-900 text-base leading-snug">{{ product.name }}</h3>
                <span class="text-xs font-medium bg-indigo-50 text-indigo-700 px-2 py-0.5 rounded-full border border-indigo-100 flex-shrink-0">{{ product.category }}</span>
                <span v-if="bestDiscount(product) > 0" class="text-xs font-bold bg-emerald-50 text-emerald-700 px-2 py-0.5 rounded-full border border-emerald-100 flex-shrink-0">-{{ bestDiscount(product) }}% OFF</span>
              </div>
              <p v-if="product.description" class="text-xs text-slate-500 mb-3">{{ product.description }}</p>

              <!-- Barre prezzi in lista -->
              <div class="space-y-2">
                <div v-for="price in sortedPrices(product)" :key="price.storeId" class="flex items-center gap-3">
                  <span class="text-xs text-slate-500 w-24 flex-shrink-0 truncate">{{ price.store }}</span>
                  <div class="flex-1 h-1.5 bg-slate-100 rounded-full overflow-hidden">
                    <div
                      class="h-full rounded-full"
                      :class="price.finalPrice === product.minPrice ? 'bg-emerald-400' : 'bg-indigo-200'"
                      :style="{ width: priceBarWidth(price.finalPrice, product) + '%' }"
                    ></div>
                  </div>
                  <div class="flex items-center gap-1.5 flex-shrink-0">
                    <span v-if="price.discount > 0" class="text-xs text-emerald-600 font-medium">-{{ price.discount }}%</span>
                    <span v-if="price.availability === 'low_stock'" class="text-xs text-amber-500 font-bold">!</span>
                    <span class="text-sm font-bold tabular-nums" :class="price.finalPrice === product.minPrice ? 'text-emerald-600' : 'text-slate-700'">{{ price.finalPrice.toFixed(2) }}€</span>
                  </div>
                </div>
              </div>
            </div>

            <!-- CTA -->
            <div class="flex-shrink-0 text-right">
              <p class="text-xs text-slate-400 mb-0.5">Miglior prezzo</p>
              <p class="text-2xl font-bold text-emerald-600 tabular-nums mb-3">{{ product.minPrice?.toFixed(2) }}€</p>
              <router-link
                :to="`/product/${product.id}`"
                class="flex items-center gap-1.5 text-sm font-semibold text-white bg-indigo-600 hover:bg-indigo-700 px-4 py-2 rounded-xl transition-colors shadow-sm whitespace-nowrap"
              >
                Vedi dettagli
                <svg class="w-3.5 h-3.5" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2.5">
                  <path stroke-linecap="round" stroke-linejoin="round" d="M9 5l7 7-7 7" />
                </svg>
              </router-link>
            </div>
          </div>
        </div>
      </div>

      <!-- Skeleton loading -->
      <div v-else-if="isLoading" class="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-3 gap-5">
        <div v-for="n in 6" :key="n" class="bg-white rounded-2xl border border-slate-100 p-5 animate-pulse">
          <div class="flex gap-2 mb-3">
            <div class="h-5 bg-slate-100 rounded flex-1"></div>
            <div class="h-5 bg-slate-100 rounded w-16"></div>
          </div>
          <div class="h-3 bg-slate-100 rounded mb-1 w-full"></div>
          <div class="h-3 bg-slate-100 rounded mb-4 w-3/4"></div>
          <div class="space-y-2.5">
            <div v-for="i in 4" :key="i">
              <div class="h-3 bg-slate-100 rounded mb-1"></div>
              <div class="h-1 bg-slate-100 rounded"></div>
            </div>
          </div>
        </div>
      </div>

      <!-- Nessun risultato (dopo filtro categoria) -->
      <div v-else-if="results.length > 0 && filteredAndSorted.length === 0" class="text-center py-16">
        <p class="text-slate-500 font-medium mb-2">Nessun prodotto nella categoria "{{ activeCategory }}"</p>
        <button @click="activeCategory = ''" class="text-sm text-indigo-600 font-medium hover:underline">Mostra tutti i risultati</button>
      </div>

      <!-- Nessun risultato dalla ricerca -->
      <div v-else-if="hasSearched && !isLoading" class="text-center py-20">
        <div class="w-16 h-16 bg-slate-100 rounded-2xl flex items-center justify-center mx-auto mb-4">
          <svg class="w-8 h-8 text-slate-400" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M9.172 16.172a4 4 0 015.656 0M9 10h.01M15 10h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
          </svg>
        </div>
        <h3 class="text-lg font-semibold text-slate-700 mb-1">Nessun prodotto trovato</h3>
        <p class="text-slate-400 text-sm">Prova con: laptop, iphone, ps5, cuffie, drone, mouse</p>
      </div>

      <!-- Stato iniziale -->
      <div v-else-if="!hasSearched" class="text-center py-16 text-slate-400">
        <svg class="w-12 h-12 mx-auto mb-3 opacity-50" fill="none" viewBox="0 0 24 24" stroke="currentColor">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
        </svg>
        <p class="text-sm">Inizia a cercare un prodotto qui sopra</p>
      </div>

    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { searchProducts } from '../../services/productService'

const searchQuery = ref('')
const results = ref([])
const isLoading = ref(false)
const hasSearched = ref(false)
const lastQuery = ref('')
const sortBy = ref('name')
const viewMode = ref('grid')
const activeCategory = ref('')

const quickSearches = ['Laptop', 'iPhone', 'PlayStation 5', 'Cuffie', 'MacBook', 'Drone', 'Tastiera', 'Mouse']

const categories = computed(() => {
  const cats = [...new Set(results.value.map(p => p.category).filter(Boolean))]
  return cats.sort()
})

const sortedPrices = (product) =>
  [...(product.prices || [])].sort((a, b) => a.finalPrice - b.finalPrice)

const bestDiscount = (product) =>
  Math.max(...(product.prices || []).map(p => p.discount || 0))

const priceBarWidth = (price, product) => {
  const prices = (product.prices || []).map(p => p.finalPrice)
  const min = Math.min(...prices)
  const max = Math.max(...prices)
  if (max === min) return 100
  return Math.round(((price - min) / (max - min)) * 60 + 40)
}

const filteredAndSorted = computed(() => {
  let arr = activeCategory.value
    ? results.value.filter(p => p.category === activeCategory.value)
    : [...results.value]

  if (sortBy.value === 'price_asc') return arr.sort((a, b) => (a.minPrice ?? 0) - (b.minPrice ?? 0))
  if (sortBy.value === 'price_desc') return arr.sort((a, b) => (b.minPrice ?? 0) - (a.minPrice ?? 0))
  if (sortBy.value === 'discount') return arr.sort((a, b) => bestDiscount(b) - bestDiscount(a))
  return arr.sort((a, b) => a.name.localeCompare(b.name))
})

const handleSearch = async () => {
  if (!searchQuery.value.trim()) return
  isLoading.value = true
  hasSearched.value = true
  lastQuery.value = searchQuery.value
  activeCategory.value = ''
  try {
    const response = await searchProducts(searchQuery.value)
    results.value = response.results || []
  } catch {
    results.value = []
  } finally {
    isLoading.value = false
  }
}

const quickSearch = (term) => {
  searchQuery.value = term
  handleSearch()
}

const clearSearch = () => {
  searchQuery.value = ''
  results.value = []
  hasSearched.value = false
  activeCategory.value = ''
}

onMounted(() => {
  searchQuery.value = 'laptop'
  handleSearch()
})
</script>
