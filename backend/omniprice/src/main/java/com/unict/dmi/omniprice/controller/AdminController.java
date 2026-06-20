package com.unict.dmi.omniprice.controller;

import com.unict.dmi.omniprice.annotation.RequiresRole;
import com.unict.dmi.omniprice.cluster.ClusterCoordinator;
import com.unict.dmi.omniprice.distributed.GenerationClockService;
import com.unict.dmi.omniprice.distributed.HeartBeatService;
import com.unict.dmi.omniprice.distributed.WorkerPool;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Controller per il monitoraggio del sistema distribuito.
 * Accessibile solo agli utenti con ruolo ADMIN.
 *
 * Autorizzazione tramite @RequiresRole (Reference Monitor via AOP), lo stesso
 * meccanismo usato dagli altri controller: un unico punto di enforcement.
 *
 * Espone lo stato di:
 * - WorkerPool (Leader-Followers)
 * - HeartBeat dei worker
 * - GenerationClock per ogni prodotto
 */
@RestController
@RequestMapping("/admin")
public class AdminController {

    private final WorkerPool workerPool;
    private final HeartBeatService heartBeatService;
    private final GenerationClockService generationClockService;
    private final ClusterCoordinator clusterCoordinator;

    public AdminController(WorkerPool workerPool,
                           HeartBeatService heartBeatService,
                           GenerationClockService generationClockService,
                           ClusterCoordinator clusterCoordinator) {
        this.workerPool = workerPool;
        this.heartBeatService = heartBeatService;
        this.generationClockService = generationClockService;
        this.clusterCoordinator = clusterCoordinator;
    }

    // GET /api/admin/status
    @GetMapping("/status")
    @RequiresRole({"ADMIN"})
    public ResponseEntity<Map<String, Object>> getSystemStatus() {
        Map<String, Object> status = new HashMap<>();

        // WorkerPool status (Leader-Followers)
        Map<String, Object> poolStatus = new HashMap<>();
        poolStatus.put("activeWorkers", workerPool.getActiveWorkers());
        poolStatus.put("pendingTasks", workerPool.getPendingTasks());
        poolStatus.put("processedTasks", workerPool.getProcessedTasks());
        status.put("workerPool", poolStatus);

        // HeartBeat status
        Map<String, Object> heartbeatStatus = new HashMap<>();
        heartbeatStatus.put("aliveWorkers", heartBeatService.getAliveWorkers());
        heartbeatStatus.put("deadWorkers", heartBeatService.getDeadWorkers());
        heartbeatStatus.put("allHeartbeats", heartBeatService.getAllHeartbeats());
        status.put("heartbeat", heartbeatStatus);

        return ResponseEntity.ok(status);
    }

    // POST /api/admin/heartbeat/{workerId}
    @PostMapping("/heartbeat/{workerId}")
    @RequiresRole({"ADMIN"})
    public ResponseEntity<Void> registerHeartbeat(@PathVariable String workerId) {
        heartBeatService.beat(workerId);
        return ResponseEntity.ok().build();
    }

    // GET /api/admin/generation/{productId}
    @GetMapping("/generation/{productId}")
    @RequiresRole({"ADMIN"})
    public ResponseEntity<Map<String, Object>> getGeneration(@PathVariable String productId) {
        long gen = generationClockService.getCurrentGeneration(productId);
        return ResponseEntity.ok(Map.of("productId", productId, "generation", gen));
    }

    // POST /api/admin/worker-task (invia un task al WorkerPool per test)
    @PostMapping("/worker-task")
    @RequiresRole({"ADMIN"})
    public ResponseEntity<Map<String, String>> submitTask(@RequestBody Map<String, String> request) {
        String taskName = request.getOrDefault("name", "test-task");
        workerPool.submit(() -> {
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        return ResponseEntity.ok(Map.of("status", "submitted", "task", taskName));
    }

    // ===== Cluster Leader and Followers (ISD Cap. 2) =====

    // GET /api/admin/cluster (stato del cluster: leader, epoca, nodi)
    @GetMapping("/cluster")
    @RequiresRole({"ADMIN"})
    public ResponseEntity<Map<String, Object>> getClusterStatus() {
        return ResponseEntity.ok(clusterCoordinator.getStatus());
    }

    // POST /api/admin/cluster/write (scrittura con Majority Quorum tramite il leader)
    @PostMapping("/cluster/write")
    @RequiresRole({"ADMIN"})
    public ResponseEntity<ClusterCoordinator.WriteResult> clusterWrite(@RequestBody Map<String, String> request) {
        String key = request.getOrDefault("key", "k");
        String value = request.getOrDefault("value", "v");
        return ResponseEntity.ok(clusterCoordinator.write(key, value));
    }

    // POST /api/admin/cluster/fail/{nodeId} (simula il guasto di un nodo -> elezione)
    @PostMapping("/cluster/fail/{nodeId}")
    @RequiresRole({"ADMIN"})
    public ResponseEntity<Map<String, Object>> failNode(@PathVariable String nodeId) {
        clusterCoordinator.failNode(nodeId);
        return ResponseEntity.ok(clusterCoordinator.getStatus());
    }

    // POST /api/admin/cluster/recover/{nodeId} (recupera un nodo guasto)
    @PostMapping("/cluster/recover/{nodeId}")
    @RequiresRole({"ADMIN"})
    public ResponseEntity<Map<String, Object>> recoverNode(@PathVariable String nodeId) {
        clusterCoordinator.recoverNode(nodeId);
        return ResponseEntity.ok(clusterCoordinator.getStatus());
    }

    // POST /api/admin/cluster/zombie/{nodeId} (demo §2.4: leader zombie con epoca obsoleta)
    @PostMapping("/cluster/zombie/{nodeId}")
    @RequiresRole({"ADMIN"})
    public ResponseEntity<ClusterCoordinator.WriteResult> zombieWrite(@PathVariable String nodeId,
                                                                      @RequestBody Map<String, String> request) {
        String key = request.getOrDefault("key", "k");
        String value = request.getOrDefault("value", "v");
        return ResponseEntity.ok(clusterCoordinator.attemptWriteAsZombie(nodeId, key, value));
    }
}
