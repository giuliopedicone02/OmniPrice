package com.unict.dmi.omniprice.cluster;

import com.unict.dmi.omniprice.distributed.GenerationClockService;
import com.unict.dmi.omniprice.distributed.HeartBeatService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifica la logica del pattern Leader and Followers distribuito (ISD Cap. 2):
 * elezione, Majority Quorum (§2.1), failover guidato dall'Heart Beat (§2.2.3) e
 * rifiuto degli aggiornamenti di un leader "zombie" tramite Generation Clock (§2.4).
 *
 * Il coordinatore viene istanziato a mano (senza contesto Spring) per evitare
 * l'interferenza dei task @Scheduled e ottenere un test deterministico.
 */
class ClusterCoordinatorTest {

    private GenerationClockService clock;
    private ClusterCoordinator cluster;

    @BeforeEach
    void setUp() {
        clock = new GenerationClockService();
        cluster = new ClusterCoordinator(new HeartBeatService(), clock);
        cluster.init(); // prima elezione (epoca 1)
    }

    @Test
    @DisplayName("All'avvio viene eletto un leader con epoca 1")
    void initialElection() {
        assertNotNull(cluster.getLeaderId());
        assertEquals(1, clock.getCurrentGeneration());
    }

    @Test
    @DisplayName("Una scrittura raggiunge il quorum e viene committata")
    void quorumWrite() {
        ClusterCoordinator.WriteResult result = cluster.write("x", "1");
        assertTrue(result.committed());
        assertTrue(result.acks() >= result.quorum());
    }

    @Test
    @DisplayName("La caduta del leader innesca una nuova elezione con epoca maggiore")
    void leaderFailoverTriggersElection() {
        String oldLeader = cluster.getLeaderId();
        long oldEpoch = clock.getCurrentGeneration();

        cluster.failNode(oldLeader); // failure -> nuova elezione

        String newLeader = cluster.getLeaderId();
        assertNotNull(newLeader);
        assertNotEquals(oldLeader, newLeader);
        assertTrue(clock.getCurrentGeneration() > oldEpoch);

        // con 2 nodi vivi su 3 il quorum (2) e' ancora raggiungibile
        assertTrue(cluster.write("y", "2").committed());
    }

    @Test
    @DisplayName("Sotto il quorum non c'e' leader e le scritture falliscono")
    void noQuorumNoWrites() {
        // abbattendo 2 nodi su 3 restano 1 < quorum(2)
        cluster.failNode("node-1");
        cluster.failNode("node-2");
        cluster.failNode("node-3");

        assertNull(cluster.getLeaderId());
        assertFalse(cluster.write("z", "3").committed());
    }

    @Test
    @DisplayName("Un leader zombie con epoca obsoleta viene rifiutato e degrada a follower")
    void zombieLeaderRejected() {
        String oldLeader = cluster.getLeaderId();
        long oldEpoch = clock.getCurrentGeneration();

        // Il vecchio leader cade -> il cluster elegge un nuovo leader con epoca > oldEpoch
        cluster.failNode(oldLeader);
        long newEpoch = clock.getCurrentGeneration();
        assertTrue(newEpoch > oldEpoch);

        // Il vecchio leader rientra credendosi ancora leader (epoca obsoleta)
        cluster.recoverNode(oldLeader);

        ClusterCoordinator.WriteResult result = cluster.attemptWriteAsZombie(oldLeader, "k", "v");
        assertFalse(result.committed(), "lo zombie non deve raggiungere il quorum");
    }
}
