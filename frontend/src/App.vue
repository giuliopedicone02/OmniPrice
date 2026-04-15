<script setup>
import { ref, onMounted } from 'vue'

const products = ref([])
const loading = ref(true)

// Funzione per scaricare i prodotti
const fetchProducts = async () => {
  try {
    // Chiameremo l'API di Giulio. Per ora, se non è pronta, 
    // potresti anche usare un array finto qui dentro!
    const response = await fetch('http://localhost:8080/api/products')
    if (response.ok) {
      products.value = await response.json()
    }
  } catch (error) {
    console.error("Errore di connessione al backend", error)
  } finally {
    loading.value = false
  }
}

// Appena la pagina si carica, scarica i prodotti
onMounted(() => {
  fetchProducts()
})
</script>

<template>
  <main style="max-width: 800px; margin: 0 auto; padding: 2rem; font-family: sans-serif;">
    <h1>OmniPrice 🛒</h1>
    
    <div style="margin-bottom: 2rem;">
      <input 
        type="text" 
        placeholder="Cerca un prodotto..." 
        style="padding: 0.5rem; width: 100%; font-size: 1.1rem;"
      >
    </div>

    <p v-if="loading">Caricamento catalogo in corso...</p>

    <ul v-else style="list-style: none; padding: 0;">
      <li 
        v-for="product in products" 
        :key="product.id"
        style="border: 1px solid #ccc; padding: 1rem; margin-bottom: 1rem; border-radius: 8px;"
      >
        <h2 style="margin: 0 0 0.5rem 0;">{{ product.name }}</h2>
        <span style="background-color: #eee; padding: 0.2rem 0.5rem; border-radius: 4px; font-size: 0.9rem;">
          {{ product.category }}
        </span>
      </li>
    </ul>
  </main>
</template>