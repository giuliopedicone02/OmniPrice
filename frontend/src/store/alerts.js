import { defineStore } from 'pinia'
import { getAlerts, createAlert, cancelAlert } from '../services/alertService'

export const useAlertsStore = defineStore('alerts', {
    state: () => ({
        alerts: [],
        loading: false,
        error: null,
    }),
    getters: {
        activeAlerts: (state) => state.alerts.filter(a => a.status === 'ACTIVE'),
        triggeredAlerts: (state) => state.alerts.filter(a => a.status === 'TRIGGERED'),
    },
    actions: {
        async fetchAlerts() {
            this.loading = true
            try {
                this.alerts = await getAlerts()
            } catch (e) {
                this.error = e.message
            } finally {
                this.loading = false
            }
        },
        async addAlert(productId, productName, targetPrice) {
            const alert = await createAlert(productId, productName, targetPrice)
            this.alerts.unshift(alert)
            return alert
        },
        async removeAlert(alertId) {
            await cancelAlert(alertId)
            const idx = this.alerts.findIndex(a => a.id === alertId)
            if (idx !== -1) this.alerts[idx].status = 'CANCELLED'
        },
    }
})
