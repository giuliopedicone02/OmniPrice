package com.unict.dmi.omniprice.cluster;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Nodo (server) simulato del cluster Leader and Followers (ISD §2.2).
 *
 * Ogni nodo conosce la propria generazione (epoca), mantiene una replica del
 * Write-Ahead Log e puo' assumere uno dei tre stati {@link NodeState}.
 * Le decisioni di consistenza (accettazione/rifiuto di una replica) sono prese
 * confrontando la generazione del leader con la propria (§2.4).
 */
public class ClusterNode {

    private final String id;
    private volatile NodeState state = NodeState.FOLLOWER;
    private final AtomicLong generation = new AtomicLong(0);
    private volatile boolean alive = true;
    private volatile long lastHeartbeat = System.currentTimeMillis();
    private final List<LogEntry> log = new CopyOnWriteArrayList<>();

    public ClusterNode(String id) {
        this.id = id;
    }

    public String getId() { return id; }
    public NodeState getState() { return state; }
    public long getGeneration() { return generation.get(); }
    public boolean isAlive() { return alive; }
    public long getLastHeartbeat() { return lastHeartbeat; }
    public int getLogSize() { return log.size(); }
    public List<LogEntry> getLog() { return log; }

    void becomeLeader(long epoch) {
        this.state = NodeState.LEADER;
        this.generation.set(epoch);
    }

    void becomeFollower(long epoch) {
        this.state = NodeState.FOLLOWER;
        if (epoch > generation.get()) {
            generation.set(epoch);
        }
    }

    void becomeCandidate() {
        this.state = NodeState.CANDIDATE;
    }

    void heartbeat() { this.lastHeartbeat = System.currentTimeMillis(); }

    void fail() { this.alive = false; }

    void recover() {
        this.alive = true;
        this.lastHeartbeat = System.currentTimeMillis();
        this.state = NodeState.CANDIDATE; // al rientro cerca il leader corrente
    }

    /** Il leader appende localmente una nuova entry al proprio log. */
    LogEntry appendAsLeader(String key, String value) {
        LogEntry entry = new LogEntry(generation.get(), log.size(), key, value);
        log.add(entry);
        return entry;
    }

    /**
     * Un follower riceve un'entry dal leader (§2.4): accetta solo se la generazione
     * del leader e' >= alla propria. Se inferiore, la respinge (leader "zombie").
     * Se superiore, adotta la nuova epoca e degrada a follower.
     *
     * @return true se l'entry e' stata accettata e applicata
     */
    synchronized boolean replicate(LogEntry entry, long leaderGeneration) {
        if (!alive) {
            return false;
        }
        if (leaderGeneration < generation.get()) {
            return false; // epoca obsoleta: rifiuto
        }
        if (leaderGeneration > generation.get()) {
            generation.set(leaderGeneration);
            this.state = NodeState.FOLLOWER;
        }
        log.add(entry);
        return true;
    }

    /**
     * Voto in elezione (§2.2.3.1): concede il voto se il candidato e' aggiornato
     * almeno quanto se stesso (generazione maggiore, oppure pari generazione ma
     * log non piu' corto).
     */
    synchronized boolean voteFor(ClusterNode candidate) {
        if (!alive) {
            return false;
        }
        long candGen = candidate.getGeneration();
        long myGen = generation.get();
        return candGen > myGen || (candGen == myGen && candidate.getLogSize() >= log.size());
    }
}
