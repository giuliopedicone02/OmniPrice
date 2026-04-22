package com.unict.dmi.omniprice.controller;

import com.unict.dmi.omniprice.distributed.GenerationClockService;
import com.unict.dmi.omniprice.distributed.HeartBeatService;
import com.unict.dmi.omniprice.distributed.WorkerPool;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Controller per il monitoraggio del sistema distribuito.
 * Accessibile solo agli utenti con ruolo ADMIN.
 *
 * Espone lo stato di:
 * - WorkerPool (Leader-Followers)
 * - HeartBeat dei worker
 * - GenerationClock per ogni prodotto
 */
@RestController
@RequestMapping("/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final WorkerPool workerPool;
    private final HeartBeatService heartBeatService;
    private final GenerationClockService generationClockService;

    public AdminController(WorkerPool workerPool,
                           HeartBeatService heartBeatService,
                           GenerationClockService generationClockService) {
        this.workerPool = workerPool;
        this.heartBeatService = heartBeatService;
        this.generationClockService = generationClockService;
    }

    // GET /api/admin/status
    @GetMapping("/status")
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
    public ResponseEntity<Void> registerHeartbeat(@PathVariable String workerId) {
        heartBeatService.beat(workerId);
        return ResponseEntity.ok().build();
    }

    // GET /api/admin/generation/{productId}
    @GetMapping("/generation/{productId}")
    public ResponseEntity<Map<String, Object>> getGeneration(@PathVariable String productId) {
        long gen = generationClockService.getCurrentGeneration(productId);
        return ResponseEntity.ok(Map.of("productId", productId, "generation", gen));
    }

    // POST /api/admin/worker-task (invia un task al WorkerPool per test)
    @PostMapping("/worker-task")
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
}
