package com.unict.dmi.omniprice.distributed;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Pool di worker che implementa il pattern Leader-Followers.
 *
 * Pattern: Leader-Followers
 * Un solo thread e' il "leader" in ogni momento: attende nuovi task dalla coda.
 * Quando un task arriva:
 *   1. Il leader rilascia il semaforo, promuovendo un follower a nuovo leader
 *   2. Il vecchio leader elabora il task (diventa "worker")
 *   3. Dopo l'elaborazione, torna follower e compete per diventare leader
 *
 * Vantaggi:
 * - Elimina il dispatch overhead (il leader processa direttamente)
 * - Riduce context switch rispetto a un pool classico
 * - Scalabile: il numero di thread e' fisso, non cresce con il carico
 */
@Component
public class WorkerPool {

    private static final Logger log = LoggerFactory.getLogger(WorkerPool.class);

    private static final int POOL_SIZE = 4;
    private static final long HEARTBEAT_INTERVAL_MS = 5_000;

    private final HeartBeatService heartBeatService;

    // Solo UN thread puo' essere leader (ha il semaforo)
    private final Semaphore leaderSemaphore = new Semaphore(1);
    private final BlockingQueue<Runnable> taskQueue = new LinkedBlockingQueue<>();
    private final List<Thread> workers = new ArrayList<>();
    private volatile boolean running = false;

    private final AtomicInteger activeWorkers = new AtomicInteger(0);
    private final AtomicInteger processedTasks = new AtomicInteger(0);

    public WorkerPool(HeartBeatService heartBeatService) {
        this.heartBeatService = heartBeatService;
    }

    @PostConstruct
    public void start() {
        running = true;
        for (int i = 0; i < POOL_SIZE; i++) {
            final int workerId = i;
            Thread worker = new Thread(() -> workerLoop(workerId), "leader-follower-" + i);
            worker.setDaemon(true);
            worker.start();
            workers.add(worker);
        }
        log.info("WorkerPool avviato con {} thread (Leader-Followers pattern)", POOL_SIZE);
    }

    @PreDestroy
    public void stop() {
        running = false;
        workers.forEach(Thread::interrupt);
        for (int i = 0; i < POOL_SIZE; i++) {
            heartBeatService.deregister("worker-" + i);
        }
        log.info("WorkerPool fermato. Task elaborati: {}", processedTasks.get());
    }

    /**
     * Aggiunge un task alla coda. Il leader corrente lo processerà.
     */
    public void submit(Runnable task) {
        taskQueue.offer(task);
    }

    /**
     * Loop principale di ciascun thread.
     * Ogni thread alterna tra ruolo follower (aspetta il semaforo)
     * e ruolo leader (prende task dalla coda) e worker (processa il task).
     * Ogni worker invia un HeartBeat periodico per segnalare che e' vivo.
     */
    private void workerLoop(int workerId) {
        String workerName = "worker-" + workerId;
        AtomicLong lastBeat = new AtomicLong(0);

        while (running) {
            try {
                // === HeartBeat: segnala al monitor che il worker e' vivo ===
                long now = System.currentTimeMillis();
                if (now - lastBeat.get() >= HEARTBEAT_INTERVAL_MS) {
                    heartBeatService.beat(workerName);
                    lastBeat.set(now);
                }

                // === Fase FOLLOWER: compete per diventare leader ===
                if (!leaderSemaphore.tryAcquire(1, TimeUnit.SECONDS)) {
                    continue; // nessun semaforo disponibile, ritenta
                }

                // === Fase LEADER: attende un task dalla coda ===
                Runnable task = taskQueue.poll(500, TimeUnit.MILLISECONDS);

                if (task == null) {
                    // Nessun task, rilascia il semaforo per non bloccare gli altri
                    leaderSemaphore.release();
                    continue;
                }

                // === Promozione follower a leader ===
                // Rilascia il semaforo PRIMA di processare: un follower diventa leader
                leaderSemaphore.release();

                // === Fase WORKER: elabora il task ===
                activeWorkers.incrementAndGet();
                try {
                    log.debug("Worker-{} elabora task", workerId);
                    task.run();
                    processedTasks.incrementAndGet();
                } catch (Exception e) {
                    log.error("Worker-{} errore durante l'elaborazione: {}", workerId, e.getMessage());
                } finally {
                    activeWorkers.decrementAndGet();
                }

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    public int getActiveWorkers() { return activeWorkers.get(); }
    public int getProcessedTasks() { return processedTasks.get(); }
    public int getPendingTasks() { return taskQueue.size(); }
}
