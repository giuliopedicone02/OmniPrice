<template>
  <div>
    <!-- Header banner -->
    <div class="bg-gradient-to-br from-slate-900 via-indigo-950 to-slate-900 py-10 px-4 text-white">
      <div class="max-w-6xl mx-auto">
        <div class="flex flex-wrap items-center justify-between gap-4">
          <div>
            <div class="flex items-center gap-2 mb-2">
              <span class="text-xs font-bold bg-red-500/20 text-red-400 border border-red-500/30 px-2.5 py-0.5 rounded-full uppercase tracking-wider">
                Admin Console
              </span>
              <span class="text-xs text-slate-400">ISD Cap. 2 — Sistemi Distribuiti</span>
            </div>
            <h1 class="text-3xl font-bold text-white tracking-tight">Monitoraggio Cluster & Coordinamento</h1>
            <p class="text-slate-400 text-sm mt-1">Gestione Leader-Followers, Majority Quorum, HeartBeat ed Epoche (Generation Clock)</p>
          </div>

          <div class="flex items-center gap-3">
            <button
              @click="fetchAll"
              :disabled="loading"
              class="px-4 py-2 bg-indigo-600 hover:bg-indigo-500 disabled:opacity-50 text-white text-sm font-semibold rounded-xl transition-all shadow-sm flex items-center gap-2"
            >
              <svg class="w-4 h-4" :class="loading ? 'animate-spin' : ''" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2">
                <path stroke-linecap="round" stroke-linejoin="round" d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15" />
              </svg>
              Aggiorna Stato
            </button>
          </div>
        </div>
      </div>
    </div>

    <!-- Feedback banner -->
    <div class="max-w-6xl mx-auto px-4 sm:px-6 lg:px-8 mt-6" v-if="actionMessage">
      <div
        class="p-4 rounded-xl text-sm font-medium flex items-center justify-between shadow-sm transition-all"
        :class="actionSuccess ? 'bg-emerald-50 text-emerald-800 border border-emerald-200' : 'bg-red-50 text-red-800 border border-red-200'"
      >
        <div class="flex items-center gap-2">
          <span>{{ actionSuccess ? '✅' : '⚠️' }}</span>
          <span>{{ actionMessage }}</span>
        </div>
        <button @click="actionMessage = ''" class="text-xs text-slate-500 hover:text-slate-800 font-bold ml-4">✕</button>
      </div>
    </div>

    <div class="max-w-6xl mx-auto px-4 sm:px-6 lg:px-8 py-8 space-y-8">

      <!-- 1. STATISTICHE GENERALI CLUSTER -->
      <div class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
        <!-- Leader Attuale -->
        <div class="bg-white rounded-2xl border border-slate-200 p-5 shadow-sm">
          <p class="text-xs font-semibold text-slate-500 uppercase tracking-wider mb-1">Leader Corrente</p>
          <div class="flex items-center gap-2">
            <div class="w-3 h-3 rounded-full" :class="clusterStatus.leaderId ? 'bg-emerald-500 animate-pulse' : 'bg-red-500'"></div>
            <span class="text-2xl font-bold text-slate-900 font-mono">
              {{ clusterStatus.leaderId || 'Nessun Leader' }}
            </span>
          </div>
          <p class="text-xs text-slate-400 mt-2">Eletto per consenso tra i nodi</p>
        </div>

        <!-- Epoca Corrente -->
        <div class="bg-white rounded-2xl border border-slate-200 p-5 shadow-sm">
          <p class="text-xs font-semibold text-slate-500 uppercase tracking-wider mb-1">Epoca (Generation Clock)</p>
          <p class="text-2xl font-bold text-indigo-600 font-mono">#{{ clusterStatus.currentEpoch ?? 0 }}</p>
          <p class="text-xs text-slate-400 mt-2">Incrementata ad ogni nuova elezione</p>
        </div>

        <!-- Quorum Richiesto -->
        <div class="bg-white rounded-2xl border border-slate-200 p-5 shadow-sm">
          <p class="text-xs font-semibold text-slate-500 uppercase tracking-wider mb-1">Majority Quorum</p>
          <p class="text-2xl font-bold text-slate-800 font-mono">
            {{ clusterStatus.quorum }} / {{ clusterStatus.clusterSize }}
          </p>
          <p class="text-xs text-slate-400 mt-2">Maggioranza minima richiesta (n/2 + 1)</p>
        </div>

        <!-- Nodi Vivi -->
        <div class="bg-white rounded-2xl border border-slate-200 p-5 shadow-sm">
          <p class="text-xs font-semibold text-slate-500 uppercase tracking-wider mb-1">Nodi Attivi</p>
          <p class="text-2xl font-bold text-slate-800 font-mono">
            {{ aliveCount }} / {{ clusterStatus.clusterSize || 3 }}
          </p>
          <p class="text-xs text-slate-400 mt-2">Heartbeat monitorato ogni 1000ms</p>
        </div>
      </div>

      <!-- 2. NODI DEL CLUSTER E CONTROLLO GUASTI -->
      <div class="bg-white rounded-2xl border border-slate-200 shadow-sm p-6">
        <div class="flex items-center justify-between mb-5">
          <div>
            <h2 class="text-lg font-bold text-slate-900 flex items-center gap-2">
              <span>🧭</span> Nodi del Cluster (Leader-Followers)
            </h2>
            <p class="text-xs text-slate-500">Simula guasti per innescare una nuova elezione con quorum ed epoche</p>
          </div>
        </div>

        <div class="grid grid-cols-1 md:grid-cols-3 gap-5">
          <div
            v-for="node in clusterStatus.nodes"
            :key="node.id"
            class="border rounded-2xl p-5 transition-all relative overflow-hidden"
            :class="{
              'border-emerald-300 bg-emerald-50/40 shadow-sm ring-2 ring-emerald-500/20': node.id === clusterStatus.leaderId && node.alive,
              'border-slate-200 bg-white': node.id !== clusterStatus.leaderId && node.alive,
              'border-red-200 bg-red-50/50 opacity-80': !node.alive
            }"
          >
            <!-- Badge Ruolo -->
            <div class="flex items-center justify-between mb-3">
              <span class="font-mono font-bold text-base text-slate-900">{{ node.id }}</span>
              <span
                class="text-xs font-bold px-2.5 py-0.5 rounded-full uppercase"
                :class="{
                  'bg-emerald-100 text-emerald-800 border border-emerald-200': node.state === 'LEADER' && node.alive,
                  'bg-blue-100 text-blue-800 border border-blue-200': node.state === 'FOLLOWER' && node.alive,
                  'bg-amber-100 text-amber-800 border border-amber-200': node.state === 'CANDIDATE' && node.alive,
                  'bg-red-100 text-red-800 border border-red-200': !node.alive
                }"
              >
                {{ !node.alive ? 'GUASTO' : node.state }}
              </span>
            </div>

            <!-- Dettagli Nodo -->
            <div class="space-y-1.5 text-xs text-slate-600 mb-5 font-mono">
              <div class="flex justify-between">
                <span class="text-slate-400 font-sans">Generazione / Epoca:</span>
                <span class="font-bold">#{{ node.generation }}</span>
              </div>
              <div class="flex justify-between">
                <span class="text-slate-400 font-sans">Voci nel Log:</span>
                <span class="font-bold">{{ node.logSize }}</span>
              </div>
              <div class="flex justify-between">
                <span class="text-slate-400 font-sans">Stato:</span>
                <span :class="node.alive ? 'text-emerald-600 font-bold' : 'text-red-600 font-bold'">
                  {{ node.alive ? 'Online (Heartbeat OK)' : 'Offline (Dead)' }}
                </span>
              </div>
            </div>

            <!-- Azioni sul Nodo -->
            <div class="space-y-2 pt-3 border-t border-slate-100">
              <button
                v-if="node.alive"
                @click="handleFailNode(node.id)"
                class="w-full py-1.5 px-3 bg-red-50 hover:bg-red-100 text-red-700 border border-red-200 rounded-lg text-xs font-semibold transition-colors flex items-center justify-center gap-1.5"
              >
                <span>💥</span> Simula Guasto (Fail)
              </button>
              <button
                v-else
                @click="handleRecoverNode(node.id)"
                class="w-full py-1.5 px-3 bg-emerald-50 hover:bg-emerald-100 text-emerald-700 border border-emerald-200 rounded-lg text-xs font-semibold transition-colors flex items-center justify-center gap-1.5"
              >
                <span>🔄</span> Ripristina Nodo (Recover)
              </button>

              <button
                @click="handleZombieWrite(node.id)"
                class="w-full py-1.5 px-3 bg-amber-50 hover:bg-amber-100 text-amber-800 border border-amber-200 rounded-lg text-xs font-semibold transition-colors flex items-center justify-center gap-1.5"
                title="Tenta una scrittura usando questo nodo come Leader Zombie con epoca obsoleta"
              >
                <span>🧟</span> Test Leader Zombie
              </button>
            </div>
          </div>
        </div>
      </div>

      <!-- 3. TEST SCRITTURA CON MAJORITY QUORUM -->
      <div class="grid grid-cols-1 lg:grid-cols-2 gap-6">
        <!-- Form Scrittura Quorum -->
        <div class="bg-white rounded-2xl border border-slate-200 shadow-sm p-6">
          <h2 class="text-lg font-bold text-slate-900 flex items-center gap-2 mb-1">
            <span>✍️</span> Scrittura con Majority Quorum
          </h2>
          <p class="text-xs text-slate-500 mb-4">
            Invia una scrittura al cluster coordinata dal Leader: viene confermata solo se la maggioranza dei nodi risponde.
          </p>

          <form @submit.prevent="handleClusterWrite" class="space-y-4">
            <div>
              <label class="block text-xs font-semibold text-slate-700 mb-1">Chiave (Key)</label>
              <input
                v-model="writeForm.key"
                type="text"
                required
                placeholder="es. config_threshold"
                class="w-full px-3 py-2 border border-slate-200 rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500"
              />
            </div>
            <div>
              <label class="block text-xs font-semibold text-slate-700 mb-1">Valore (Value)</label>
              <input
                v-model="writeForm.value"
                type="text"
                required
                placeholder="es. 42.5"
                class="w-full px-3 py-2 border border-slate-200 rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500"
              />
            </div>

            <button
              type="submit"
              :disabled="writeLoading"
              class="w-full py-2.5 bg-indigo-600 hover:bg-indigo-700 disabled:opacity-50 text-white text-sm font-semibold rounded-xl transition-all shadow-sm flex items-center justify-center gap-2"
            >
              <svg v-if="writeLoading" class="animate-spin w-4 h-4" fill="none" viewBox="0 0 24 24">
                <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"/>
                <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z"/>
              </svg>
              Esegui Scrittura a Quorum
            </button>
          </form>

          <!-- Risultato Scrittura -->
          <div v-if="lastWriteResult" class="mt-4 p-4 rounded-xl border text-xs" :class="lastWriteResult.committed ? 'bg-emerald-50 border-emerald-200 text-emerald-900' : 'bg-red-50 border-red-200 text-red-900'">
            <div class="flex items-center justify-between mb-1">
              <span class="font-bold uppercase tracking-wider">Esito Scrittura</span>
              <span class="font-bold font-mono">{{ lastWriteResult.committed ? 'COMMITTED' : 'NON COMMITTED' }}</span>
            </div>
            <p class="text-slate-600 mt-1">Messaggio: <strong>{{ lastWriteResult.message }}</strong></p>
            <p class="text-slate-600">Acks Ricevuti: <strong>{{ lastWriteResult.acks }} / Quorum {{ lastWriteResult.quorum }}</strong></p>
            <p class="text-slate-600">Leader Esecutore: <strong>{{ lastWriteResult.leaderId || 'Nessuno' }}</strong></p>
          </div>
        </div>

        <!-- 4. WORKER POOL & HEARTBEATS -->
        <div class="bg-white rounded-2xl border border-slate-200 shadow-sm p-6">
          <div class="flex items-center justify-between mb-3">
            <h2 class="text-lg font-bold text-slate-900 flex items-center gap-2">
              <span>⚡</span> Worker Pool & HeartBeat Service
            </h2>
            <button
              @click="handleTaskSubmit"
              class="px-3 py-1.5 bg-slate-100 hover:bg-slate-200 text-slate-800 rounded-lg text-xs font-semibold transition-colors"
            >
              + Invia Task di Test
            </button>
          </div>
          <p class="text-xs text-slate-500 mb-4">
            Gestione della concorrenza a thread (*WorkerPool*) e failure detection periodica (*HeartBeat*).
          </p>

          <!-- Worker Pool Stats -->
          <div class="grid grid-cols-3 gap-3 mb-5">
            <div class="bg-slate-50 rounded-xl p-3 border border-slate-100 text-center">
              <p class="text-xs text-slate-400">Worker Attivi</p>
              <p class="text-lg font-bold text-slate-800 font-mono">{{ systemStatus.workerPool?.activeWorkers ?? 0 }}</p>
            </div>
            <div class="bg-slate-50 rounded-xl p-3 border border-slate-100 text-center">
              <p class="text-xs text-slate-400">Task In Coda</p>
              <p class="text-lg font-bold text-slate-800 font-mono">{{ systemStatus.workerPool?.pendingTasks ?? 0 }}</p>
            </div>
            <div class="bg-slate-50 rounded-xl p-3 border border-slate-100 text-center">
              <p class="text-xs text-slate-400">Task Eseguiti</p>
              <p class="text-lg font-bold text-slate-800 font-mono">{{ systemStatus.workerPool?.processedTasks ?? 0 }}</p>
            </div>
          </div>

          <!-- HeartBeat Details -->
          <div>
            <p class="text-xs font-semibold text-slate-700 uppercase tracking-wider mb-2">Failure Detection (Heartbeats)</p>
            <div class="space-y-2 max-h-40 overflow-y-auto pr-1">
              <div
                v-for="(timestamp, workerId) in systemStatus.heartbeat?.allHeartbeats"
                :key="workerId"
                class="flex items-center justify-between p-2.5 bg-slate-50 border border-slate-100 rounded-xl text-xs"
              >
                <div class="flex items-center gap-2">
                  <div
                    class="w-2 h-2 rounded-full"
                    :class="systemStatus.heartbeat?.aliveWorkers?.includes(workerId) ? 'bg-emerald-500' : 'bg-red-500'"
                  ></div>
                  <span class="font-mono font-bold text-slate-700">{{ workerId }}</span>
                </div>
                <span class="text-slate-400 font-mono">{{ formatTimestamp(timestamp) }}</span>
              </div>
            </div>
          </div>

        </div>
      </div>

    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import {
  getClusterStatus,
  writeToCluster,
  failClusterNode,
  recoverClusterNode,
  testZombieWrite,
  getSystemStatus,
  submitWorkerTask
} from '../../services/adminService'

const loading = ref(false)
const clusterStatus = ref({
  leaderId: null,
  clusterSize: 3,
  quorum: 2,
  currentEpoch: 1,
  nodes: []
})
const systemStatus = ref({})
const writeForm = ref({ key: 'soglia_prezzo', value: '199.99' })
const writeLoading = ref(false)
const lastWriteResult = ref(null)

const actionMessage = ref('')
const actionSuccess = ref(true)

let intervalId = null

const aliveCount = computed(() => {
  return clusterStatus.value.nodes?.filter(n => n.alive).length ?? 0
})

const fetchAll = async () => {
  loading.value = true
  try {
    const [cStatus, sStatus] = await Promise.all([
      getClusterStatus(),
      getSystemStatus()
    ])
    clusterStatus.value = cStatus
    systemStatus.value = sStatus
  } catch (error) {
    console.error('Errore recupero dati admin:', error)
  } finally {
    loading.value = false
  }
}

const handleFailNode = async (nodeId) => {
  try {
    const res = await failClusterNode(nodeId)
    clusterStatus.value = res
    actionSuccess.value = true
    actionMessage.value = `Nodo ${nodeId} guastato con successo. Il cluster ha gestito la ri-elezione automatica.`
    await fetchAll()
  } catch (error) {
    actionSuccess.value = false
    actionMessage.value = `Errore durante il guasto del nodo: ${error.message}`
  }
}

const handleRecoverNode = async (nodeId) => {
  try {
    const res = await recoverClusterNode(nodeId)
    clusterStatus.value = res
    actionSuccess.value = true
    actionMessage.value = `Nodo ${nodeId} ripristinato con successo.`
    await fetchAll()
  } catch (error) {
    actionSuccess.value = false
    actionMessage.value = `Errore durante il ripristino del nodo: ${error.message}`
  }
}

const handleZombieWrite = async (nodeId) => {
  try {
    const res = await testZombieWrite(nodeId, writeForm.value.key, writeForm.value.value)
    lastWriteResult.value = res
    actionSuccess.value = res.committed
    actionMessage.value = res.committed
      ? `Scrittura completata con successo!`
      : `Test Leader Zombie: La richiesta da ${nodeId} è stata correttamente respinta con epoca obsoleta!`
    await fetchAll()
  } catch (error) {
    actionSuccess.value = false
    actionMessage.value = `Errore nel test zombie: ${error.message}`
  }
}

const handleClusterWrite = async () => {
  writeLoading.value = true
  try {
    const res = await writeToCluster(writeForm.value.key, writeForm.value.value)
    lastWriteResult.value = res
    actionSuccess.value = res.committed
    actionMessage.value = res.committed
      ? `Scrittura a Quorum completata (${res.acks}/${res.quorum} acks)!`
      : `Scrittura fallita: ${res.message}`
    await fetchAll()
  } catch (error) {
    actionSuccess.value = false
    actionMessage.value = `Errore scrittura cluster: ${error.message}`
  } finally {
    writeLoading.value = false
  }
}

const handleTaskSubmit = async () => {
  try {
    await submitWorkerTask('admin-demo-task')
    actionSuccess.value = true
    actionMessage.value = 'Task inviato al WorkerPool per l\'elaborazione.'
    await fetchAll()
  } catch (error) {
    actionSuccess.value = false
    actionMessage.value = `Errore invio task: ${error.message}`
  }
}

const formatTimestamp = (ts) => {
  if (!ts) return '-'
  const d = new Date(ts)
  return d.toLocaleTimeString()
}

onMounted(() => {
  fetchAll()
  intervalId = setInterval(fetchAll, 3000)
})

onUnmounted(() => {
  if (intervalId) clearInterval(intervalId)
})
</script>
