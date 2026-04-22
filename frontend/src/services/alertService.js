import api from './api'

export const getAlerts = async () => {
    try {
        const response = await api.get('/alerts')
        return response.data
    } catch (error) {
        console.error('Errore recupero alert:', error)
        return []
    }
}

export const createAlert = async (productId, productName, targetPrice) => {
    const response = await api.post('/alerts', { productId, productName, targetPrice })
    return response.data
}

export const cancelAlert = async (alertId) => {
    const response = await api.delete(`/alerts/${alertId}`)
    return response.data
}
