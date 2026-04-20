<template>
  <div class="max-w-7xl mx-auto py-6 sm:px-6 lg:px-8">
    
    <div class="mb-8 flex gap-4">
      <input 
        v-model="searchQuery" 
        @keyup.enter="handleSearch"
        type="text" 
        placeholder="Cerca un prodotto (es. laptop)..." 
        class="flex-1 px-4 py-2 border border-gray-300 rounded-md shadow-sm focus:ring-indigo-500 focus:border-indigo-500"
      >
      <button 
        @click="handleSearch"
        :disabled="isLoading"
        class="px-6 py-2 bg-indigo-600 text-white rounded-md hover:bg-indigo-700 disabled:opacity-50 transition-colors"
      >
        {{ isLoading ? 'Ricerca...' : 'Cerca' }}
      </button>
    </div>

    <div v-if="results.length > 0" class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
      <div v-for="product in results" :key="product.id" class="bg-white rounded-lg shadow-md p-6 border border-gray-100 hover:shadow-lg transition-shadow">
        <div class="flex justify-between items-start mb-4">
          <h3 class="text-lg font-bold text-gray-900 truncate pr-2">{{ product.name }}</h3>
          <span class="bg-indigo-100 text-indigo-800 text-xs font-semibold px-2.5 py-0.5 rounded flex-shrink-0">
            {{ product.category }}
          </span>
        </div>
        
        <div class="space-y-2 mb-4">
          <p class="text-sm text-gray-500 font-medium">Prezzi rilevati:</p>
          <ul class="space-y-1 bg-gray-50 p-3 rounded-md">
            <li v-for="price in product.prices" :key="price.store" class="flex justify-between text-sm">
              <span class="font-medium text-gray-700">{{ price.store }}</span>
              <span class="font-bold">{{ price.price }} {{ price.currency }}</span>
            </li>
          </ul>
        </div>

        <div class="pt-4 border-t border-gray-200 flex justify-between items-center mt-auto">
          <div class="text-sm">
            <span class="text-gray-500 block text-xs">Miglior prezzo</span>
            <span class="block font-bold text-green-600 text-lg">{{ product.minPrice }} €</span>
          </div>
          <button class="text-indigo-600 hover:text-indigo-800 text-sm font-semibold flex items-center">
            Vedi Dettagli 
            <svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4 ml-1" viewBox="0 0 20 20" fill="currentColor">
              <path fill-rule="evenodd" d="M12.293 5.293a1 1 0 011.414 0l4 4a1 1 0 010 1.414l-4 4a1 1 0 01-1.414-1.414L14.586 11H3a1 1 0 110-2h11.586l-2.293-2.293a1 1 0 010-1.414z" clip-rule="evenodd" />
            </svg>
          </button>
        </div>
      </div>
    </div>

    <div v-else-if="!isLoading && hasSearched" class="text-center py-16 bg-white rounded-lg shadow-sm border border-gray-200">
      <svg xmlns="http://www.w3.org/2000/svg" class="h-12 w-12 mx-auto text-gray-400 mb-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9.172 16.172a4 4 0 015.656 0M9 10h.01M15 10h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
      </svg>
      <p class="text-gray-600 font-medium text-lg">Nessun prodotto trovato.</p>
      <p class="text-gray-400 mt-1">Prova a cercare qualcosa di diverso (es. "laptop").</p>
    </div>

  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { searchProducts } from '../../services/productService'

const searchQuery = ref('')
const results = ref([])
const isLoading = ref(false)
const hasSearched = ref(false)

const handleSearch = async () => {
  if (!searchQuery.value.trim()) return
  
  isLoading.value = true
  hasSearched.value = true
  
  try {
    const response = await searchProducts(searchQuery.value)
    results.value = response.results || []
  } catch (error) {
    console.error("Errore di ricerca:", error)
    results.value = []
  } finally {
    isLoading.value = false
  }
}

// Facciamo una ricerca automatica all'avvio della pagina per test
onMounted(() => {
  searchQuery.value = 'laptop'
  handleSearch()
})
</script>