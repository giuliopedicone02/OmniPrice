import api from './api'

export const getClusterStatus = async () => {
    const response = await api.get('/admin/cluster')
    return response.data
}

export const writeToCluster = async (key, value) => {
    const response = await api.post('/admin/cluster/write', { key, value })
    return response.data
}

export const failClusterNode = async (nodeId) => {
    const response = await api.post(`/admin/cluster/fail/${nodeId}`)
    return response.data
}

export const recoverClusterNode = async (nodeId) => {
    const response = await api.post(`/admin/cluster/recover/${nodeId}`)
    return response.data
}

export const testZombieWrite = async (nodeId, key, value) => {
    const response = await api.post(`/admin/cluster/zombie/${nodeId}`, { key, value })
    return response.data
}

export const getSystemStatus = async () => {
    const response = await api.get('/admin/status')
    return response.data
}

export const submitWorkerTask = async (name) => {
    const response = await api.post('/admin/worker-task', { name })
    return response.data
}
