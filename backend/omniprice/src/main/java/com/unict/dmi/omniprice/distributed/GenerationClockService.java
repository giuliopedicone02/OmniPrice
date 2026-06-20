package com.unict.dmi.omniprice.distributed;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Servizio Generation Clock per il coordinamento degli aggiornamenti distribuiti.
 *
 * Pattern: Generation Clock (Epoca) — cfr. ISD §2.4
 * La generazione e' un numero monotonicamente crescente che identifica l'EPOCA
 * corrente del cluster. Viene incrementata SOLO ad ogni nuova elezione del leader
 * (non ad ogni aggiornamento): tutti gli aggiornamenti prodotti dallo stesso leader
 * portano quindi la stessa generazione.
 *
 * Un follower respinge un aggiornamento se la sua generazione e' INFERIORE a quella
 * gia' applicata: cosi' si neutralizza il "leader zombie" (un vecchio leader che,
 * dopo un partizionamento di rete, invia comandi con un'epoca ormai superata).
 *
 * Esempio (ISD §2.4):
 *   Leader1 (Gen 1) si ferma -> il cluster elegge Leader2 (Gen 2).
 *   Al ritorno Leader1 invia comandi con Gen 1 -> i follower (a Gen 2) li respingono.
 */
@Service
public class GenerationClockService {

    private static final Logger log = LoggerFactory.getLogger(GenerationClockService.class);

    // resourceId -> ultima generazione (epoca) applicata su quella risorsa
    private final Map<String, AtomicLong> appliedGenerations = new ConcurrentHashMap<>();

    // Epoca corrente del cluster: cambia solo all'elezione di un nuovo leader.
    // Parte da 0; la prima elezione (all'avvio del cluster) la porta a 1.
    private final AtomicLong currentGeneration = new AtomicLong(0);

    /**
     * Restituisce l'epoca corrente del cluster.
     * Usato dal produttore (PriceCheckScheduler) per timbrare gli aggiornamenti
     * di un intero ciclo con la stessa generazione.
     */
    public long getCurrentGeneration() {
        return currentGeneration.get();
    }

    /**
     * Avvia una nuova epoca: incrementa la generazione del cluster.
     * Va invocato SOLO al cambio di leader (nuova elezione), tipicamente quando
     * l'Heart Beat segnala che il leader precedente non e' piu' attivo (ISD §2.2.3).
     *
     * @return la nuova generazione (epoca)
     */
    public long startNewGeneration() {
        long g = currentGeneration.incrementAndGet();
        log.info("Nuova elezione: epoca del cluster -> generazione {}", g);
        return g;
    }

    /**
     * Tenta di applicare un aggiornamento con la generazione (epoca) data.
     * Accetta se la generazione e' >= a quella gia' applicata sulla risorsa
     * (stesso leader o leader piu' recente); rifiuta se inferiore (leader obsoleto).
     *
     * @param resourceId  ID della risorsa (es. productId)
     * @param generation  Epoca dell'aggiornamento (epoca del leader che lo produce)
     * @return true se l'aggiornamento e' valido e puo' essere applicato
     */
    public boolean tryUpdate(String resourceId, long generation) {
        AtomicLong applied = appliedGenerations.computeIfAbsent(resourceId, k -> new AtomicLong(0));

        // CAS loop: rifiuta solo le epoche obsolete, accetta quelle uguali o piu' recenti
        while (true) {
            long current = applied.get();
            if (generation < current) {
                log.debug("Aggiornamento obsoleto scartato per '{}': gen {} < epoca applicata {}",
                        resourceId, generation, current);
                return false;
            }
            if (applied.compareAndSet(current, generation)) {
                return true;
            }
            // CAS fallito (race condition), ritenta
        }
    }

    /**
     * Restituisce l'ultima generazione (epoca) applicata su una risorsa.
     */
    public long getCurrentGeneration(String resourceId) {
        AtomicLong gen = appliedGenerations.get(resourceId);
        return gen != null ? gen.get() : 0;
    }

    /**
     * Resetta la generazione applicata di una risorsa (usato nei test).
     */
    public void reset(String resourceId) {
        appliedGenerations.remove(resourceId);
    }
}
