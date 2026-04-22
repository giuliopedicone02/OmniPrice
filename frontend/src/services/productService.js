import api from './api'

export const searchProducts = async (query = '') => {
    try {
        const response = await api.get(`/products/search?q=${encodeURIComponent(query)}`)
        return response.data
    } catch (error) {
        console.error('Errore durante la ricerca:', error)
        return { results: [], totalResults: 0 }
    }
}

export const getProductById = async (id) => {
    try {
        const response = await api.get(`/products/${id}`)
        return response.data
    } catch (error) {
        console.error('Errore recupero prodotto:', error)
        return null
    }
}

export const getPriceHistory = async (productId) => {
    try {
        const response = await api.get(`/price-history/${productId}`)
        return response.data
    } catch (error) {
        console.error('Errore recupero storico prezzi:', error)
        return null
    }
}
