package com.unict.dmi.omniprice.cluster;

import com.unict.dmi.omniprice.distributed.GenerationClockService;
import com.unict.dmi.omniprice.distributed.HeartBeatService;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Coordinatore del cluster: implementa il pattern Leader and Followers distribuito
 * (ISD Cap. 2), integrando Majority Quorum (§2.1), elezione del leader (§2.2),
 * Heart Beat per la failure detection (§2.3) e Generation Clock come epoca (§2.4).
 *
 * E' una simulazione in-process: i nodi del cluster sono oggetti {@link ClusterNode}
 * gestiti in una sola JVM, ma la logica di consenso (quorum, epoca, rifiuto degli
 * aggiornamenti obsoleti) e' quella descritta dal pattern.
 *
 * NB: e' distinto dal {@code WorkerPool}, che implementa l'omonimo pattern di
 * concorrenza Leader/Followers a livello di thread (dispatch dei task).
 */
@Service
public class ClusterCoordinator {

    private static final Logger log = LoggerFactory.getLogger(ClusterCoordinator.class);

    private static final int CLUSTER_SIZE = 3;
    // §2.3: Intervallo Timeout > Intervallo Richiesta > RTT
    private static final long HEARTBEAT_INTERVAL_MS = 1_000;
    private static final long LEADER_TIMEOUT_MS = 3_000;

    private final HeartBeatService heartBeatService;
    private final GenerationClockService generationClockService;

    private final List<ClusterNode> nodes = new CopyOnWriteArrayList<>();
    private volatile String leaderId;

    public ClusterCoordinator(HeartBeatService heartBeatService,
                              GenerationClockService generationClockService) {
        this.heartBeatService = heartBeatService;
        this.generationClockService = generationClockService;
    }

    @PostConstruct
    public void init() {
        for (int i = 1; i <= CLUSTER_SIZE; i++) {
            nodes.add(new ClusterNode("node-" + i));
        }
        log.info("Cluster avviato con {} nodi (quorum {})", CLUSTER_SIZE, quorum());
        electLeader("avvio del cluster");
    }

    /** Maggioranza richiesta: n/2 + 1 (ISD §2.1.3). */
    public int quorum() {
        return CLUSTER_SIZE / 2 + 1;
    }

    public int size() {
        return CLUSTER_SIZE;
    }

    public String getLeaderId() {
        return leaderId;
    }

    // ===== Elezione (§2.2.3) =====

    /**
     * Elegge un nuovo leader scegliendo il nodo piu' aggiornato tra quelli vivi
     * (generazione piu' alta, poi log piu' lungo, poi ID maggiore - §2.2.3.1).
     * L'elezione ha successo solo se il candidato ottiene la maggioranza dei voti
     * e se il numero di nodi vivi raggiunge il quorum (altrimenti il cluster resta
     * senza leader e non accetta scritture).
     */
    public synchronized boolean electLeader(String reason) {
        List<ClusterNode> alive = aliveNodes();
        if (alive.size() < quorum()) {
            leaderId = null;
            log.warn("Elezione fallita ({}): nodi vivi {}/{} < quorum {}",
                    reason, alive.size(), CLUSTER_SIZE, quorum());
            return false;
        }

        ClusterNode candidate = alive.stream()
                .max(Comparator.comparingLong(ClusterNode::getGeneration)
                        .thenComparingInt(ClusterNode::getLogSize)
                        .thenComparing(ClusterNode::getId))
                .orElseThrow();
        candidate.becomeCandidate();

        long votes = alive.stream().filter(n -> n.voteFor(candidate)).count();
        if (votes < quorum()) {
            log.warn("Elezione fallita ({}): {} voti < quorum {}", reason, votes, quorum());
            return false;
        }

        long epoch = generationClockService.startNewGeneration();
        for (ClusterNode n : nodes) {
            if (n == candidate) {
                n.becomeLeader(epoch);
            } else if (n.isAlive()) {
                n.becomeFollower(epoch);
            }
        }
        leaderId = candidate.getId();
        log.info("Eletto leader {} con epoca {} [{}] - voti {}/{}",
                leaderId, epoch, reason, votes, alive.size());
        return true;
    }

    // ===== Scrittura con Majority Quorum (§2.1) =====

    /**
     * Scrittura coordinata dal leader: l'entry viene replicata ai follower e
     * considerata committed solo se la maggioranza (quorum) dei nodi la conferma.
     */
    public synchronized WriteResult write(String key, String value) {
        ClusterNode leader = leader();
        if (leader == null || !leader.isAlive()) {
            return new WriteResult(false, "nessun leader disponibile", 0, quorum(), null);
        }

        LogEntry entry = leader.appendAsLeader(key, value);
        int acks = 1; // il leader ha gia' applicato l'entry
        for (ClusterNode n : nodes) {
            if (n == leader || !n.isAlive()) {
                continue;
            }
            if (n.replicate(entry, leader.getGeneration())) {
                acks++;
            }
        }

        boolean committed = acks >= quorum();
        log.info("Write '{}={}' epoca {}: {} ({} ack / quorum {})",
                key, value, leader.getGeneration(),
                committed ? "COMMITTED" : "NON committed", acks, quorum());
        return new WriteResult(committed,
                committed ? "committed" : "quorum non raggiunto",
                acks, quorum(), leaderId);
    }

    /**
     * Demo del "leader zombie" (§2.4): un nodo che si crede ancora leader tenta di
     * coordinare una scrittura con un'epoca ormai superata. I follower (a epoca piu'
     * alta) rifiutano la replica; il nodo scopre di essere obsoleto e degrada a
     * follower adottando l'epoca corrente.
     */
    public synchronized WriteResult attemptWriteAsZombie(String nodeId, String key, String value) {
        ClusterNode zombie = nodeById(nodeId);
        if (zombie == null) {
            return new WriteResult(false, "nodo inesistente", 0, quorum(), leaderId);
        }

        LogEntry entry = new LogEntry(zombie.getGeneration(), zombie.getLogSize(), key, value);
        int acks = zombie.isAlive() ? 1 : 0;
        long maxSeenGeneration = zombie.getGeneration();

        for (ClusterNode n : nodes) {
            if (n == zombie || !n.isAlive()) {
                continue;
            }
            if (n.replicate(entry, zombie.getGeneration())) {
                acks++;
            } else {
                maxSeenGeneration = Math.max(maxSeenGeneration, n.getGeneration());
            }
        }

        boolean committed = acks >= quorum();
        if (!committed && maxSeenGeneration > zombie.getGeneration()) {
            zombie.becomeFollower(maxSeenGeneration);
            log.warn("Leader zombie {} rifiutato (epoca {} < {}): degrada a follower",
                    nodeId, entry.generation(), maxSeenGeneration);
        }
        return new WriteResult(committed,
                committed ? "committed" : "rifiutato: epoca obsoleta (leader zombie)",
                acks, quorum(), leaderId);
    }

    // ===== Failure injection (per la dimostrazione) =====

    public synchronized void failNode(String id) {
        ClusterNode n = nodeById(id);
        if (n == null) {
            return;
        }
        n.fail();
        heartBeatService.deregister(id);
        log.warn("Nodo {} simulato GUASTO", id);
        if (id.equals(leaderId)) {
            log.warn("Il leader {} e' caduto: avvio nuova elezione", id);
            electLeader("caduta del leader " + id);
        }
    }

    public synchronized void recoverNode(String id) {
        ClusterNode n = nodeById(id);
        if (n == null) {
            return;
        }
        n.recover();
        log.info("Nodo {} recuperato (stato CANDIDATE, epoca {})", id, n.getGeneration());
    }

    // ===== Heart Beat e monitoraggio (§2.3) =====

    /** I nodi vivi inviano periodicamente il proprio battito. */
    @Scheduled(fixedDelay = HEARTBEAT_INTERVAL_MS)
    public void sendHeartbeats() {
        for (ClusterNode n : nodes) {
            if (n.isAlive()) {
                n.heartbeat();
                heartBeatService.beat(n.getId());
            }
        }
    }

    /** Rileva un leader non piu' attivo e avvia una nuova elezione (§2.2.3). */
    @Scheduled(fixedDelay = HEARTBEAT_INTERVAL_MS)
    public void monitorLeader() {
        ClusterNode leader = leader();
        if (leader == null) {
            electLeader("nessun leader presente");
            return;
        }
        long since = System.currentTimeMillis() - leader.getLastHeartbeat();
        if (!leader.isAlive() || since > LEADER_TIMEOUT_MS) {
            log.warn("Leader {} non risponde (timeout {}ms): nuova elezione", leader.getId(), since);
            electLeader("timeout heartbeat leader");
        }
    }

    // ===== Stato (per AdminController) =====

    public Map<String, Object> getStatus() {
        Map<String, Object> status = new LinkedHashMap<>();
        status.put("leaderId", leaderId);
        status.put("clusterSize", CLUSTER_SIZE);
        status.put("quorum", quorum());
        status.put("currentEpoch", generationClockService.getCurrentGeneration());

        List<Map<String, Object>> nodeList = new ArrayList<>();
        for (ClusterNode n : nodes) {
            Map<String, Object> node = new LinkedHashMap<>();
            node.put("id", n.getId());
            node.put("state", n.getState());
            node.put("generation", n.getGeneration());
            node.put("alive", n.isAlive());
            node.put("logSize", n.getLogSize());
            nodeList.add(node);
        }
        status.put("nodes", nodeList);
        return status;
    }

    private ClusterNode leader() {
        return leaderId == null ? null : nodeById(leaderId);
    }

    private ClusterNode nodeById(String id) {
        return nodes.stream().filter(n -> n.getId().equals(id)).findFirst().orElse(null);
    }

    private List<ClusterNode> aliveNodes() {
        return nodes.stream().filter(ClusterNode::isAlive).toList();
    }

    /**
     * Esito di una scrittura con quorum.
     */
    public record WriteResult(boolean committed, String message, int acks, int quorum, String leaderId) {
    }
}
