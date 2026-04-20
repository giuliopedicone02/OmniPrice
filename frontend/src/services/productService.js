import api from './api'

export const searchProducts = async (query = '') => {
    try {
        const response = await api.get(`/products/search?q=${query}`)
        return response.data // Questo conterrà 'results', 'totalResults', ecc.
    } catch (error) {
        console.error("Errore durante la ricerca:", error)
        return { results: [], totalResults: 0 }
    }
}