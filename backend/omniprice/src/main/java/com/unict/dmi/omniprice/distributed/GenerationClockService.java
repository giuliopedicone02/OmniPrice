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
 * Pattern: Generation Clock
 * Ogni aggiornamento di prezzo porta un numero di generazione crescente.
 * Un nodo accetta un aggiornamento solo se la generazione e' piu' recente
 * di quella gia' presente, scartando aggiornamenti obsoleti (stale updates)
 * che potrebbero arrivare fuori ordine per colpa della latenza di rete.
 *
 * Esempio:
 *   Generazione 5 arriva -> accettata, aggiorna a gen 5
 *   Generazione 3 arriva in ritardo -> scartata (gia' a gen 5)
 *   Generazione 6 arriva -> accettata, aggiorna a gen 6
 */
@Service
public class GenerationClockService {

    private static final Logger log = LoggerFactory.getLogger(GenerationClockService.class);

    // productId -> generazione corrente
    private final Map<String, AtomicLong> generations = new ConcurrentHashMap<>();

    // Contatore globale per generare nuovi numeri di generazione
    private final AtomicLong globalClock = new AtomicLong(0);

    /**
     * Genera un nuovo numero di generazione crescente e monotono.
     * Usato dal produttore di aggiornamenti (PriceCheckScheduler).
     */
    public long nextGeneration() {
        return globalClock.incrementAndGet();
    }

    /**
     * Tenta di applicare un aggiornamento con la generazione data.
     * Restituisce true se accettato, false se obsoleto (stale).
     *
     * @param resourceId  ID della risorsa (es. productId)
     * @param generation  Numero di generazione dell'aggiornamento
     * @return true se l'aggiornamento e' valido e puo' essere applicato
     */
    public boolean tryUpdate(String resourceId, long generation) {
        AtomicLong current = generations.computeIfAbsent(resourceId, k -> new AtomicLong(-1));

        // CAS loop: aggiorna solo se la generazione e' piu' recente
        while (true) {
            long currentGen = current.get();
            if (generation <= currentGen) {
                log.debug("Aggiornamento obsoleto scartato per '{}': gen {} <= gen corrente {}",
                        resourceId, generation, currentGen);
                return false;
            }
            if (current.compareAndSet(currentGen, generation)) {
                log.debug("Aggiornamento accettato per '{}': gen {} -> {}", resourceId, currentGen, generation);
                return true;
            }
            // CAS fallito (race condition), ritenta
        }
    }

    /**
     * Restituisce la generazione corrente di una risorsa.
     */
    public long getCurrentGeneration(String resourceId) {
        AtomicLong gen = generations.get(resourceId);
        return gen != null ? gen.get() : -1;
    }

    /**
     * Resetta la generazione di una risorsa (usato nei test).
     */
    public void reset(String resourceId) {
        generations.remove(resourceId);
    }
}
